package com.app.interaction;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;

/**
 * 评论 Service 接口
 */
public interface CommentService {

    /**
     * 发表评论
     * @param userId  用户 ID
     * @param request 评论请求
     * @return 评论视图
     */
    CommentVO createComment(Long userId, CommentCreateRequest request);

    /**
     * 删除评论
     * @param commentId  评论 ID
     * @param userId     当前用户 ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 获取帖文的评论列表（分页，顶级评论）
     */
    PageResult<CommentVO> getPostComments(Long postId, Long currentUserId, Pageable pageable);

    /**
     * 获取评论的回复列表
     */
    PageResult<CommentVO> getCommentReplies(Long commentId, Long currentUserId, Pageable pageable);
}
