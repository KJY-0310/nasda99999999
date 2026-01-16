package com.example.nasda.service;

import com.example.nasda.domain.*;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CommentServiceCrudTests {

    @Autowired CommentService commentService;
    @Autowired PostService postService;
    @Autowired CommentRepository commentRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    @DisplayName("댓글 생성")
    void create_comment() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("디자인");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "게시글",
                "본문"
        );

        Integer commentId =
                commentService.createComment(post.getPostId(), user.getUserId(), "댓글 내용");

        CommentEntity saved = commentRepository.findById(commentId).orElseThrow();
        assertThat(saved.getContent()).isEqualTo("댓글 내용");
        assertThat(saved.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("댓글 수정")
    void edit_comment() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("여행");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "게시글",
                "본문"
        );

        Integer commentId =
                commentService.createComment(post.getPostId(), user.getUserId(), "수정 전");

        commentService.editComment(commentId, user.getUserId(), "수정 후");

        CommentEntity edited = commentRepository.findById(commentId).orElseThrow();
        assertThat(edited.getContent()).isEqualTo("수정 후");
    }

    @Test
    @DisplayName("댓글 삭제")
    void delete_comment() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("사진");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "게시글",
                "본문"
        );

        Integer commentId =
                commentService.createComment(post.getPostId(), user.getUserId(), "삭제될 댓글");

        commentService.deleteComment(commentId, user.getUserId());

        assertThat(commentRepository.existsById(commentId)).isFalse();
    }

    @Test
    @DisplayName("댓글 페이징 테스트")
    void paging_comments() {
        UserEntity user = createUser();
        CategoryEntity category = createCategory("취미");

        PostEntity post = postService.create(
                user.getUserId(),
                category.getCategoryId(),
                "페이징 게시글",
                "본문"
        );

        for (int i = 1; i <= 25; i++) {
            commentService.createComment(
                    post.getPostId(),
                    user.getUserId(),
                    "댓글 " + i
            );
        }

        var page0 = commentService.getCommentsPage(post.getPostId(), 0, 10, user.getUserId());
        var page1 = commentService.getCommentsPage(post.getPostId(), 1, 10, user.getUserId());
        var page2 = commentService.getCommentsPage(post.getPostId(), 2, 10, user.getUserId());

        assertThat(page0.getContent()).hasSize(10);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page0.getTotalElements()).isEqualTo(25);
    }

    // helpers
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
