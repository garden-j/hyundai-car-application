package com.example.application.data.direct.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.application.data.direct.TestRoutes;
import com.example.application.data.direct.api.dto.RouteCandidateDto;
import com.example.application.data.direct.api.dto.RouteRecommendRequest;
import com.example.application.data.direct.domain.Enums.Difficulty;
import com.example.application.data.direct.domain.Enums.DrivingSkill;
import com.example.application.data.direct.domain.Enums.RouteKind;
import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.domain.Place;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public class RouteSelectorTest {
    static final GeoPoint START = new GeoPoint(37.492, 127.030);
    static final Place DEST = new Place("마트", "", new GeoPoint(37.4993, 127.0461));

    static RouteRecommendRequest req(Difficulty d, Integer tension, DrivingSkill... skills) {
        return new RouteRecommendRequest(START, null, com.example.application.data.direct.common.Lists.setOf(skills), null, null, null, d, null, tension, null);
    }

    static AnalyzedRoute route(int distanceM, int durationS, int... types) {
        return AnalyzedRoute.of(TestRoutes.withGuides(distanceM, durationS, types), com.example.application.data.direct.common.Lists.of(), DEST);
    }

    // 쉬움(난이도 최저), 짧음, 유턴 많음
    final List<AnalyzedRoute> pool = com.example.application.data.direct.common.Lists.of(
            route(6000, 900, 2),
            route(4000, 700, 1, 1, 72),
            route(5000, 800, 3, 3, 1));

    @Test
    public void picksEasyShortestAndGoalInWireframeOrder() {
        List<RouteCandidateDto> out = RouteSelector.select(new java.util.ArrayList<>(pool),
                req(Difficulty.NORMAL, 2, DrivingSkill.U_TURN), START);
        assertThat(out).extracting(RouteCandidateDto::kind)
                .containsExactly(RouteKind.WIDE_EASY, RouteKind.SHORTEST, RouteKind.GOAL_FOCUSED);
        assertThat(out.get(0).distanceKm()).isEqualTo(6.0);
        assertThat(out.get(1).distanceKm()).isEqualTo(4.0);
        assertThat(out.get(2).uTurns()).isEqualTo(2);
        assertThat(out.get(2).reason()).isEqualTo("유턴 지점이 2곳 있어요");
        assertThat(out).extracting(RouteCandidateDto::id).containsExactly("r1", "r2", "r3");
        assertThat(out.get(0).briefing()).isNotNull();
        assertThat(out.get(0).source()).isEqualTo("KAKAO");
    }

    @Test
    public void challengePutsGoalFirstUnlessTense() {
        assertThat(RouteSelector.select(new java.util.ArrayList<>(pool),
                req(Difficulty.CHALLENGE, 1, DrivingSkill.U_TURN), START).get(0).kind())
                .isEqualTo(RouteKind.GOAL_FOCUSED);
        assertThat(RouteSelector.select(new java.util.ArrayList<>(pool),
                req(Difficulty.CHALLENGE, 5, DrivingSkill.U_TURN), START).get(0).kind())
                .isEqualTo(RouteKind.WIDE_EASY);
    }

    @Test
    public void duplicatesCollapseAndSingleRouteGetsNeutralTitle() {
        List<AnalyzedRoute> same = new java.util.ArrayList<>(com.example.application.data.direct.common.Lists.of(route(5000, 800, 1), route(5020, 820, 1)));
        List<RouteCandidateDto> out = RouteSelector.select(same, req(Difficulty.NORMAL, 2), START);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).title()).isEqualTo("추천 경로예요");
    }

    @Test
    public void noGoalMatchMeansQuietRoad() {
        List<RouteCandidateDto> out = RouteSelector.select(new java.util.ArrayList<>(com.example.application.data.direct.common.Lists.of(
                route(6000, 900, 2), route(4000, 700, 2, 2), route(5000, 800, 2, 2, 2))),
                req(Difficulty.NORMAL, 2, DrivingSkill.MERGE), START);
        assertThat(out.get(2).title()).isEqualTo("한산한 길 위주");
    }

    @Test
    public void previewIsDecimated() {
        assertThat(RouteSelector.preview(route(1000, 100))).hasSize(2);
    }
}
