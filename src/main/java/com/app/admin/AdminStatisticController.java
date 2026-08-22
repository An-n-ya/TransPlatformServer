package com.app.admin;

import com.app.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 管理后台统计控制器 — 某天核心指标 + 最近 N 天趋势（按天）
 */
@RestController
@RequestMapping("/admin/v1/statistic")
@RequiredArgsConstructor
@Validated
@Tag(name = "管理后台-统计", description = "某天发帖数/新注册/活跃人数，以及最近 N 天发帖与注册趋势")
public class AdminStatisticController {

    private final StatisticService statisticService;

    @GetMapping
    @Operation(summary = "后台数据统计（可按日期查询）")
    public ApiResponse<StatisticVO> statistic(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "趋势天数最小 1")
            @Max(value = 31, message = "趋势天数最大 31") int days) {

        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.success(statisticService.statistic(target, days));
    }
}
