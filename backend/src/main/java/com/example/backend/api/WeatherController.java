package com.example.backend.api;

import com.example.backend.api.dto.WeatherDto;
import com.example.backend.weather.WeatherService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {
    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    /** 현재 위치 날씨(프리셋 추천: 우천·추위·더위 → 악천후 모드). */
    @GetMapping("/current")
    public WeatherDto current(@RequestParam double lat, @RequestParam double lng) {
        return service.current(lat, lng);
    }
}
