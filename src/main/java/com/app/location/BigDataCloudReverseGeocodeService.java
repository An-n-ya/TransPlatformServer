package com.app.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 基于 BigDataCloud 逆地理编码 API 的实现。
 * <p>
 * API Key 从环境变量 {@code BIG_DATA_CLOUD_KEY_API} 读取。
 * 仅关注返回体中的 city 字段作为 location；BigDataCloud 支持全球，境外坐标直接返回真实城市。
 */
@Slf4j
@Service
public class BigDataCloudReverseGeocodeService {

    private static final String BIG_DATA_CLOUD_REVERSE_GEOCODING_URL =
            "https://api-bdc.net/data/reverse-geocode";

    /** 返回中文（简体）城市名，如 北京市 */
    private static final String LOCALITY_LANGUAGE = "zh-Hans";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public BigDataCloudReverseGeocodeService(@Value("${BIG_DATA_CLOUD_KEY_API:}") String apiKey,
                                             ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(BIG_DATA_CLOUD_REVERSE_GEOCODING_URL).build();
    }

    /**
     * 逆地理编码：GPS 坐标 → 城市。
     *
     * @throws IllegalStateException 未配置 API Key / 请求或解析失败
     */
    public ReverseGeocodeVO resolve(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 BIG_DATA_CLOUD_KEY_API，无法进行逆地理编码");
        }

        String location = latitude + "," + longitude;
        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("key", apiKey)
                            .queryParam("localityLanguage", LOCALITY_LANGUAGE)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("BigDataCloud reverse geocoding request failed for {}: {}", location, e.getMessage());
            throw new IllegalStateException("逆地理编码请求失败，请稍后再试", e);
        }

        return parseCity(body, location);
    }

    /**
     * 解析 BigDataCloud 响应，仅提取 city（为空时回退 principalSubdivision / locality）。
     */
    private ReverseGeocodeVO parseCity(String body, String location) {
        try {
            JsonNode root = objectMapper.readTree(body);

            // API 错误（无效 key / 配额超限等）返回 {"status":403,...}，视为失败
            if (root.hasNonNull("status") && root.path("status").asInt() != 0) {
                log.error("BigDataCloud reverse geocoding error for {}: status={}, body={}",
                        location, root.path("status").asInt(), body);
                throw new IllegalStateException(
                        "BigDataCloud 返回错误 status=" + root.path("status").asInt());
            }

            // 优先取 city，为空时回退 principalSubdivision / locality（BigDataCloud 支持全球，直接返回真实城市）
            String city = root.path("city").asText("").trim();
            if (city.isEmpty()) {
                city = root.path("principalSubdivision").asText("").trim();
            }
            if (city.isEmpty()) {
                city = root.path("locality").asText("").trim();
            }
            if (city.isEmpty()) {
                log.warn("No city resolved from BigDataCloud response for {}", location);
                throw new IllegalStateException("无法解析该坐标对应的城市");
            }
            return new ReverseGeocodeVO(city);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse BigDataCloud reverse geocoding response for {}", location, e);
            throw new IllegalStateException("逆地理编码响应解析失败", e);
        }
    }
}
