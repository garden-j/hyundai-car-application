package com.example.application.data.direct.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.application.data.direct.api.dto.WeatherDto;
import com.example.application.data.direct.common.ExternalServiceUnavailableException;
import com.example.application.data.direct.external.kma.KmaGrid;
import com.example.application.data.direct.external.kma.KmaProperties;

import org.junit.Test;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class KmaTest {

    @Test
    public void gridConversionKnownPoints() {
        // 서울 종로 일대 → (60, 127): 기상청 격자 예시로 널리 쓰이는 값
        assertThat(KmaGrid.toGrid(37.579871, 126.98935)).isEqualTo(new KmaGrid.Cell(60, 127));
        // 기준점(38N, 126E) → (43, 136)
        assertThat(KmaGrid.toGrid(38.0, 126.0)).isEqualTo(new KmaGrid.Cell(43, 136));
    }

    private static Object call(String method, Class<?>[] types, Object... args) throws Exception {
        Class<?> c = Class.forName("com.example.application.data.direct.external.kma.KmaWeatherClient");
        Method m = c.getDeclaredMethod(method, types);
        m.setAccessible(true);
        try {
            return m.invoke(null, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void baseTimeUsesPreviousHourBefore40Minutes() throws Exception {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        ZonedDateTime early = ZonedDateTime.of(2026, 11, 13, 15, 20, 0, 0, kst);
        ZonedDateTime late = ZonedDateTime.of(2026, 11, 13, 15, 45, 0, 0, kst);
        assertThat(((ZonedDateTime) call("baseTime", new Class[]{ZonedDateTime.class}, early)).getHour()).isEqualTo(14);
        assertThat(((ZonedDateTime) call("baseTime", new Class[]{ZonedDateTime.class}, late)).getHour()).isEqualTo(15);
    }

    @Test
    public void parsesNowcastItems() throws Exception {
        String json = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                 "body":{"items":{"item":[
                   {"category":"T1H","obsrValue":"3.2"},
                   {"category":"PTY","obsrValue":"1"},
                   {"category":"RN1","obsrValue":"0.5"}]}}}}""";
        JsonElement node = JsonParser.parseString(json);
        WeatherDto w = (WeatherDto) call("parse", new Class[]{JsonElement.class, ZonedDateTime.class},
                node, ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
        assertThat(w.temperatureC()).isEqualTo(3.2f);
        assertThat(w.raining()).isTrue();
        assertThat(w.precipitationType()).isEqualTo("RAIN");
        assertThat(w.rainfallMm()).isEqualTo(0.5f);
    }

    @Test
    public void errorResultCodeIsUnavailable() throws Exception {
        JsonElement node = JsonParser.parseString(
                "{\"response\":{\"header\":{\"resultCode\":\"30\",\"resultMsg\":\"SERVICE_KEY_IS_NOT_REGISTERED_ERROR\"}}}");
        assertThatThrownBy(() -> call("parse", new Class[]{JsonElement.class, ZonedDateTime.class}, node,
                ZonedDateTime.now())).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    public void picksProviderFromKeyShape() {
        KmaProperties hub = new KmaProperties("abcdEFGHijklMNOPqrstUV", null);
        assertThat(hub.nowcastUrl()).contains("apihub.kma.go.kr");
        assertThat(hub.keyParam()).isEqualTo("authKey");

        KmaProperties portal = new KmaProperties("a".repeat(96), null);
        assertThat(portal.nowcastUrl()).contains("apis.data.go.kr");
        assertThat(portal.keyParam()).isEqualTo("serviceKey");
    }
}
