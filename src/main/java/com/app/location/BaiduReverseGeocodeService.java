package com.app.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 基于百度地图逆地理编码 API 的实现。
 * <p>
 * API Key 从环境变量 {@code BAIDU_API_KEY} 读取。
 * 仅关注返回结果中的 city 字段，将其作为 location 信息返回；
 * 百度仅支持境内地址，境外坐标返回 status=240，此时 location 记为"境外"。
 */
@Slf4j
@Service
public class BaiduReverseGeocodeService {

    private static final String BAIDU_REVERSE_GEOCODING_URL =
            "https://api.map.baidu.com/reverse_geocoding/v3/";

    /** 百度仅支持境内地址，境外坐标返回 status=240，此时 location 记为境外 */
    private static final int BAIDU_STATUS_OVERSEAS = 240;
    /** 境外坐标对应的 location 值 */
    private static final String OVERSEAS_LOCATION = "境外";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public BaiduReverseGeocodeService(@Value("${BAIDU_API_KEY:}") String apiKey,
                                      ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(BAIDU_REVERSE_GEOCODING_URL).build();
    }

    /**
     * 逆地理编码：GPS 坐标 → 城市。
     *
     * @throws IllegalStateException 未配置 API Key / 请求或解析失败
     */
    public ReverseGeocodeVO resolve(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 BAIDU_API_KEY，无法进行逆地理编码");
        }

        String location = latitude + "," + longitude;
        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("ak", apiKey)
                            .queryParam("output", "json")
                            .queryParam("coordtype", "wgs84ll")
                            .queryParam("location", location)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Baidu reverse geocoding request failed for {}: {}", location, e.getMessage());
            throw new IllegalStateException("逆地理编码请求失败，请稍后再试", e);
        }

        return parseCity(body, location);
    }

    /**
     * 解析百度响应，仅提取 city（为空时回退到 province）。
     */
    private ReverseGeocodeVO parseCity(String body, String location) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int status = root.path("status").asInt(-1);
            if (status == BAIDU_STATUS_OVERSEAS) {
                // 境外坐标，百度不返回境内地址，直接记为境外
                log.info("Overseas coordinates detected for {}: status={}", location, status);
                return new ReverseGeocodeVO(OVERSEAS_LOCATION);
            }
            if (status != 0) {
                log.error("Baidu reverse geocoding error for {}: status={}, body={}",
                        location, status, body);
                throw new IllegalStateException("逆地理编码失败（百度返回 status=" + status + "）");
            }

            JsonNode addressComponent = root.path("result").path("addressComponent");
            String city = addressComponent.path("city").asText("").trim();
            // 直辖市等场景 city 可能为空，回退到 province
            if (city.isEmpty()) {
                city = addressComponent.path("province").asText("").trim();
            }
            if (city.isEmpty()) {
                log.warn("No city resolved from Baidu response for {}", location);
                throw new IllegalStateException("无法解析该坐标对应的城市");
            }
            return new ReverseGeocodeVO(city);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Baidu reverse geocoding response for {}", location, e);
            throw new IllegalStateException("逆地理编码响应解析失败", e);
        }
    }
}
