package com.app.admin;

import java.time.LocalDate;

/**
 * 后台统计 Service 接口
 */
public interface StatisticService {

    /**
     * 统计某天的发帖数、新注册人数、活跃人数，以及最近 N 天的发帖/注册趋势（按天）。
     *
     * @param date 统计日期（yyyy-MM-dd），趋势以该日为终点向前取 N 天
     * @param days 趋势天数（默认 7，范围 1-31）
     * @return 统计数据
     */
    StatisticVO statistic(LocalDate date, int days);
}
