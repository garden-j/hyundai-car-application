package com.example.application.data.direct.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.application.data.direct.TestRoutes;
import com.example.application.data.direct.external.kakao.KakaoDirectionsParser;
import com.example.application.data.direct.external.kakao.KakaoRoute;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import com.google.gson.JsonParser;

public class KakaoDirectionsParserTest {

    @Test
    public void parsesSuccessfulRoutesOnly() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/kakao-directions-sample.json")) {
            List<KakaoRoute> routes = KakaoDirectionsParser.parse(JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
            assertThat(routes).hasSize(1); // result_code 104 경로는 제외
        }
    }

    @Test
    public void readsSummaryGuidesAndRoads() throws Exception {
        KakaoRoute r = TestRoutes.sample();
        assertThat(r.distanceM()).isEqualTo(4300);
        assertThat(r.durationS()).isEqualTo(780);
        assertThat(r.guides()).hasSize(6);
        assertThat(r.guides().get(1).type()).isEqualTo(1);
        assertThat(r.guides().get(1).name()).isEqualTo("강남역사거리");
        // vertexes는 [x(경도), y(위도), ...] → GeoPoint(위도, 경도)
        assertThat(r.roads().get(0).path().get(0).latitude()).isEqualTo(37.4920);
        assertThat(r.roads().get(0).path().get(0).longitude()).isEqualTo(127.0300);
        assertThat(r.roads().get(1).trafficState()).isEqualTo(1);
    }
}
