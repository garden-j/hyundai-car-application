package com.example.backend.external.kakao;

import com.example.backend.domain.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/** 길찾기 응답 JSON → {@link KakaoRoute}. result_code가 0(성공)인 경로만 돌려준다. */
public final class KakaoDirectionsParser {
    private KakaoDirectionsParser() {
    }

    public static List<KakaoRoute> parse(JsonNode root) {
        List<KakaoRoute> out = new ArrayList<>();
        for (JsonNode route : root.path("routes")) {
            if (route.path("result_code").asInt(-1) != 0) {
                continue;
            }
            JsonNode summary = route.path("summary");
            List<KakaoRoute.Guide> guides = new ArrayList<>();
            List<KakaoRoute.Road> roads = new ArrayList<>();
            for (JsonNode section : route.path("sections")) {
                for (JsonNode g : section.path("guides")) {
                    guides.add(new KakaoRoute.Guide(g.path("name").asString(""), g.path("x").asDouble(),
                            g.path("y").asDouble(), g.path("distance").asInt(), g.path("duration").asInt(),
                            g.path("type").asInt(), g.path("guidance").asString("")));
                }
                for (JsonNode r : section.path("roads")) {
                    List<GeoPoint> path = new ArrayList<>();
                    JsonNode v = r.path("vertexes");
                    for (int i = 0; i + 1 < v.size(); i += 2) {
                        path.add(new GeoPoint(v.get(i + 1).asDouble(), v.get(i).asDouble()));
                    }
                    roads.add(new KakaoRoute.Road(r.path("name").asString(""), r.path("distance").asInt(),
                            r.path("traffic_state").asInt(), path));
                }
            }
            out.add(new KakaoRoute(summary.path("distance").asInt(), summary.path("duration").asInt(),
                    guides, roads, summary.path("priority").asString("")));
        }
        return out;
    }

    /** 첫 경로의 실패 코드·메시지(모든 경로가 실패했을 때 로그용). */
    public static String failure(JsonNode root) {
        JsonNode first = root.path("routes").path(0);
        return first.path("result_code").asInt(-1) + " " + first.path("result_msg").asString("");
    }
}
