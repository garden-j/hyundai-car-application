package com.example.backend.api;

import com.example.backend.api.dto.RouteRecommendRequest;
import com.example.backend.api.dto.RouteRecommendResponse;
import com.example.backend.routing.RouteRecommendationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {
    private final RouteRecommendationService service;

    public RouteController(RouteRecommendationService service) {
        this.service = service;
    }

    /** 연습 경로 후보(최대 3개)와 각 경로의 복잡 구간 브리핑. */
    @PostMapping("/recommend")
    public RouteRecommendResponse recommend(@Valid @RequestBody RouteRecommendRequest request) {
        return new RouteRecommendResponse(service.recommend(request));
    }
}
