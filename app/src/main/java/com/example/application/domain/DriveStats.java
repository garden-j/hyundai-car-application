package com.example.application.domain;

import java.util.EnumMap;
import java.util.Map;

/** 이번 주행에서 겪은 회전 종류별 횟수와 주행 시간(격려·목표 달성 판단용). */
public class DriveStats {
    public final Map<DriveGuide.Maneuver, Integer> maneuvers = new EnumMap<>(DriveGuide.Maneuver.class);
    public long startedAtMillis;
    public long endedAtMillis;
    public boolean arrived;
    public int relaxRequests;
    public int rests;

    public void countManeuver(DriveGuide.Maneuver m) {
        Integer n = maneuvers.get(m);
        maneuvers.put(m, n == null ? 1 : n + 1);
    }

    public int count(DriveGuide.Maneuver m) {
        Integer n = maneuvers.get(m);
        return n == null ? 0 : n;
    }

    public int minutes() {
        long end = endedAtMillis > 0 ? endedAtMillis : System.currentTimeMillis();
        return startedAtMillis <= 0 ? 0 : (int) Math.max(0, (end - startedAtMillis) / 60_000);
    }
}
