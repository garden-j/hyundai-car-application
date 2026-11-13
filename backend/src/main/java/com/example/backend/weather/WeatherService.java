package com.example.backend.weather;

import com.example.backend.api.dto.WeatherDto;
import com.example.backend.external.kma.KmaGrid;
import com.example.backend.external.kma.KmaWeatherClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 현재 날씨. 같은 격자는 10분 동안 캐시해 외부 API 호출 수를 줄인다. */
@Service
public class WeatherService {
    static final Duration TTL = Duration.ofMinutes(10);

    private record Cached(WeatherDto value, Instant at) {
    }

    private final KmaWeatherClient kma;
    private final Clock clock;
    private final Map<KmaGrid.Cell, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public WeatherService(KmaWeatherClient kma) {
        this(kma, Clock.systemUTC());
    }

    WeatherService(KmaWeatherClient kma, Clock clock) {
        this.kma = kma;
        this.clock = clock;
    }

    public WeatherDto current(double lat, double lon) {
        KmaGrid.Cell cell = KmaGrid.toGrid(lat, lon);
        Instant now = clock.instant();
        Cached c = cache.get(cell);
        if (c != null && Duration.between(c.at(), now).compareTo(TTL) < 0) {
            return c.value();
        }
        WeatherDto fresh = kma.nowcast(cell, ZonedDateTime.ofInstant(now, clock.getZone()));
        cache.put(cell, new Cached(fresh, now));
        return fresh;
    }
}
