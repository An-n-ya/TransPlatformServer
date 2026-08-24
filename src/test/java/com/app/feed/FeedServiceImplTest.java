package com.app.feed;

import com.app.common.CursorPage;
import com.app.content.PostService;
import com.app.content.PostVO;
import com.app.user.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Feed 流测试：发帖推送（含作者本人）+ 游标分页读取。
 */
class FeedServiceImplTest {

    private StringRedisTemplate redis;
    private ListOperations<String, String> listOps;
    private FollowRepository followRepository;
    private PostService postService;
    private FeedServiceImpl feedService;

    @BeforeEach
    void setUp() throws Exception {
        redis = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        followRepository = mock(FollowRepository.class);
        postService = mock(PostService.class);
        when(redis.opsForList()).thenReturn(listOps);

        feedService = new FeedServiceImpl(redis, followRepository, postService, null);
        // maxFeedSize 由 @Value 注入，测试中通过反射设置
        Field maxFeedSize = FeedServiceImpl.class.getDeclaredField("maxFeedSize");
        maxFeedSize.setAccessible(true);
        maxFeedSize.set(feedService, 500);
    }

    // ==================== 发帖推送（含作者本人） ====================

    @Test
    void pushIncludesAuthorAndAllFollowers() {
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of(1L, 2L));

        feedService.pushPostToFollowers(100L, 3L);

        verify(listOps).leftPush("feed:1", "100");
        verify(listOps).leftPush("feed:2", "100");
        verify(listOps).leftPush("feed:3", "100");
        verify(listOps, times(3)).trim(anyString(), eq(0L), eq(499L));
    }

    @Test
    void pushIncludesAuthorEvenWithoutFollowers() {
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of());

        feedService.pushPostToFollowers(100L, 3L);

        verify(listOps).leftPush("feed:3", "100");
        verify(listOps, times(1)).trim(anyString(), eq(0L), eq(499L));
        verifyNoMoreInteractions(listOps);
    }

    @Test
    void authorNotPushedTwiceWhenAlsoInFollowerList() {
        when(followRepository.findFollowerIdsByFolloweeId(3L)).thenReturn(List.of(3L, 1L));

        feedService.pushPostToFollowers(100L, 3L);

        verify(listOps, times(1)).leftPush("feed:3", "100");
        verify(listOps).leftPush("feed:1", "100");
        verify(listOps, times(2)).trim(anyString(), eq(0L), eq(499L));
    }

    // ==================== 游标分页读取 ====================

    /** 生成 from..to 的 id 字符串列表（从大到小，最新在前） */
    private List<String> rangeIds(int from, int to) {
        List<String> list = new ArrayList<>();
        for (int i = from; i >= to; i--) {
            list.add(String.valueOf(i));
        }
        return list;
    }

    private PostVO vo(long id) {
        return PostVO.builder().id(id).content("post-" + id).build();
    }

    @Test
    void firstPageReturnsNewestPageAndNextCursor() {
        // 列表有 50 条（100..51），请求 size=20
        when(listOps.range("feed:1", 0, 49)).thenReturn(rangeIds(100, 51));
        when(postService.getPostsByIds(anyList(), eq(1L)))
                .thenAnswer(inv -> ((List<?>) inv.getArgument(0)).stream().map(id -> vo((Long) id)).toList());
        // hasMore 判断：最后一条可见帖文（81）之后还有 29 条
        when(listOps.indexOf("feed:1", "81")).thenReturn(19L);
        when(listOps.size("feed:1")).thenReturn(50L);

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 20);

        assertEquals(20, page.getContent().size());
        assertEquals(100L, page.getContent().get(0).getId());   // 最新在前
        assertEquals(81L, page.getContent().get(19).getId());   // 本页最后一条
        assertEquals(81L, page.getNextCursor());
        assertTrue(page.isHasMore());
    }

    @Test
    void continuesAfterCursorWithoutDuplicates() {
        // 用上一页的 nextCursor=81 继续：从 id=81 之后取下一批
        when(listOps.indexOf("feed:1", "81")).thenReturn(19L);
        when(listOps.range("feed:1", 20, 69)).thenReturn(rangeIds(80, 31));
        when(postService.getPostsByIds(anyList(), eq(1L)))
                .thenAnswer(inv -> ((List<?>) inv.getArgument(0)).stream().map(id -> vo((Long) id)).toList());
        // hasMore 判断：最后一条可见帖文（61）之后还有 10 条
        when(listOps.indexOf("feed:1", "61")).thenReturn(39L);
        when(listOps.size("feed:1")).thenReturn(50L);

        CursorPage<PostVO> page = feedService.getFeed(1L, 81L, 20);

        assertEquals(20, page.getContent().size());
        assertEquals(80L, page.getContent().get(0).getId());    // 紧接着上一条，无重复
        assertEquals(61L, page.getNextCursor());
        assertTrue(page.isHasMore());
    }

    @Test
    void scansMultipleBatchesToFillPageSkippingDeleted() {
        // 第一段 100..51 中只有 100..96 可见（95..51 已删除）
        when(listOps.range("feed:1", 0, 49)).thenReturn(rangeIds(100, 51));
        // 第二段 50..1 中只有 50..36 可见
        when(listOps.indexOf("feed:1", "51")).thenReturn(49L);
        when(listOps.range("feed:1", 50, 99)).thenReturn(rangeIds(50, 1));

        when(postService.getPostsByIds(anyList(), eq(1L))).thenAnswer(inv -> {
            List<?> ids = inv.getArgument(0);
            List<PostVO> vos = new ArrayList<>();
            for (Object id : ids) {
                long v = (Long) id;
                if ((v >= 96 && v <= 100) || (v >= 36 && v <= 50)) {
                    vos.add(vo(v));
                }
            }
            return vos;
        });
        // hasMore 判断：最后一条可见帖文（36）位于 index 64，后面还有 35 条
        when(listOps.indexOf("feed:1", "36")).thenReturn(64L);
        when(listOps.size("feed:1")).thenReturn(100L);

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 20);

        // 5 条（100..96）+ 15 条（50..36）= 20 条，跳过中间已删除的，一页填满
        assertEquals(20, page.getContent().size());
        assertEquals(100L, page.getContent().get(0).getId());
        assertEquals(36L, page.getContent().get(19).getId());
        assertEquals(36L, page.getNextCursor());
        assertTrue(page.isHasMore());
    }

    @Test
    void returnsEmptyWhenNothingVisible() {
        // 列表有数据但全部已删除
        when(listOps.range("feed:1", 0, 49)).thenReturn(rangeIds(10, 1));
        when(postService.getPostsByIds(anyList(), eq(1L))).thenReturn(List.of());

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 20);

        assertTrue(page.getContent().isEmpty());
        assertNull(page.getNextCursor());
        assertFalse(page.isHasMore());
    }

    @Test
    void returnsEmptyWhenCursorPostRemoved() {
        // 游标帖文已被 LTRIM 截断，LPOS 返回 null → 无更多
        when(listOps.indexOf("feed:1", "999")).thenReturn(null);

        CursorPage<PostVO> page = feedService.getFeed(1L, 999L, 20);

        assertTrue(page.getContent().isEmpty());
        assertFalse(page.isHasMore());
    }

    @Test
    void returnsEmptyWhenFeedEmpty() {
        when(listOps.range("feed:1", 0, 49)).thenReturn(List.of());

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 20);

        assertTrue(page.getContent().isEmpty());
        assertNull(page.getNextCursor());
        assertFalse(page.isHasMore());
    }

    @Test
    void hasMoreTrueWhenSmallSizeButMorePostsRemain() {
        // 回归用例：列表只有 8 条（8..1），size=3，本页只取 3 条但后面还有 5 条
        when(listOps.range("feed:1", 0, 49)).thenReturn(rangeIds(8, 1));
        when(postService.getPostsByIds(anyList(), eq(1L)))
                .thenAnswer(inv -> ((List<?>) inv.getArgument(0)).stream().map(id -> vo((Long) id)).toList());
        when(listOps.indexOf("feed:1", "6")).thenReturn(2L);   // 最后一条可见帖文 id=6 位于 index 2
        when(listOps.size("feed:1")).thenReturn(8L);

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 3);

        assertEquals(3, page.getContent().size());
        assertEquals(8L, page.getContent().get(0).getId());
        assertEquals(6L, page.getNextCursor());
        assertTrue(page.isHasMore());   // 后面还有 5 条，hasMore 必须为 true
    }

    @Test
    void hasMoreFalseWhenAllPostsShown() {
        // 列表 8 条全部展示完（size 足够大）→ hasMore=false
        when(listOps.range("feed:1", 0, 49)).thenReturn(rangeIds(8, 1));
        when(postService.getPostsByIds(anyList(), eq(1L)))
                .thenAnswer(inv -> ((List<?>) inv.getArgument(0)).stream().map(id -> vo((Long) id)).toList());
        when(listOps.indexOf("feed:1", "1")).thenReturn(7L);   // 最后一条 id=1 位于列表末尾 index 7
        when(listOps.size("feed:1")).thenReturn(8L);

        CursorPage<PostVO> page = feedService.getFeed(1L, null, 20);

        assertEquals(8, page.getContent().size());
        assertEquals(1L, page.getNextCursor());
        assertFalse(page.isHasMore());
    }
}
