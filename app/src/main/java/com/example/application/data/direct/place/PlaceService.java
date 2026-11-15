package com.example.application.data.direct.place;

import com.example.application.data.direct.common.Lists;

import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.domain.Place;
import com.example.application.data.direct.external.kakao.KakaoLocalClient;


import java.util.List;
public class PlaceService {
    private final KakaoLocalClient local;

    public PlaceService(KakaoLocalClient local) {
        this.local = local;
    }

    public List<Place> search(String query, GeoPoint near) {
        if (query == null || query.trim().isEmpty()) {
            return Lists.of();
        }
        return local.search(query.trim(), near, 15);
    }
}
