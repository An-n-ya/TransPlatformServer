package com.app.interaction;

/**
 * 点赞 Service 接口
 */
public interface LikeService {

    /**
     * 点赞（支持帖文和评论）
     * @param userId     用户 ID
     * @param targetType 目标类型: post/comment
     * @param targetId   目标 ID
     */
    void like(Long userId, String targetType, Long targetId);

    /**
     * 取消点赞
     */
    void unlike(Long userId, String targetType, Long targetId);
}
