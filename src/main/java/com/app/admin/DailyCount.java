package com.app.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按天统计项 — 某一天的计数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "按天统计项")
public class DailyCount {

    @Schema(description = "日期（yyyy-MM-dd）", example = "2026-08-22")
    private String date;

    @Schema(description = "当日数量")
    private long count;
}
