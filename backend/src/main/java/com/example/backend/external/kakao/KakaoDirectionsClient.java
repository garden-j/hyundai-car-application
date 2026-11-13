package com.example.backend.external.kakao;

import com.example.backend.common.ExternalServiceUnavailableException;
import com.example.backend.domain.GeoPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;

/** 카카오모빌리티 자동차 길찾기(GET /v1/directions). */
@Component
public class KakaoDirectionsClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoDirectionsClient.class);

    /** 경로 탐색 옵션. */
    public record Query(GeoPoint origin, GeoPoint destination, List<GeoPoint> waypoints, String priority,
                        String avoid, boolean alternatives) {
    }

    private final KakaoProperties props;
    private final RestClient http;

    public KakaoDirectionsClient(KakaoProperties props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder.build();
    }

    public boolean available() {
        return props.configured();
    }

    public List<KakaoRoute> directions(Query q) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("ROUTING_UNAVAILABLE", "카카오 REST API 키가 설정되지 않았어요");
        }
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(props.directionsUrl())
                .queryParam("origin", q.origin().toKakao())
                .queryParam("destination", q.destination().toKakao())
                .queryParam("priority", q.priority() == null ? "RECOMMEND" : q.priority())
                .queryParam("alternatives", q.alternatives())
                .queryParam("road_details", false)
                .queryParam("summary", false);
        if (q.waypoints() != null && !q.waypoints().isEmpty()) {
            uri.queryParam("waypoints", q.waypoints().stream().map(GeoPoint::toKakao)
                    .collect(Collectors.joining("|")));
        }
        if (q.avoid() != null) {
            uri.queryParam("avoid", q.avoid());
        }
        try {
            JsonNode body = http.get()
                    .uri(uri.encode().build().toUri())
                    .header("Authorization", props.authorization())
                    .retrieve()
                    .body(JsonNode.class);
            List<KakaoRoute> routes = body == null ? List.of() : KakaoDirectionsParser.parse(body);
            if (routes.isEmpty() && body != null) {
                log.info("kakao directions returned no route: {}", KakaoDirectionsParser.failure(body));
            }
            return routes;
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("ROUTING_UNAVAILABLE", "길찾기 서비스에 연결하지 못했어요", e);
        }
    }
}
