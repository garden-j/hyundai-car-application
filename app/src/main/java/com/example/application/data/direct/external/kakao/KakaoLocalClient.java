package com.example.application.data.direct.external.kakao;

import com.example.application.data.direct.common.ExternalServiceUnavailableException;
import com.example.application.data.direct.common.Http;
import com.example.application.data.direct.common.Json;
import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.domain.Place;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 카카오 로컬 키워드 장소 검색(GET /v2/local/search/keyword.json). x = 경도, y = 위도. */
public class KakaoLocalClient {
    private final KakaoProperties props;

    public KakaoLocalClient(KakaoProperties props) {
        this.props = props;
    }

    public boolean available() {
        return props.configured();
    }

    /** 정확도순으로 검색한다. near가 있으면 현재 위치 근처 결과가 위로 오도록 가중치로 쓴다. */
    public List<Place> search(String query, GeoPoint near, int size) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("PLACES_UNAVAILABLE", "카카오 REST API 키가 설정되지 않았어요");
        }
        Http.Query uri = new Http.Query(props.localKeywordUrl())
                .param("query", query)
                .param("size", Math.max(1, Math.min(15, size)));
        if (near != null) {
            uri.param("x", near.longitude()).param("y", near.latitude()).param("sort", "accuracy");
        }
        try {
            return parse(Http.getJson(uri.build(), props.authorization()));
        } catch (IOException e) {
            throw new ExternalServiceUnavailableException("PLACES_UNAVAILABLE", "장소 검색 서비스에 연결하지 못했어요", e);
        }
    }

    static List<Place> parse(JsonElement body) {
        List<Place> out = new ArrayList<>();
        for (JsonElement d : Json.arr(body, "documents")) {
            String road = Json.str(d, "road_address_name", "");
            String address = road.trim().isEmpty() ? Json.str(d, "address_name", "") : road;
            try {
                out.add(new Place(Json.str(d, "place_name", ""), address,
                        new GeoPoint(Double.parseDouble(Json.str(d, "y", "0")),
                                Double.parseDouble(Json.str(d, "x", "0")))));
            } catch (NumberFormatException ignored) {
                // 좌표가 없는 결과는 건너뛴다
            }
        }
        return out;
    }
}
