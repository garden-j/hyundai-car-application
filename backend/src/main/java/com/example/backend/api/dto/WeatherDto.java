package com.example.backend.api.dto;

/**
 * 현재 날씨(기상청 초단기실황). 프리셋 추천(악천후 모드)과 환경 변화 판단에 쓴다.
 *
 * @param precipitationType 강수 형태: NONE, RAIN, RAIN_SNOW, SNOW, DRIZZLE, DRIZZLE_SNOW, SNOW_FLURRY
 */
public record WeatherDto(Float temperatureC, boolean raining, boolean snowing, String precipitationType,
                         Float rainfallMm, String observedAt, String source) {
}
