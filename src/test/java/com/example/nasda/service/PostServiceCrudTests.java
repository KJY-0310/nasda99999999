package com.example.nasda.service;

import com.example.nasda.domain.*;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostServiceCrudTests {

    @Autowired PostService postService;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    @DisplayName("게시글 생성 → 조회")
    void create_and_get() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("디자인");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "테스트 제목",
                "테스트 본문"
        );

        PostEntity found = postService.get(post.getPostId());

        assertThat(found.getTitle()).isEqualTo("테스트 제목");
        assertThat(found.getDescription()).isEqualTo("테스트 본문");
        assertThat(found.getUser().getUserId()).isEqualTo(user.getUserId());
        assertThat(found.getCategory().getCategoryName()).isEqualTo("디자인");
    }

    @Test
    @DisplayName("게시글 수정")
    void update() {
        UserEntity user = createUser();
        CategoryEntity c1 = createCategory("여행");
        CategoryEntity c2 = createCategory("자연");

        PostEntity post = postService.create(
                user.getUserId(),
                c1.getCategoryId(),
                "수정 전",
                "본문"
        );

        postService.update(
                post.getPostId(),
                user.getUserId(),
                c2.getCategoryId(),
                "수정 후",
                "수정된 본문"
        );

        PostEntity updated = postService.get(post.getPostId());
        assertThat(updated.getTitle()).isEqualTo("수정 후");
        assertThat(updated.getCategory().getCategoryName()).isEqualTo("자연");
    }

    @Test
    @DisplayName("게시글 삭제")
    void delete() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("사진");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "삭제용",
                "삭제 테스트"
        );

        Integer postId = post.getPostId();
        assertThat(postRepository.existsById(postId)).isTrue();

        postService.delete(postId, user.getUserId());

        assertThat(postRepository.existsById(postId)).isFalse();
    }

    // ======================
    // helper methods
    // ======================
    private UserEntity createUser() {
        return userRepository.save(
                UserEntity.builder()
                        .loginId("test_" + System.nanoTime())
                        .password("pw")
                        .email("test" + System.nanoTime() + "@mail.com")
                        .nickname("테스터" + System.nanoTime())
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );
    }

    private CategoryEntity createCategory(String name) {
        return categoryRepository.save(
                CategoryEntity.builder()
                        .categoryName(name)
                        .build()
        );
    }
}
