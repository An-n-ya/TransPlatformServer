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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 内容控制器 — 帖文 CRUD
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "内容", description = "帖文的发布、详情、删除、列表")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "发布帖文（JSON 方式，images 传已上传的图片 URL）")
    public ApiResponse<PostVO> createPostJson(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        return ApiResponse.success(postService.createPost(userId, request));
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "发布帖文（multipart 方式，images 字段传图片文件）")
    public ApiResponse<PostVO> createPostMultipart(
            @AuthenticationPrincipal Long userId,
            @RequestParam("content") String content,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.success(postService.createPost(userId, content, location, images));
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
