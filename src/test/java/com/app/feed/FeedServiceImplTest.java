package com.app.feed;

import com.app.user.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证发帖推送逻辑：除了推送给所有粉丝，作者自己的新帖也要进入自己的 feed。
 */
class FeedServiceImplTest {

    private StringRedisTemplate redis;
    private ListOperations<String, String> listOps;
    private FollowRepository followRepository;
    private FeedServiceImpl feedService;

    @BeforeEach
    void setUp() throws Exception {
        redis = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        followRepository = mock(FollowRepository.class);
        when(redis.opsForList()).thenReturn(listOps);

        feedService = new FeedServiceImpl(redis, followRepository, null, null);
        // maxFeedSize 由 @Value 注入，测试中通过反射设置
        Field maxFeedSize = FeedServiceImpl.class.getDeclaredField("maxFeedSize");
        maxFeedSize.setAccessible(true);
        maxFeedSize.set(feedService, 500);
    }

    @Test
    void pushIncludesAuthorAndAllFollowers() {
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of(1L, 2L));

        feedService.pushPostToFollowers(100L, 3L);

        // 两个粉丝 + 作者本人，共 3 个 feed 都收到 postId=100
        verify(listOps).leftPush("feed:1", "100");
        verify(listOps).leftPush("feed:2", "100");
        verify(listOps).leftPush("feed:3", "100");
        verify(listOps, times(3)).trim(anyString(), eq(0L), eq(499L));
    }

    @Test
    void pushIncludesAuthorEvenWithoutFollowers() {
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of());

        feedService.pushPostToFollowers(100L, 3L);

        // 没有粉丝时，作者自己的 feed 也要收到新帖
        verify(listOps).leftPush("feed:3", "100");
        verify(listOps, times(1)).trim(anyString(), eq(0L), eq(499L));
        verifyNoMoreInteractions(listOps);
    }

    @Test
    void authorNotPushedTwiceWhenAlsoInFollowerList() {
        // 极端情况：作者同时出现在粉丝列表（自关注），应去重只 push 一次
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of(3L, 1L));

        feedService.pushPostToFollowers(100L, 3L);

        verify(listOps, times(1)).leftPush("feed:3", "100");
        verify(listOps).leftPush("feed:1", "100");
        verify(listOps, times(2)).trim(anyString(), eq(0L), eq(499L));
    }
}
