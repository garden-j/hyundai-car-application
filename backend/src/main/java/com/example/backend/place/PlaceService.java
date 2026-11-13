package com.example.backend.place;

import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;
import com.example.backend.external.kakao.KakaoLocalClient;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceService {
    private final KakaoLocalClient local;

    public PlaceService(KakaoLocalClient local) {
        this.local = local;
    }

    public List<Place> search(String query, GeoPoint near) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return local.search(query.trim(), near, 15);
    }
}
