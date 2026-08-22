package com.app.admin;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import com.app.content.PostService;
import com.app.content.PostVO;
import com.app.interaction.CommentService;
import com.app.interaction.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台内容控制器 — 帖文 / 评论的管理（查询与逻辑删除）
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
@Tag(name = "管理后台-内容", description = "管理员帖文、评论查询与逻辑删除")
public class AdminContentController {

    private final PostService postService;
    private final CommentService commentService;

    @GetMapping("/posts")
    @Operation(summary = "管理员查询帖文列表（可按用户/内容/状态过滤，分页）")
    public ApiResponse<PageResult<PostVO>> listPosts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(postService.adminListPosts(userId, content, status, pageable));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "管理员删除帖文（逻辑删除，任意用户的帖文）")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePostByAdmin(postId);
        return ApiResponse.success();
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "管理员获取帖文的评论列表（分页，含回复）")
    public ApiResponse<PageResult<CommentVO>> getPostComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(commentService.getPostComments(postId, null, pageable));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "管理员删除评论（逻辑删除，任意用户的评论）")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
        return ApiResponse.success();
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "管理员获取评论的回复列表（分页）")
    public ApiResponse<PageResult<CommentVO>> getCommentReplies(
            @PathVariable Long commentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(commentService.getCommentReplies(commentId, null, pageable));
    }
}
