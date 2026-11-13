package com.example.backend.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.TestRoutes;
import com.example.backend.external.kakao.KakaoDirectionsClient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    KakaoDirectionsClient directions;

    static final String COURSE_REQUEST = """
            {"origin":{"latitude":37.492,"longitude":127.030},
             "goalTypes":["SKILL"],"skills":["U_TURN"],"difficulty":"NORMAL","familiarity":"FAMILIAR",
             "tension":4,"tensionReasons":["UNFAMILIAR_ROAD"],"environments":[]}""";

    @Test
    void routesReturn503WhenKakaoKeyMissing() throws Exception {
        when(directions.available()).thenReturn(false);
        mvc.perform(post("/api/v1/routes/recommend").contentType(MediaType.APPLICATION_JSON).content(COURSE_REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ROUTING_UNAVAILABLE"));
    }

    @Test
    void courseModeReturnsThreeCandidatesWithBriefing() throws Exception {
        when(directions.available()).thenReturn(true);
        when(directions.directions(any())).thenReturn(
                List.of(TestRoutes.withGuides(6000, 900, 2)),
                List.of(TestRoutes.withGuides(4000, 700, 1, 72)),
                List.of(TestRoutes.withGuides(5000, 800, 3, 3)));
        mvc.perform(post("/api/v1/routes/recommend").contentType(MediaType.APPLICATION_JSON).content(COURSE_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(3))
                .andExpect(jsonPath("$.routes[0].kind").value("WIDE_EASY"))
                .andExpect(jsonPath("$.routes[0].waypoints.length()").value(2))
                .andExpect(jsonPath("$.routes[0].destination.name").value("출발지"))
                .andExpect(jsonPath("$.routes[0].origin.latitude").value(37.492))
                .andExpect(jsonPath("$.routes[2].briefing[0].type").exists());
    }

    @Test
    void tooFarDestinationIs400() throws Exception {
        when(directions.available()).thenReturn(true);
        String body = """
                {"origin":{"latitude":37.42,"longitude":-122.08},"goalTypes":["DESTINATION"],
                 "destination":{"destination":{"name":"마트","address":"","point":{"latitude":37.49,"longitude":127.04}},
                 "roundTrip":false}}""";
        mvc.perform(post("/api/v1/routes/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void invalidRequestIs400() throws Exception {
        mvc.perform(post("/api/v1/routes/recommend").contentType(MediaType.APPLICATION_JSON).content("{\"tension\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void placesAndWeather503WithoutKeys() throws Exception {
        mvc.perform(get("/api/v1/places/search").param("q", "마트"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PLACES_UNAVAILABLE"));
        mvc.perform(get("/api/v1/weather/current").param("lat", "37.5").param("lng", "127.0"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("WEATHER_UNAVAILABLE"));
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
