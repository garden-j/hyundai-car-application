package com.example.backend.external.kakao;

import com.example.backend.common.ExternalServiceUnavailableException;
import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/** 카카오 로컬 키워드 장소 검색(GET /v2/local/search/keyword.json). x = 경도, y = 위도. */
@Component
public class KakaoLocalClient {
    private final KakaoProperties props;
    private final RestClient http;

    public KakaoLocalClient(KakaoProperties props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder.build();
    }

    public boolean available() {
        return props.configured();
    }

    /** 정확도순으로 검색한다. near가 있으면 현재 위치 근처 결과가 위로 오도록 가중치로 쓴다. */
    public List<Place> search(String query, GeoPoint near, int size) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("PLACES_UNAVAILABLE", "카카오 REST API 키가 설정되지 않았어요");
        }
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(props.localKeywordUrl())
                .queryParam("query", query)
                .queryParam("size", Math.max(1, Math.min(15, size)));
        if (near != null) {
            uri.queryParam("x", near.longitude()).queryParam("y", near.latitude()).queryParam("sort", "accuracy"); // 정확도순(현재 위치는 가중치로만 사용)
        }
        try {
            JsonNode body = http.get().uri(uri.encode().build().toUri())
                    .header("Authorization", props.authorization())
                    .retrieve().body(JsonNode.class);
            return body == null ? List.of() : parse(body);
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("PLACES_UNAVAILABLE", "장소 검색 서비스에 연결하지 못했어요", e);
        }
    }

    static List<Place> parse(JsonNode body) {
        List<Place> out = new ArrayList<>();
        for (JsonNode d : body.path("documents")) {
            String road = d.path("road_address_name").asString("");
            String address = road.isBlank() ? d.path("address_name").asString("") : road;
            out.add(new Place(d.path("place_name").asString(""), address,
                    new GeoPoint(Double.parseDouble(d.path("y").asString("0")),
                            Double.parseDouble(d.path("x").asString("0")))));
        }
        return out;
    }
}
