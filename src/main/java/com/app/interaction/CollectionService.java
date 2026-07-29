package com.app.interaction;

/**
 * 收藏 Service 接口
 */
public interface CollectionService {

    /**
     * 收藏帖文
     */
    void collect(Long userId, Long postId);

    /**
     * 取消收藏
     */
    void uncollect(Long userId, Long postId);
}
