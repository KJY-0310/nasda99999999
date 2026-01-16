package com.example.nasda.controller;

import com.example.nasda.dto.post.HomePostDto;
import com.example.nasda.service.AuthUserService;
import com.example.nasda.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final PostService postService;
    private final AuthUserService authUserService;

    // ✅ 메인 페이지: 처음에는 size개만 서버 렌더링
    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HomePostDto> postsPage = postService.getHomePostsByCategory(category, pageable);

        model.addAttribute("posts", postsPage.getContent());

        String nickname = authUserService.getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        model.addAttribute("category", (category == null || category.isBlank()) ? "전체" : category);
        model.addAttribute("hasNext", postsPage.hasNext());
        model.addAttribute("nextPage", postsPage.getNumber() + 1);
        model.addAttribute("size", size);

        return "index";
    }

    // ✅ 무한 스크롤 API: 다음 페이지 데이터만 JSON으로 내려줌
    @GetMapping("/api/posts")
    @ResponseBody
    public Page<HomePostDto> apiPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return postService.getHomePostsByCategory(category, pageable);
    }
}
