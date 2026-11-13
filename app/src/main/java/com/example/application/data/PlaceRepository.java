package com.example.application.data;

import com.example.application.model.GeoPoint;
import com.example.application.model.Place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자주 가는 곳·장소 검색. 장소 검색 API(백엔드 프록시) 연동 전까지는
 * PT 시연 장소(강남대로) 주변 예시 장소를 사용한다.
 */
public class PlaceRepository {
    private final Map<String, Place> frequent = new LinkedHashMap<>();
    private final List<Place> catalog;

    public PlaceRepository() {
        frequent.put("집", new Place("집", "서울 강남구 역삼동 (예시)", new GeoPoint(37.5006, 127.0364)));
        frequent.put("직장", new Place("직장", "서울 강남구 선릉역 인근 (예시)", new GeoPoint(37.5045, 127.0490)));
        frequent.put("학교", new Place("학교", "서울 서초구 서울교대 (예시)", new GeoPoint(37.4900, 127.0144)));
        frequent.put("마트", new Place("우리 동네 마트", "서울 강남구 역삼동 (예시)", new GeoPoint(37.4993, 127.0461)));
        catalog = new ArrayList<>(frequent.values());
        catalog.addAll(Arrays.asList(
                new Place("코엑스", "서울 강남구 영동대로 513", new GeoPoint(37.5115, 127.0595)),
                new Place("양재 하나로마트", "서울 서초구 청계산로 10", new GeoPoint(37.4630, 127.0430)),
                new Place("서초구청", "서울 서초구 남부순환로 2584", new GeoPoint(37.4837, 127.0324)),
                new Place("반포한강공원 주차장", "서울 서초구 신반포로11길 40", new GeoPoint(37.5110, 126.9960)),
                new Place("현대자동차 강남대로사옥", "서울 강남구 강남대로 350", new GeoPoint(37.4920, 127.0300))));
    }

    /** 칩으로 보여줄 자주 가는 곳(이름 → 장소). */
    public Map<String, Place> frequentPlaces() {
        return frequent;
    }

    public List<Place> search(String query) {
        List<Place> out = new ArrayList<>();
        String q = query == null ? "" : query.replace(" ", "");
        for (Place p : catalog) {
            if (q.isEmpty() || p.name.replace(" ", "").contains(q) || p.address.replace(" ", "").contains(q)) {
                out.add(p);
            }
        }
        return out;
    }
}
