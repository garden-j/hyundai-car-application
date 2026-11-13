package com.example.backend.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.TestRoutes;
import com.example.backend.api.dto.BriefingPointDto;
import com.example.backend.domain.Enums.BriefingType;
import com.example.backend.external.kakao.KakaoRoute;

import org.junit.jupiter.api.Test;

import java.util.List;

class RouteAnalysisTest {

    @Test
    void featuresFromGuideTypes() throws Exception {
        RouteFeatures f = RouteFeatures.of(TestRoutes.sample());
        assertThat(f.leftTurns()).isEqualTo(1);
        assertThat(f.uTurns()).isEqualTo(1);
        assertThat(f.complexIntersections()).isEqualTo(1); // 72 회전 교차로
        assertThat(f.laneChanges()).isEqualTo(1);           // 6 오른쪽 방향
        assertThat(f.merges()).isZero();
        assertThat(f.congestionRatio()).isCloseTo(1800 / 4300.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(f.wideRoadRatio()).isCloseTo(2500 / 4300.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void mergeTypesAreCounted() {
        RouteFeatures f = RouteFeatures.of(TestRoutes.withGuides(5000, 600, 10, 49, 86, 2));
        assertThat(f.merges()).isEqualTo(3);
        assertThat(f.rightTurns()).isEqualTo(1);
    }

    @Test
    void scoreUsesSameWeightsAsApp() throws Exception {
        RouteScorer.Score s = RouteScorer.score(RouteFeatures.of(TestRoutes.sample()));
        // 교차로10 + 유턴6 + 차로4 + 좌회전3 + 정체 round(0.4186*15)=6
        assertThat(s.value()).isEqualTo(29);
        assertThat(s.factors()).anyMatch(x -> x.startsWith("막히는 구간"));
    }

    @Test
    void briefingIsOrderedByDistanceWithCumulativeKm() throws Exception {
        List<BriefingPointDto> b = BriefingBuilder.build(TestRoutes.sample());
        assertThat(b).extracting(BriefingPointDto::type).containsExactly(
                BriefingType.LEFT_TURN, BriefingType.U_TURN, BriefingType.COMPLEX_INTERSECTION,
                BriefingType.LANE_CHANGE_PREP);
        assertThat(b.get(0).distanceKm()).isEqualTo(0.8);
        assertThat(b.get(2).distanceKm()).isEqualTo(2.9); // 800+900+1200
        assertThat(b.get(0).instruction()).isEqualTo("강남역사거리에서 좌회전");
        assertThat(b.get(0).speech()).startsWith("800미터 앞, 강남역사거리에서 좌회전.");
        assertThat(b).extracting(BriefingPointDto::order).containsExactly(1, 2, 3, 4);
    }

    @Test
    void briefingKeepsHeaviestFive() {
        KakaoRoute r = TestRoutes.withGuides(10000, 1200, 1, 1, 1, 1, 72, 10, 3);
        List<BriefingPointDto> b = BriefingBuilder.build(r);
        assertThat(b).hasSize(BriefingBuilder.MAX_POINTS);
        assertThat(b).extracting(BriefingPointDto::type)
                .contains(BriefingType.COMPLEX_INTERSECTION, BriefingType.MERGE, BriefingType.U_TURN);
        assertThat(b.stream().filter(p -> p.type() == BriefingType.LEFT_TURN).count()).isEqualTo(2);
    }
}
