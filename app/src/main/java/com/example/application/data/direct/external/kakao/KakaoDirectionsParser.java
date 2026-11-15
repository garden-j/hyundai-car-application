package com.example.application.data.direct.external.kakao;

import com.example.application.data.direct.common.Json;
import com.example.application.data.direct.domain.GeoPoint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** 길찾기 응답 JSON → {@link KakaoRoute}. result_code가 0(성공)인 경로만 돌려준다. */
public final class KakaoDirectionsParser {
    private KakaoDirectionsParser() {
    }

    public static List<KakaoRoute> parse(JsonElement root) {
        List<KakaoRoute> out = new ArrayList<>();
        for (JsonElement route : Json.arr(root, "routes")) {
            if (Json.integer(route, "result_code", -1) != 0) {
                continue;
            }
            JsonObject summary = Json.obj(route, "summary");
            List<KakaoRoute.Guide> guides = new ArrayList<>();
            List<KakaoRoute.Road> roads = new ArrayList<>();
            for (JsonElement section : Json.arr(route, "sections")) {
                for (JsonElement g : Json.arr(section, "guides")) {
                    guides.add(new KakaoRoute.Guide(Json.str(g, "name", ""), Json.dbl(g, "x", 0),
                            Json.dbl(g, "y", 0), Json.integer(g, "distance", 0), Json.integer(g, "duration", 0),
                            Json.integer(g, "type", 0), Json.str(g, "guidance", "")));
                }
                for (JsonElement r : Json.arr(section, "roads")) {
                    List<GeoPoint> path = new ArrayList<>();
                    JsonArray v = Json.arr(r, "vertexes");
                    for (int i = 0; i + 1 < v.size(); i += 2) {
                        path.add(new GeoPoint(v.get(i + 1).getAsDouble(), v.get(i).getAsDouble()));
                    }
                    roads.add(new KakaoRoute.Road(Json.str(r, "name", ""), Json.integer(r, "distance", 0),
                            Json.integer(r, "traffic_state", 0), path));
                }
            }
            out.add(new KakaoRoute(Json.integer(summary, "distance", 0), Json.integer(summary, "duration", 0),
                    guides, roads, Json.str(summary, "priority", "")));
        }
        return out;
    }

    /** 첫 경로의 실패 코드·메시지(모든 경로가 실패했을 때 로그용). */
    public static String failure(JsonElement root) {
        JsonArray routes = Json.arr(root, "routes");
        if (routes.size() == 0) {
            return "no routes";
        }
        JsonElement first = routes.get(0);
        return Json.integer(first, "result_code", -1) + " " + Json.str(first, "result_msg", "");
    }
}
