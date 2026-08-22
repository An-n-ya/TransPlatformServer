package com.app.admin;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台统计实现 — 基于各表 created_at 按天聚合。
 *
 * 兼容数据库中 created_at 的两种存储格式：
 *   1. 日期时间字符串（如 '2026-08-21 07:21:45'）→ substr 取前 10 位
 *   2. 时间戳毫秒（如 1787281960985）→ 除以 1000 后按 unixepoch 转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final EntityManager entityManager;

    /** 统一提取 created_at 的日期部分（yyyy-MM-dd） */
    private static final String DAY_EXPR =
            "CASE WHEN typeof(created_at) IN ('integer','real') " +
            "THEN date(cast(created_at as integer)/1000, 'unixepoch') " +
            "ELSE substr(created_at, 1, 10) END";

    @Override
    @Transactional(readOnly = true)
    public StatisticVO statistic(LocalDate date, int days) {
        int n = Math.max(1, Math.min(days, 31));
        LocalDate start = date.minusDays(n - 1L);

        long posts = countByDay("posts", date);
        long newUsers = countByDay("users", date);
        long active = countActiveByDay(date);

        List<DailyCount> postTrend = trendByDay("posts", start, date);
        List<DailyCount> registerTrend = trendByDay("users", start, date);

        return StatisticVO.builder()
                .date(date.toString())
                .postsCount(posts)
                .newUsersCount(newUsers)
                .activeUsersCount(active)
                .postTrend(postTrend)
                .registerTrend(registerTrend)
                .build();
    }

    /** 统计某表某天的行数 */
    private long countByDay(String table, LocalDate day) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + DAY_EXPR + " = :day";
        Number n = (Number) entityManager.createNativeQuery(sql)
                .setParameter("day", day.toString())
                .getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    /** 统计某天活跃人数：当日有发帖/评论/点赞/收藏/关注行为的去重用户数 */
    @SuppressWarnings("unchecked")
    private long countActiveByDay(LocalDate day) {
        String sql = "SELECT COUNT(*) FROM ("
                + " SELECT user_id FROM posts WHERE " + DAY_EXPR + " = :day"
                + " UNION SELECT user_id FROM comments WHERE " + DAY_EXPR + " = :day"
                + " UNION SELECT user_id FROM likes WHERE " + DAY_EXPR + " = :day"
                + " UNION SELECT user_id FROM collections WHERE " + DAY_EXPR + " = :day"
                + " UNION SELECT follower_id FROM follows WHERE " + DAY_EXPR + " = :day"
                + ")";
        Number n = (Number) entityManager.createNativeQuery(sql)
                .setParameter("day", day.toString())
                .getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    /** 按天分组统计某表的行数，并补全区间内无数据的天（计 0） */
    @SuppressWarnings("unchecked")
    private List<DailyCount> trendByDay(String table, LocalDate start, LocalDate end) {
        String sql = "SELECT " + DAY_EXPR + " AS day, COUNT(*) AS cnt FROM " + table
                + " WHERE " + DAY_EXPR + " BETWEEN :start AND :end GROUP BY 1";
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("start", start.toString())
                .setParameter("end", end.toString())
                .getResultList();

        Map<String, Long> countByDate = rows.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).longValue()));

        List<DailyCount> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.add(DailyCount.builder()
                    .date(d.toString())
                    .count(countByDate.getOrDefault(d.toString(), 0L))
                    .build());
        }
        return result;
    }
}
