package com.app.location;

import com.app.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 位置解析控制器 — 逆地理编码：GPS 坐标 → 城市 location
 */
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Validated
@Tag(name = "位置解析", description = "逆地理编码，将 GPS 坐标转换为城市信息")
public class ReverseGeocodeController {

    private final ReverseGeocodeService reverseGeocodeService;

    @GetMapping("/reverse-geocode")
    @Operation(summary = "逆地理编码（GPS 坐标转城市）",
            description = "默认调用 BigDataCloud 服务（失败时回退百度），仅返回 city 作为 location 信息")
    public ApiResponse<ReverseGeocodeVO> reverseGeocode(
            @RequestParam
            @DecimalMin(value = "-90", message = "纬度不能小于 -90")
            @DecimalMax(value = "90", message = "纬度不能大于 90")
            double latitude,

            @RequestParam
            @DecimalMin(value = "-180", message = "经度不能小于 -180")
            @DecimalMax(value = "180", message = "经度不能大于 180")
            double longitude) {

        return ApiResponse.success(reverseGeocodeService.reverseGeocode(latitude, longitude));
    }
}
