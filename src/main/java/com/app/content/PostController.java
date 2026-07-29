package com.app.content;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 内容控制器 — 帖文 CRUD
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "内容", description = "帖文的发布、详情、删除、列表")
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    @Operation(summary = "发布帖文")
    public ApiResponse<PostVO> createPost(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody PostCreateRequest request) {
        return ApiResponse.success(postService.createPost(userId, request));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "获取帖文详情")
    public ApiResponse<PostVO> getPost(@PathVariable Long postId,
                                       @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(postService.getPost(postId, userId));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "删除帖文（逻辑删除）")
    public ApiResponse<Void> deletePost(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long postId) {
        postService.deletePost(postId, userId);
        return ApiResponse.success();
    }

    @GetMapping("/users/{userId}/posts")
    @Operation(summary = "获取指定用户的帖文列表（分页）")
    public ApiResponse<PageResult<PostVO>> getUserPosts(@PathVariable Long userId,
                                                        @AuthenticationPrincipal Long currentUserId,
                                                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(postService.getUserPosts(userId, currentUserId, pageable));
    }
}
