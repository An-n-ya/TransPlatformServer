package com.app.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证百度逆地理编码响应解析逻辑（仅关注 city，为空时回退 province，240 → 境外）。
 */
class BaiduReverseGeocodeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReverseGeocodeVO parseCity(String body) throws Exception {
        BaiduReverseGeocodeService service = new BaiduReverseGeocodeService("dummy-key", objectMapper);
        Method method = BaiduReverseGeocodeService.class
                .getDeclaredMethod("parseCity", String.class, String.class);
        method.setAccessible(true);
        try {
            return (ReverseGeocodeVO) method.invoke(service, body, "39.95,116.51");
        } catch (InvocationTargetException e) {
            // 反射调用会包装目标异常，解包后重新抛出，便于断言
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    @Test
    void parsesCityFromBaiduResponse() throws Exception {
        // 与用户示例返回结构一致（截取 addressComponent）
        String body = """
                {"status":0,"result":{"location":{"lng":116.51484487904993,"lat":39.95133518263271},
                "formatted_address":"北京市朝阳区东风(地区)乡东坝路",
                "addressComponent":{"country":"中国","province":"北京市","city":"北京市","district":"朝阳区","street":"东坝路"}}}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("北京市", vo.location());
    }

    @Test
    void fallsBackToProvinceWhenCityEmpty() throws Exception {
        // 某些地区 city 为空（如部分地级市），应回退到 province
        String body = """
                {"status":0,"result":{"addressComponent":{"city":"","province":"广东省"}}}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("广东省", vo.location());
    }

    @Test
    void returnsOverseasWhenStatus240() throws Exception {
        // 境外坐标：百度仅支持境内地址，返回 status=240，location 记为“境外”
        String body = """
                {"status":240,"message":"query is invalid"}
                """;
        ReverseGeocodeVO vo = parseCity(body);
        assertEquals("境外", vo.location());
    }

    @Test
    void throwsWhenBaiduStatusNotZero() {
        String body = """
                {"status":230,"result":{"addressComponent":{"city":"北京市"}}}
                """;
        assertThrows(IllegalStateException.class, () -> parseCity(body));
    }

    @Test
    void throwsWhenNoCityOrProvince() {
        String body = """
                {"status":0,"result":{"addressComponent":{"city":"","province":""}}}
                """;
        assertThrows(IllegalStateException.class, () -> parseCity(body));
    }
}
