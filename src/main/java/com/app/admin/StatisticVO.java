package com.app.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 后台统计数据视图 — 某天的核心指标 + 最近 N 天趋势
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "后台统计数据")
public class StatisticVO {

    @Schema(description = "统计日期（yyyy-MM-dd）", example = "2026-08-22")
    private String date;

    @Schema(description = "该日发帖数")
    private long postsCount;

    @Schema(description = "该日新注册人数")
    private long newUsersCount;

    @Schema(description = "该日活跃人数（当日有发帖/评论/点赞/收藏/关注行为的去重用户数）")
    private long activeUsersCount;

    @Schema(description = "最近 N 天发帖趋势（按天）")
    private List<DailyCount> postTrend;

    @Schema(description = "最近 N 天注册趋势（按天）")
    private List<DailyCount> registerTrend;
}
