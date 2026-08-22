package com.app.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证逆地理编码编排：默认 BigDataCloud，失败时回退百度。
 */
class ReverseGeocodeServiceImplTest {

    private final BigDataCloudReverseGeocodeService bigDataCloud = mock(BigDataCloudReverseGeocodeService.class);
    private final BaiduReverseGeocodeService baidu = mock(BaiduReverseGeocodeService.class);
    private final ReverseGeocodeServiceImpl service = new ReverseGeocodeServiceImpl(bigDataCloud, baidu);

    @Test
    void usesBigDataCloudByDefault() {
        ReverseGeocodeVO expected = new ReverseGeocodeVO("北京市");
        when(bigDataCloud.resolve(39.9, 116.4)).thenReturn(expected);

        assertEquals(expected, service.reverseGeocode(39.9, 116.4));
        verifyNoInteractions(baidu);
    }

    @Test
    void fallsBackToBaiduWhenBigDataCloudFails() {
        ReverseGeocodeVO expected = new ReverseGeocodeVO("境外");
        when(bigDataCloud.resolve(40.71, -74.00))
                .thenThrow(new IllegalStateException("BigDataCloud 返回错误 status=403"));
        when(baidu.resolve(40.71, -74.00)).thenReturn(expected);

        assertEquals(expected, service.reverseGeocode(40.71, -74.00));
        verify(baidu).resolve(40.71, -74.00);
    }

    @Test
    void propagatesErrorWhenBothFail() {
        when(bigDataCloud.resolve(1.0, 2.0))
                .thenThrow(new IllegalStateException("BigDataCloud failed"));
        when(baidu.resolve(1.0, 2.0))
                .thenThrow(new IllegalStateException("未配置 BAIDU_API_KEY，无法进行逆地理编码"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.reverseGeocode(1.0, 2.0));
        assertSame("未配置 BAIDU_API_KEY，无法进行逆地理编码", ex.getMessage());
    }
}
