package com.app.feed;

import com.app.common.PageResult;
import com.app.content.Post;
import com.app.content.PostRepository;
import com.app.content.PostService;
import com.app.content.PostVO;
import com.app.user.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Value("${feed.max-size:500}")
    private int maxFeedSize;

    @Override
    public PageResult<PostVO> getFeed(Long userId, int page, int size) {
        String feedKey = FEED_KEY_PREFIX + userId;

        int start = page * size;
        int end = start + size - 1;

        Long total = stringRedisTemplate.opsForList().size(feedKey);
        if (total == null || total == 0) {
            return PageResult.empty();
        }

        List<String> postIdStrs = stringRedisTemplate.opsForList().range(feedKey, start, end);
        if (postIdStrs == null || postIdStrs.isEmpty()) {
            return PageResult.empty();
        }

        List<Long> postIds = postIdStrs.stream()
                .map(Long::valueOf)
                .toList();

        List<PostVO> posts = postService.getPostsByIds(postIds, userId);
        return PageResult.of(posts, page, size, total);
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
