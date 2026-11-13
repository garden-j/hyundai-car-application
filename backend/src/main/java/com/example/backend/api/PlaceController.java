package com.example.backend.api;

import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;
import com.example.backend.place.PlaceService;

import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
    private final PlaceService service;

    public PlaceController(PlaceService service) {
        this.service = service;
    }

    /** 목적지·출발지 검색. lat/lng를 주면 가까운 순. */
    @GetMapping("/search")
    public List<Place> search(@RequestParam("q") @Size(max = 100) String query,
                              @RequestParam(required = false) Double lat,
                              @RequestParam(required = false) Double lng) {
        GeoPoint near = lat != null && lng != null ? new GeoPoint(lat, lng) : null;
        return service.search(query, near);
    }
}
