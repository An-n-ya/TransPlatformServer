package com.app.interaction;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import com.app.content.PostService;
import com.app.content.PostVO;
import com.app.interaction.Comment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final CommentRepository commentRepository;
    private final CollectionService collectionService;
    private final PostService postService;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;

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

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "点赞评论")
    public ApiResponse<Void> likeComment(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long commentId) {
        likeService.like(userId, "comment", commentId);
        return ApiResponse.success();
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "取消点赞评论")
    public ApiResponse<Void> unlikeComment(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long commentId) {
        likeService.unlike(userId, "comment", commentId);
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
    @Operation(summary = "获取帖文评论列表（分页，包含回复数）")
    public ApiResponse<PageResult<CommentVO>> getPostComments(@PathVariable Long postId,
                                                              @AuthenticationPrincipal Long userId,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(commentService.getPostComments(postId, userId, pageable));
    }

    // ========== 回复子评论 ==========

    @PostMapping("/comments/{commentId}/replies")
    @Operation(summary = "回复评论（创建子评论）")
    public ApiResponse<CommentVO> replyToComment(@AuthenticationPrincipal Long userId,
                                                  @PathVariable Long commentId,
                                                  @Valid @RequestBody CommentCreateRequest request) {
        // 查找父评论以获取 postId 和作者
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("评论不存在"));
        request.setPostId(parent.getPostId());
        request.setParentId(commentId);
        request.setReplyToUserId(parent.getUserId());
        return ApiResponse.success(commentService.createComment(userId, request));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "获取评论的回复列表（分页）")
    public ApiResponse<PageResult<CommentVO>> getCommentReplies(@PathVariable Long commentId,
                                                                @AuthenticationPrincipal Long userId,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(commentService.getCommentReplies(commentId, userId, pageable));
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

    // ========== 点赞/收藏列表 ==========

    @GetMapping("/users/me/liked-posts")
    @Operation(summary = "获取当前用户点赞过的帖文列表（分页）")
    public ApiResponse<PageResult<PostVO>> getLikedPosts(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Like> likes = likeRepository.findByUserIdAndTargetType(userId, "post", pageable);
        List<Long> postIds = likes.getContent().stream().map(Like::getTargetId).toList();
        List<PostVO> posts = postService.getPostsByIds(postIds, userId);
        return ApiResponse.success(PageResult.of(posts, likes.getNumber(), likes.getSize(), likes.getTotalElements()));
    }

    @GetMapping("/users/me/collected-posts")
    @Operation(summary = "获取当前用户收藏过的帖文列表（分页）")
    public ApiResponse<PageResult<PostVO>> getCollectedPosts(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Collection> collections = collectionRepository.findByUserId(userId, pageable);
        List<Long> postIds = collections.getContent().stream().map(Collection::getPostId).toList();
        List<PostVO> posts = postService.getPostsByIds(postIds, userId);
        return ApiResponse.success(PageResult.of(posts, collections.getNumber(), collections.getSize(), collections.getTotalElements()));
    }
}
