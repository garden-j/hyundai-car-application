package com.example.backend.external.kma;

import com.example.backend.api.dto.WeatherDto;
import com.example.backend.common.ExternalServiceUnavailableException;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.databind.JsonNode;

/** 기상청 초단기실황(getUltraSrtNcst). 매시 정시 관측값이 40분 무렵 제공되므로 그 전에는 이전 시각을 조회한다. */
@Component
public class KmaWeatherClient {
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PUBLISH_MINUTE = 40;

    private final KmaProperties props;
    private final RestClient http;

    public KmaWeatherClient(KmaProperties props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder.build();
    }

    public boolean available() {
        return props.configured();
    }

    public WeatherDto nowcast(KmaGrid.Cell cell, ZonedDateTime now) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE", "기상청 인증키가 설정되지 않았어요");
        }
        ZonedDateTime base = baseTime(now);
        var uri = UriComponentsBuilder.fromUriString(props.nowcastUrl())
                .queryParam("serviceKey", props.serviceKey())
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", base.format(DateTimeFormatter.BASIC_ISO_DATE))
                .queryParam("base_time", base.format(DateTimeFormatter.ofPattern("HH00")))
                .queryParam("nx", cell.nx())
                .queryParam("ny", cell.ny())
                .encode().build().toUri();
        try {
            JsonNode body = http.get().uri(uri).retrieve().body(JsonNode.class);
            return parse(body, base);
        } catch (RestClientException e) {
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

    static WeatherDto parse(JsonNode body, ZonedDateTime base) {
        if (body == null) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE", "날씨 응답이 비어 있어요");
        }
        String code = body.path("response").path("header").path("resultCode").asString("");
        if (!"00".equals(code)) {
            throw new ExternalServiceUnavailableException("WEATHER_UNAVAILABLE",
                    "날씨 조회 실패: " + code + " " + body.path("response").path("header").path("resultMsg").asString(""));
        }
        Float temp = null;
        Float rain = null;
        int pty = 0;
        for (JsonNode item : body.path("response").path("body").path("items").path("item")) {
            String value = item.path("obsrValue").asString("");
            switch (item.path("category").asString("")) {
                case "T1H" -> temp = parseFloat(value);
                case "RN1" -> rain = parseFloat(value);
                case "PTY" -> pty = (int) Math.round(parseFloat(value) == null ? 0 : parseFloat(value));
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
