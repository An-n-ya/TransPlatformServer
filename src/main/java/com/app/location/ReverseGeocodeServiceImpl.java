package com.app.location;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 逆地理编码服务实现 — 默认使用 BigDataCloud 服务，失败时回退到百度地图。
 */
@Slf4j
@Service
public class ReverseGeocodeServiceImpl implements ReverseGeocodeService {

    private final BigDataCloudReverseGeocodeService bigDataCloudService;
    private final BaiduReverseGeocodeService baiduService;

    public ReverseGeocodeServiceImpl(BigDataCloudReverseGeocodeService bigDataCloudService,
                                     BaiduReverseGeocodeService baiduService) {
        this.bigDataCloudService = bigDataCloudService;
        this.baiduService = baiduService;
    }

    @Override
    public ReverseGeocodeVO reverseGeocode(double latitude, double longitude) {
        // 默认 BigDataCloud，请求/解析失败时回退到百度
        try {
            return bigDataCloudService.resolve(latitude, longitude);
        } catch (Exception e) {
            log.warn("BigDataCloud reverse geocoding failed (lat={}, lng={}), falling back to Baidu: {}",
                    latitude, longitude, e.getMessage());
            return baiduService.resolve(latitude, longitude);
        }
    }
}
