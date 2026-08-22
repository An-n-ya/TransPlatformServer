package com.app.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 BigDataCloud 逆地理编码响应解析逻辑（取 city，境外 countryCode → "境外"）。
 */
class BigDataCloudReverseGeocodeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReverseGeocodeVO parseCity(String body) throws Exception {
        BigDataCloudReverseGeocodeService service =
                new BigDataCloudReverseGeocodeService("dummy-key", objectMapper);
        Method method = BigDataCloudReverseGeocodeService.class
                .getDeclaredMethod("parseCity", String.class, String.class);
        method.setAccessible(true);
        try {
            return (ReverseGeocodeVO) method.invoke(service, body, "39.9,116.4");
        } catch (InvocationTargetException e) {
            // 反射调用会包装目标异常，解包后重新抛出，便于断言
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    @Test
    void parsesCityFromBigDataCloudResponse() throws Exception {
        // 与真实接口返回（localityLanguage=zh-Hans）结构一致，取 city
        String body = """
                {"latitude":39.9042,"longitude":116.4074,"countryName":"中华人民共和国",
                "countryCode":"CN","principalSubdivision":"北京市","city":"北京市","locality":"东城区"}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("北京市", vo.location());
    }

    @Test
    void fallsBackWhenCityEmpty() throws Exception {
        // city 为空时回退 principalSubdivision
        String body = """
                {"countryCode":"CN","principalSubdivision":"广东省","city":"","locality":"越秀区"}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("广东省", vo.location());
    }

    @Test
    void returnsRealCityForOverseas() throws Exception {
        // 境外坐标（如纽约），BigDataCloud 直接返回真实城市名
        String body = """
                {"countryCode":"US","countryName":"美国","city":"纽约","locality":"曼哈顿"}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("纽约", vo.location());
    }

    @Test
    void fallsBackToLocalityWhenNoCityAndNoCountry() throws Exception {
        // 海洋等无城市区域，回退到 locality
        String body = """
                {"countryCode":"","city":"","locality":"大西洋"}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("大西洋", vo.location());
    }

    @Test
    void throwsWhenApiErrorStatus() {
        // 无效 key / 配额超限等返回 {"status":403,...}
        String body = """
                {"status":403,"description":"access denied or your quota limit has been exceeded"}
                """;
        assertThrows(IllegalStateException.class, () -> parseCity(body));
    }
}
