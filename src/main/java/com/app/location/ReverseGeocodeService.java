package com.app.location;

/**
 * 逆地理编码服务 — 将 GPS 地理位置转换为城市信息。
 * 默认使用 BigDataCloud，失败时回退到百度地图。
 */
public interface ReverseGeocodeService {

    /**
     * 逆地理编码：GPS 坐标 → 城市
     *
     * @param latitude  纬度（-90 ~ 90）
     * @param longitude 经度（-180 ~ 180）
     * @return 城市信息（取百度返回中的 city，若为空则回退到 province）
     */
    ReverseGeocodeVO reverseGeocode(double latitude, double longitude);
}
