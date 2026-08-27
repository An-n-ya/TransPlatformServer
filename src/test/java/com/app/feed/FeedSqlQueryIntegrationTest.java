package com.app.feed;

import com.app.content.Post;
import com.app.content.PostRepository;
import com.app.user.User;
import com.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：验证广场/附近时间流的 JPQL 直查在真实数据库（SQLite）上可用，
 * 包括 null 游标与分页游标的组合。
 */
@SpringBootTest
@ActiveProfiles("test")
class FeedSqlQueryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User beijingUser;
    private User shanghaiUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        beijingUser = new User("bj_user", "北京用户", "p");
        beijingUser.setLocation("北京市");
        beijingUser = userRepository.save(beijingUser);

        shanghaiUser = new User("sh_user", "上海用户", "p");
        shanghaiUser.setLocation("上海市");
        shanghaiUser = userRepository.save(shanghaiUser);

        // 北京用户发 3 条，上海用户发 2 条
        postRepository.save(new Post(beijingUser.getId(), "bj-1", null, "北京市"));
        postRepository.save(new Post(beijingUser.getId(), "bj-2", null, "北京市"));
        postRepository.save(new Post(beijingUser.getId(), "bj-3", null, "北京市"));
        postRepository.save(new Post(shanghaiUser.getId(), "sh-1", null, "上海市"));
        postRepository.save(new Post(shanghaiUser.getId(), "sh-2", null, "上海市"));
    }

    @Test
    void plazaReturnsAllPostsNewestFirst() {
        List<Post> posts = postRepository.findPlazaFeed(null, PageRequest.of(0, 10));
        assertEquals(5, posts.size());
        // 按 id 倒序：最新在最前
        for (int i = 0; i < posts.size() - 1; i++) {
            assertTrue(posts.get(i).getId() > posts.get(i + 1).getId());
        }
    }

    @Test
    void plazaCursorFiltersOlderPosts() {
        List<Post> all = postRepository.findPlazaFeed(null, PageRequest.of(0, 10));
        Long cursor = all.get(2).getId();

        List<Post> rest = postRepository.findPlazaFeed(cursor, PageRequest.of(0, 10));
        assertEquals(2, rest.size());
        for (Post p : rest) {
            assertTrue(p.getId() < cursor);
        }
    }

    @Test
    void plazaCursorOlderThanAllReturnsEmpty() {
        List<Post> all = postRepository.findPlazaFeed(null, PageRequest.of(0, 10));
        Long cursor = all.get(all.size() - 1).getId(); // 最旧一条帖文的 id
        // 游标已是最后一页的最旧帖文，其后再无更旧帖文
        assertTrue(postRepository.findPlazaFeed(cursor, PageRequest.of(0, 10)).isEmpty());
    }

    @Test
    void nearbyFiltersByUserLocation() {
        List<Post> posts = postRepository.findNearbyFeed("北京市", null, PageRequest.of(0, 10));
        assertEquals(3, posts.size());
        assertTrue(posts.stream().allMatch(p -> p.getUserId().equals(beijingUser.getId())));

        List<Post> shPosts = postRepository.findNearbyFeed("上海市", null, PageRequest.of(0, 10));
        assertEquals(2, shPosts.size());
        assertTrue(shPosts.stream().allMatch(p -> p.getUserId().equals(shanghaiUser.getId())));
    }

    @Test
    void nearbyCursorFiltersOlderPosts() {
        List<Post> bj = postRepository.findNearbyFeed("北京市", null, PageRequest.of(0, 10));
        Long cursor = bj.get(1).getId();

        List<Post> rest = postRepository.findNearbyFeed("北京市", cursor, PageRequest.of(0, 10));
        assertEquals(1, rest.size());
        assertTrue(rest.get(0).getId() < cursor);
    }

    @Test
    void nearbyUnknownLocationReturnsEmpty() {
        assertTrue(postRepository.findNearbyFeed("广州市", null, PageRequest.of(0, 10)).isEmpty());
        assertNull(postRepository.findNearbyFeed("广州市", null, PageRequest.of(0, 10)).stream().findFirst().orElse(null));
    }
}
