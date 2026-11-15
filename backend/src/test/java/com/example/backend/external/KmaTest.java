package com.example.backend.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.api.dto.WeatherDto;
import com.example.backend.common.ExternalServiceUnavailableException;
import com.example.backend.external.kma.KmaGrid;
import com.example.backend.external.kma.KmaProperties;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class KmaTest {

    @Test
    void gridConversionKnownPoints() {
        // 서울 종로 일대 → (60, 127): 기상청 격자 예시로 널리 쓰이는 값
        assertThat(KmaGrid.toGrid(37.579871, 126.98935)).isEqualTo(new KmaGrid.Cell(60, 127));
        // 기준점(38N, 126E) → (43, 136)
        assertThat(KmaGrid.toGrid(38.0, 126.0)).isEqualTo(new KmaGrid.Cell(43, 136));
    }

    private static Object call(String method, Class<?>[] types, Object... args) throws Exception {
        Class<?> c = Class.forName("com.example.backend.external.kma.KmaWeatherClient");
        Method m = c.getDeclaredMethod(method, types);
        m.setAccessible(true);
        try {
            return m.invoke(null, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void baseTimeUsesPreviousHourBefore40Minutes() throws Exception {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        ZonedDateTime early = ZonedDateTime.of(2026, 11, 13, 15, 20, 0, 0, kst);
        ZonedDateTime late = ZonedDateTime.of(2026, 11, 13, 15, 45, 0, 0, kst);
        assertThat(((ZonedDateTime) call("baseTime", new Class[]{ZonedDateTime.class}, early)).getHour()).isEqualTo(14);
        assertThat(((ZonedDateTime) call("baseTime", new Class[]{ZonedDateTime.class}, late)).getHour()).isEqualTo(15);
    }

    @Test
    void parsesNowcastItems() throws Exception {
        String json = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                 "body":{"items":{"item":[
                   {"category":"T1H","obsrValue":"3.2"},
                   {"category":"PTY","obsrValue":"1"},
                   {"category":"RN1","obsrValue":"0.5"}]}}}}""";
        JsonNode node = JsonMapper.builder().build().readTree(json);
        WeatherDto w = (WeatherDto) call("parse", new Class[]{JsonNode.class, ZonedDateTime.class},
                node, ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
        assertThat(w.temperatureC()).isEqualTo(3.2f);
        assertThat(w.raining()).isTrue();
        assertThat(w.precipitationType()).isEqualTo("RAIN");
        assertThat(w.rainfallMm()).isEqualTo(0.5f);
    }

    @Test
    void errorResultCodeIsUnavailable() throws Exception {
        JsonNode node = JsonMapper.builder().build().readTree(
                "{\"response\":{\"header\":{\"resultCode\":\"30\",\"resultMsg\":\"SERVICE_KEY_IS_NOT_REGISTERED_ERROR\"}}}");
        assertThatThrownBy(() -> call("parse", new Class[]{JsonNode.class, ZonedDateTime.class}, node,
                ZonedDateTime.now())).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void picksProviderFromKeyShape() {
        KmaProperties hub = new KmaProperties("abcdEFGHijklMNOPqrstUV", null);
        assertThat(hub.nowcastUrl()).contains("apihub.kma.go.kr");
        assertThat(hub.keyParam()).isEqualTo("authKey");

        KmaProperties portal = new KmaProperties("a".repeat(96), null);
        assertThat(portal.nowcastUrl()).contains("apis.data.go.kr");
        assertThat(portal.keyParam()).isEqualTo("serviceKey");
    }
}
