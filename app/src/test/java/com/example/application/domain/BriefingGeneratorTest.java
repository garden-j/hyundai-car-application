package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.application.model.BriefingPoint;
import com.example.application.model.GeoPoint;
import com.example.application.model.RouteCandidate;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BriefingGeneratorTest {

    private static RouteCandidate route() {
        RouteCandidate r = new RouteCandidate();
        r.distanceKm = 6.0;
        r.complexIntersections = 2;
        r.merges = 1;
        r.laneChanges = 4;
        r.leftTurns = 3;
        r.uTurns = 1;
        r.parkingSpots = 1;
        r.previewPath = Arrays.asList(new GeoPoint(37.49, 127.03), new GeoPoint(37.50, 127.04));
        return r;
    }

    @Test
    public void pointsAreOrderedByDistanceAndCapped() {
        List<BriefingPoint> points = BriefingGenerator.generate(route());
        assertTrue(points.size() <= BriefingGenerator.MAX_POINTS + 1); // + 주차
        for (int i = 1; i < points.size(); i++) {
            assertTrue(points.get(i - 1).distanceKm <= points.get(i).distanceKm);
            assertEquals(i + 1, points.get(i).order);
        }
        assertEquals(BriefingPoint.Type.PARKING, points.get(points.size() - 1).type);
    }

    @Test
    public void heavierSegmentsArePrioritised() {
        List<BriefingPoint> points = BriefingGenerator.generate(route());
        long complex = points.stream().filter(p -> p.type == BriefingPoint.Type.COMPLEX_INTERSECTION).count();
        assertEquals(2, complex);
    }

    @Test
    public void speechUsesCorrectJosa() {
        for (BriefingPoint p : BriefingGenerator.generate(route())) {
            if (p.type == BriefingPoint.Type.COMPLEX_INTERSECTION) {
                assertTrue(p.speech, p.speech.contains("교차로예요"));
            }
            if (p.type == BriefingPoint.Type.MERGE) {
                assertTrue(p.speech, p.speech.contains("구간이에요"));
            }
        }
    }

    @Test
    public void emptyRouteSaysNothingComplex() {
        RouteCandidate r = new RouteCandidate();
        r.distanceKm = 2;
        List<String> script = BriefingGenerator.speechScript(BriefingGenerator.generate(r));
        assertEquals(1, script.size());
        assertTrue(script.get(0).contains("없어요"));
    }

    @Test
    public void spokenDistance() {
        assertEquals("800미터", BriefingGenerator.spokenDistance(0.8));
        assertEquals("1.2킬로미터", BriefingGenerator.spokenDistance(1.2));
    }
}
