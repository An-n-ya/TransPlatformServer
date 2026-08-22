package com.app.location;

/**
 * 逆地理编码结果 — 从百度返回中提取 city 作为 location 信息返回给前端。
 */
public record ReverseGeocodeVO(String location) {
}
