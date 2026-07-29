package com.app.interaction;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 互动控制器 — 点赞、评论、收藏
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "互动", description = "点赞/取消点赞、评论/回复、收藏/取消收藏")
public class InteractionController {

    private final LikeService likeService;
    private final CommentService commentService;
    private final CollectionService collectionService;

    // ========== 点赞 ==========

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "点赞帖文")
    public ApiResponse<Void> likePost(@AuthenticationPrincipal Long userId,
                                      @PathVariable Long postId) {
        likeService.like(userId, "post", postId);
        return ApiResponse.success();
    }

    @DeleteMapping("/posts/{postId}/like")
    @Operation(summary = "取消点赞帖文")
    public ApiResponse<Void> unlikePost(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long postId) {
        likeService.unlike(userId, "post", postId);
        return ApiResponse.success();
    }

    // ========== 评论 ==========

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "发表评论")
    public ApiResponse<CommentVO> createComment(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long postId,
                                                @Valid @RequestBody CommentCreateRequest request) {
        request.setPostId(postId);
        return ApiResponse.success(commentService.createComment(userId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除评论")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long commentId) {
        commentService.deleteComment(commentId, userId);
        return ApiResponse.success();
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "获取帖文评论列表（分页）")
    public ApiResponse<PageResult<CommentVO>> getPostComments(@PathVariable Long postId,
                                                              @AuthenticationPrincipal Long userId,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(commentService.getPostComments(postId, userId, pageable));
    }

    // ========== 收藏 ==========

    @PostMapping("/posts/{postId}/collect")
    @Operation(summary = "收藏帖文")
    public ApiResponse<Void> collectPost(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long postId) {
        collectionService.collect(userId, postId);
        return ApiResponse.success();
    }

    @DeleteMapping("/posts/{postId}/collect")
    @Operation(summary = "取消收藏帖文")
    public ApiResponse<Void> uncollectPost(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long postId) {
        collectionService.uncollect(userId, postId);
        return ApiResponse.success();
    }
}
