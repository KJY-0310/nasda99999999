package com.example.nasda.service;

import com.example.nasda.domain.*;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.repository.PostImageRepository;
import com.example.nasda.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DummyDataGenerateTests {

    private static final Logger log = LoggerFactory.getLogger(DummyDataGenerateTests.class);

    private static final String DUMMY_PREFIX = "[DUMMY]";
    private static final String DUMMY_USER_LOGIN_PREFIX = "dummy_";
    private static final String DUMMY_USER_NICK_PREFIX = "더미유저_";
    private static final String DUMMY_EMAIL_PREFIX = "dummy_";

    @Autowired private PostService postService;
    @Autowired private CommentService commentService;

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    /**
     * ✅ 더미 데이터 생성 (DB에 실제 반영)
     * - posts: 100
     * - comments: 0~3 랜덤
     */
    @Test
    @Order(1)
    @Transactional
    @Commit
    void generateDummyData_commit() {

        // 유저 1명 생성
        UserEntity user = createDummyUser();

        // 카테고리 3개 생성
        List<CategoryEntity> categories = createDummyCategories();

        int totalPosts = 100;
        int totalComments = 0;

        for (int i = 1; i <= totalPosts; i++) {
            CategoryEntity picked = categories.get(i % categories.size());

            PostEntity post = postService.create(
                    user.getUserId(),
                    picked.getCategoryId(),
                    DUMMY_PREFIX + " 게시글 " + i,
                    DUMMY_PREFIX + " 내용 " + i + "\n" + LocalDateTime.now()
            );

            // 댓글 0~3개 랜덤
            int commentCount = ThreadLocalRandom.current().nextInt(0, 4);
            for (int c = 1; c <= commentCount; c++) {
                commentService.createComment(
                        post.getPostId(),
                        user.getUserId(),
                        DUMMY_PREFIX + " 댓글 " + c
                );
                totalComments++;
            }
        }

        log.info("✅ 더미 생성 완료: posts={}, comments={}", totalPosts, totalComments);
    }

    /**
     * ✅ 더미 데이터 정리 (DB에 실제 반영)
     * - 일부를 수동 삭제해도 에러 없이 동작
     * - 여러 번 생성해서 남은 더미까지 전부 정리
     */
    @Test
    @Order(2)
    @Transactional
    @Commit
    void cleanupDummyData_commit() {
        cleanupDummyDataInternal();
    }

    private void cleanupDummyDataInternal() {
        log.info("🧹 더미 데이터 정리 시작");

        // 0) 더미 게시글 ID 목록 먼저 확보 (FK 때문에 핵심)
        List<Integer> dummyPostIds = em.createQuery(
                        "select p.postId from PostEntity p where p.title like :prefix",
                        Integer.class
                )
                .setParameter("prefix", DUMMY_PREFIX + "%")
                .getResultList();

        log.info("🧹 정리 대상 dummy posts={}", dummyPostIds.size());

        if (!dummyPostIds.isEmpty()) {
            // 1) post_images 먼저 삭제
            int deletedImages = em.createQuery(
                            "delete from PostImageEntity pi where pi.post.postId in :postIds"
                    )
                    .setParameter("postIds", dummyPostIds)
                    .executeUpdate();
            log.info("🧹 deleted post_images={}", deletedImages);

            // 2) comments 삭제 (post 기준)
            int deletedCommentsByPost = em.createQuery(
                            "delete from CommentEntity c where c.post.postId in :postIds"
                    )
                    .setParameter("postIds", dummyPostIds)
                    .executeUpdate();
            log.info("🧹 deleted comments(by post)={}", deletedCommentsByPost);

            // 3) posts 삭제
            int deletedPosts = em.createQuery(
                            "delete from PostEntity p where p.postId in :postIds"
                    )
                    .setParameter("postIds", dummyPostIds)
                    .executeUpdate();
            log.info("🧹 deleted posts={}", deletedPosts);
        }

        // 4) 혹시 “더미 댓글만 남는 케이스”까지 안전하게 정리 (post와 무관하게 content로 한번 더)
        int deletedCommentsByContent = em.createQuery(
                        "delete from CommentEntity c where c.content like :prefix"
                )
                .setParameter("prefix", DUMMY_PREFIX + "%")
                .executeUpdate();
        log.info("🧹 deleted comments(by content)={}", deletedCommentsByContent);

        // 5) 더미 카테고리 삭제
        int deletedCategories = em.createQuery(
                        "delete from CategoryEntity ca where ca.categoryName like :prefix"
                )
                .setParameter("prefix", DUMMY_PREFIX + "%")
                .executeUpdate();
        log.info("🧹 deleted categories={}", deletedCategories);

        // 6) 더미 유저 삭제
        //    (혹시 더미 유저가 남아있더라도, 더미 게시글/댓글은 위에서 다 지웠으니 FK 문제 거의 없음)
        int deletedUsers = em.createQuery(
                        "delete from UserEntity u " +
                                "where u.loginId like :loginPrefix " +
                                "   or u.nickname like :nickPrefix " +
                                "   or u.email like :emailPrefix"
                )
                .setParameter("loginPrefix", DUMMY_USER_LOGIN_PREFIX + "%")
                .setParameter("nickPrefix", DUMMY_USER_NICK_PREFIX + "%")
                .setParameter("emailPrefix", DUMMY_EMAIL_PREFIX + "%@test.com")
                .executeUpdate();
        log.info("🧹 deleted users={}", deletedUsers);

        em.flush();
        em.clear();

        log.info("🧹 더미 데이터 정리 완료");
    }

    // =======================
    // helper
    // =======================

    private UserEntity createDummyUser() {
        long now = System.currentTimeMillis();

        UserEntity user = UserEntity.builder()
                .loginId(DUMMY_USER_LOGIN_PREFIX + now)
                .password(passwordEncoder.encode("1234"))
                .email(DUMMY_EMAIL_PREFIX + now + "@test.com")
                .nickname(DUMMY_USER_NICK_PREFIX + now)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private List<CategoryEntity> createDummyCategories() {
        List<String> names = List.of(
                DUMMY_PREFIX + " 디자인",
                DUMMY_PREFIX + " 빈티지",
                DUMMY_PREFIX + " 키치"
        );

        List<CategoryEntity> result = new ArrayList<>();
        for (String name : names) {
            CategoryEntity saved = categoryRepository.save(
                    CategoryEntity.builder()
                            .categoryName(name)
                            .isActive(true)
                            .build()
            );
            result.add(saved);
        }
        return result;
    }
}
