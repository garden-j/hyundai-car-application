package com.example.application.data.direct.external.kma;

import com.example.application.data.direct.api.dto.WeatherDto;
import com.example.application.data.direct.common.ExternalServiceUnavailableException;
import com.example.application.data.direct.common.Http;
import com.example.application.data.direct.common.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** 기상청 초단기실황(getUltraSrtNcst). 매시 정시 관측값이 40분 무렵 제공되므로 그 전에는 이전 시각을 조회한다. */
public class KmaWeatherClient {
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PUBLISH_MINUTE = 40;

    private final KmaProperties props;

    public KmaWeatherClient(KmaProperties props) {
        this.props = props;
    }

    public boolean available() {
        return props.configured();
    }

    public WeatherDto nowcast(KmaGrid.Cell cell, ZonedDateTime now) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE", "기상청 인증키가 설정되지 않았어요");
        }
        ZonedDateTime base = baseTime(now);
        String url = new Http.Query(props.nowcastUrl())
                .param(props.keyParam(), props.serviceKey().trim())
                .param("pageNo", 1)
                .param("numOfRows", 100)
                .param("dataType", "JSON")
                .param("base_date", base.format(DateTimeFormatter.BASIC_ISO_DATE))
                .param("base_time", base.format(DateTimeFormatter.ofPattern("HH00")))
                .param("nx", cell.nx())
                .param("ny", cell.ny())
                .build();
        try {
            return parse(Http.getJson(url, null), base);
        } catch (IOException e) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE", "날씨 서비스에 연결하지 못했어요", e);
        }
    }

    static ZonedDateTime baseTime(ZonedDateTime now) {
        ZonedDateTime kst = now.withZoneSameInstant(KST);
        if (kst.getMinute() < PUBLISH_MINUTE) {
            kst = kst.minusHours(1);
        }
        return kst.withMinute(0).withSecond(0).withNano(0);
    }

    static WeatherDto parse(JsonElement body, ZonedDateTime base) {
        JsonObject header = Json.obj(Json.obj(body, "response"), "header");
        String code = Json.str(header, "resultCode", "");
        if (!"00".equals(code)) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE",
                    "날씨 조회 실패: " + code + " " + Json.str(header, "resultMsg", ""));
        }
        Float temp = null;
        Float rain = null;
        int pty = 0;
        JsonObject items = Json.obj(Json.obj(Json.obj(body, "response"), "body"), "items");
        for (JsonElement item : Json.arr(items, "item")) {
            String value = Json.str(item, "obsrValue", "");
            switch (Json.str(item, "category", "")) {
                case "T1H" -> temp = parseFloat(value);
                case "RN1" -> rain = parseFloat(value);
                case "PTY" -> {
                    Float v = parseFloat(value);
                    pty = v == null ? 0 : Math.round(v);
                }
                default -> { }
            }
        }
        String type = switch (pty) {
            case 1 -> "RAIN";
            case 2 -> "RAIN_SNOW";
            case 3 -> "SNOW";
            case 5 -> "DRIZZLE";
            case 6 -> "DRIZZLE_SNOW";
            case 7 -> "SNOW_FLURRY";
            default -> "NONE";
        };
        boolean raining = pty == 1 || pty == 2 || pty == 5 || pty == 6;
        boolean snowing = pty == 2 || pty == 3 || pty == 6 || pty == 7;
        return new WeatherDto(temp, raining, snowing, type, rain,
                base.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "KMA");
    }

    private static Float parseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return null; // "강수없음" 등 문자열 값
        }
    }
}
