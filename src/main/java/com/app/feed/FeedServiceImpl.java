package com.app.feed;

import com.app.common.CursorPage;
import com.app.content.Post;
import com.app.content.PostRepository;
import com.app.content.PostService;
import com.app.content.PostVO;
import com.app.user.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Feed 流实现 — 写扩散（Push）模式
 *
 * 数据存储格式:
 *   feed:{userId} → Redis List<PostId>，最新在前，LTRIM 保留最近 N 条
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final StringRedisTemplate stringRedisTemplate;
    private final FollowRepository followRepository;
    private final PostService postService;
    private final PostRepository postRepository;

    private static final String FEED_KEY_PREFIX = "feed:";

    /** 每次扫描的批次大小：跳过已删除/过滤掉的帖文，尽量填满一页 */
    private static final int FEED_FETCH_BATCH = 50;

    @Value("${feed.max-size:500}")
    private int maxFeedSize;

    @Override
    public CursorPage<PostVO> getFeed(Long userId, Long cursor, int size) {
        String feedKey = FEED_KEY_PREFIX + userId;
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();

        List<PostVO> result = new ArrayList<>();
        // 锚点 = 当前扫描到的最后一条原始 id；null 表示从列表头部（最新）开始
        String anchorId = (cursor != null) ? cursor.toString() : null;
        String lastVisibleId = null;
        boolean exhausted = false;

        // 每批至少取 size 条原始 id，保证一页尽量填满（即使中间混有已删除帖文）
        int fetchBatch = Math.max(size, FEED_FETCH_BATCH);

        while (result.size() < size && !exhausted) {
            List<String> rawIds = fetchIdsAfter(feedKey, listOps, anchorId, fetchBatch);
            if (rawIds == null || rawIds.isEmpty()) {
                // 游标帖文已被移除（LTRIM 截断/删除）或已到列表末尾
                exhausted = true;
                break;
            }
            // 锚点推进为本次取到的最后一条原始 id，保证下一批继续向后、不重复
            anchorId = rawIds.get(rawIds.size() - 1);

            // 按原始顺序批量查库（内部会过滤 status != 1 的已删除帖文）
            List<Long> postIds = rawIds.stream().map(Long::valueOf).toList();
            List<PostVO> visible = postService.getPostsByIds(postIds, userId);
            if (!visible.isEmpty()) {
                // 只取本页还需要的条数，避免超出一页
                List<PostVO> take = visible.size() <= size - result.size()
                        ? visible
                        : visible.subList(0, size - result.size());
                result.addAll(take);
                lastVisibleId = take.get(take.size() - 1).getId().toString();
            }

            // 返回数量不足一批说明已到列表末尾
            if (rawIds.size() < fetchBatch) {
                exhausted = true;
            }
        }

        if (result.isEmpty()) {
            return CursorPage.empty();
        }

        // hasMore = 最后一条可见帖文之后是否还有原始项。
        // 不能用“取满一批/取满 size”判断：当列表不足一批（如 size 小、列表短）时，
        // 本页可能只取了 size 条但后面仍有更多。
        Long lastPos = listOps.indexOf(feedKey, lastVisibleId);
        Long listSize = listOps.size(feedKey);
        boolean hasMore = lastPos != null && listSize != null && lastPos < listSize - 1;

        return new CursorPage<>(result, Long.valueOf(lastVisibleId), hasMore);
    }

    /**
     * 取锚点之后的一批 id；anchorId 为 null 时从列表头部取。
     * 游标帖文不存在时返回 null。
     */
    private List<String> fetchIdsAfter(String feedKey, ListOperations<String, String> listOps,
                                       String anchorId, int batch) {
        if (anchorId == null) {
            return listOps.range(feedKey, 0, batch - 1);
        }
        Long pos = listOps.indexOf(feedKey, anchorId);
        if (pos == null) {
            return null;
        }
        return listOps.range(feedKey, pos + 1, pos + batch);
    }

    @Override
    public void pushPostToFollowers(Long postId, Long userId) {
        List<Long> followerIds = followRepository.findFollowerIdsByFolloweeId(userId);

        // 推送给所有粉丝 + 作者本人，确保作者自己的新帖也出现在自己的 feed
        // 用 LinkedHashSet 去重（防止作者同时在粉丝列表中），并保持插入顺序
        Set<Long> targetIds = new LinkedHashSet<>(followerIds);
        targetIds.add(userId);

        String postIdStr = postId.toString();
        for (Long targetId : targetIds) {
            String feedKey = FEED_KEY_PREFIX + targetId;
            stringRedisTemplate.opsForList().leftPush(feedKey, postIdStr);
            stringRedisTemplate.opsForList().trim(feedKey, 0, maxFeedSize - 1);
        }

        log.info("Pushed post {} to {} users ({} followers + author)", postId, targetIds.size(), followerIds.size());
    }

    @Override
    public void pullFolloweePosts(Long followerId, Long followeeId) {
        // 新关注时，拉取被关注者最近的 50 条帖文到 Feed
        // 使用 PageRequest 获取最近的帖文
        List<Post> recentPosts = postRepository
                .findByUserIdAndStatus(followeeId, 1, PageRequest.of(0, 50))
                .getContent();

        String feedKey = FEED_KEY_PREFIX + followerId;
        // 倒序插入，保持时间顺序（最新的在最前）
        for (int i = recentPosts.size() - 1; i >= 0; i--) {
            stringRedisTemplate.opsForList().leftPush(feedKey, recentPosts.get(i).getId().toString());
        }
        stringRedisTemplate.opsForList().trim(feedKey, 0, maxFeedSize - 1);

        log.info("Pulled {} recent posts from user {} to follower {}'s feed",
                recentPosts.size(), followeeId, followerId);
    }

    @Override
    public void removeFolloweePosts(Long followerId, Long followeeId) {
        String feedKey = FEED_KEY_PREFIX + followerId;

        // 查询该用户的帖文 ID
        List<Post> userPosts = postRepository
                .findByUserIdAndStatus(followeeId, 1, PageRequest.of(0, 200))
                .getContent();
        Set<String> postIdsToRemove = userPosts.stream()
                .map(p -> p.getId().toString())
                .collect(Collectors.toSet());

        for (String postIdStr : postIdsToRemove) {
            stringRedisTemplate.opsForList().remove(feedKey, 0, postIdStr);
        }

        log.info("Removed {} posts of user {} from follower {}'s feed",
                postIdsToRemove.size(), followeeId, followerId);
    }
}
