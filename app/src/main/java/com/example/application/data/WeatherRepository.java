package com.example.application.data;

import com.example.application.core.Callback;
import com.example.application.data.remote.ApiClient;
import com.example.application.model.GeoPoint;

import java.util.Locale;

/** 현재 날씨(서버 → 기상청). 서버가 없으면 onError → 날씨 없이 추천한다. */
public class WeatherRepository {
    /** 서버 WeatherDto와 같은 필드. */
    public static class Weather {
        public Float temperatureC;
        public boolean raining;
        public boolean snowing;
        public String precipitationType;
    }

    private final ApiClient api;

    public WeatherRepository(ApiClient api) {
        this.api = api;
    }

    public void current(GeoPoint at, Callback<Weather> callback) {
        if (api == null) {
            callback.onError(new IllegalStateException("weather server not configured"));
            return;
        }
        api.get(String.format(Locale.US, "/api/v1/weather/current?lat=%.6f&lng=%.6f", at.latitude, at.longitude),
                Weather.class, callback);
    }
}
