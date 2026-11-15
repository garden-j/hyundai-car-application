package com.example.application.data.direct.external.kakao;

import com.example.application.data.direct.common.ExternalServiceUnavailableException;
import com.example.application.data.direct.common.Http;
import com.example.application.data.direct.domain.GeoPoint;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 카카오모빌리티 자동차 길찾기(GET /v1/directions). 블로킹 호출이므로 백그라운드 스레드에서 쓴다. */
public class KakaoDirectionsClient {
    private static final String TAG = "KakaoDirections";

    /** 경로 탐색 옵션. */
    public record Query(GeoPoint origin, GeoPoint destination, List<GeoPoint> waypoints, String priority,
                        String avoid, boolean alternatives) {
    }

    private final KakaoProperties props;

    public KakaoDirectionsClient(KakaoProperties props) {
        this.props = props;
    }

    public boolean available() {
        return props.configured();
    }

    public List<KakaoRoute> directions(Query q) {
        if (!props.configured()) {
            throw new ExternalServiceUnavailableException("ROUTING_UNAVAILABLE", "카카오 REST API 키가 설정되지 않았어요");
        }
        Http.Query uri = new Http.Query(props.directionsUrl())
                .param("origin", q.origin().toKakao())
                .param("destination", q.destination().toKakao())
                .param("priority", q.priority() == null ? "RECOMMEND" : q.priority())
                .param("alternatives", q.alternatives())
                .param("road_details", false)
                .param("summary", false);
        if (q.waypoints() != null && !q.waypoints().isEmpty()) {
            List<String> pts = new ArrayList<>();
            for (GeoPoint p : q.waypoints()) {
                pts.add(p.toKakao());
            }
            uri.param("waypoints", String.join("|", pts));
        }
        if (q.avoid() != null) {
            uri.param("avoid", q.avoid());
        }
        try {
            JsonElement body = Http.getJson(uri.build(), props.authorization());
            List<KakaoRoute> routes = KakaoDirectionsParser.parse(body);
            if (routes.isEmpty()) {
                android.util.Log.i(TAG, "kakao directions returned no route: " + KakaoDirectionsParser.failure(body));
            }
            return routes;
        } catch (IOException e) {
            throw new ExternalServiceUnavailableException("ROUTING_UNAVAILABLE", "길찾기 서비스에 연결하지 못했어요", e);
        }
    }
}
