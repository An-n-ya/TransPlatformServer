package com.app.feed;

import com.app.common.CursorPage;
import com.app.content.PostVO;

/**
 * Feed 流 Service 接口
 */
public interface FeedService {

    /**
     * 获取首页 Feed 流（基于关注关系的时间线，游标分页）
     *
     * @param userId 当前用户 ID
     * @param cursor 游标 = 上一页最后一条帖文 ID；null 表示第一页（最新）
     * @param size   每页大小
     * @return 帖文列表 + 下一页游标
     */
    CursorPage<PostVO> getFeed(Long userId, Long cursor, int size);

    /**
     * 用户发帖后，推送到所有粉丝以及作者本人的 Feed 列表
     * @param postId 帖文 ID
     * @param userId 发帖用户 ID
     */
    void pushPostToFollowers(Long postId, Long userId);

    /**
     * 用户关注某人时，拉取该用户最近的帖文到自己的 Feed
     * @param followerId 关注者 ID
     * @param followeeId 被关注者 ID
     */
    void pullFolloweePosts(Long followerId, Long followeeId);

    /**
     * 用户取关某人时，移除该用户的帖文 ID 从 Feed 列表
     * @param followerId 关注者 ID
     * @param followeeId 被关注者 ID
     */
    void removeFolloweePosts(Long followerId, Long followeeId);
}
