package com.example.application.domain;

import com.example.application.model.DrivingSkill;
import com.example.application.model.GuidanceLevel;
import com.example.application.model.PracticeSetup;

import java.util.ArrayList;
import java.util.List;

/**
 * 주행 후 격려(Figma L/22): 이번 주행 결과와 연습 목표 달성 여부로 제목·해낸 목표·음성 멘트·다음 추천을 만든다.
 * 목표를 다 이루지 못해도 노력을 인정하는 말로 마무리한다. 격려 안내 수준에 따라 멘트 길이를 조절한다.
 */
public final class Encouragement {
    public final String title;
    public final String subtitle;
    /** 해낸 목표(칩으로 표시). */
    public final List<String> achieved;
    /** 다음에 더 해 볼 것. */
    public final List<String> missed;
    public final String speech;
    public final String next;

    private Encouragement(String title, String subtitle, List<String> achieved, List<String> missed,
                          String speech, String next) {
        this.title = title;
        this.subtitle = subtitle;
        this.achieved = achieved;
        this.missed = missed;
        this.speech = speech;
        this.next = next;
    }

    static DriveGuide.Maneuver maneuverOf(DrivingSkill s) {
        switch (s) {
            case LEFT_TURN:
                return DriveGuide.Maneuver.LEFT;
            case RIGHT_TURN:
                return DriveGuide.Maneuver.RIGHT;
            case U_TURN:
                return DriveGuide.Maneuver.U_TURN;
            case LANE_CHANGE:
                return DriveGuide.Maneuver.KEEP_LEFT;
            case MERGE:
                return DriveGuide.Maneuver.MERGE;
            default:
                return null;
        }
    }

    static boolean done(DrivingSkill s, DriveStats st) {
        switch (s) {
            case LANE_CHANGE:
                return st.count(DriveGuide.Maneuver.KEEP_LEFT) + st.count(DriveGuide.Maneuver.KEEP_RIGHT)
                        + st.count(DriveGuide.Maneuver.EXIT) > 0;
            case INTERSECTION:
                return st.count(DriveGuide.Maneuver.LEFT) + st.count(DriveGuide.Maneuver.RIGHT)
                        + st.count(DriveGuide.Maneuver.STRAIGHT) > 0;
            case PARKING:
                return st.arrived; // 도착 후 주차까지 했다고 본다
            default:
                DriveGuide.Maneuver m = maneuverOf(s);
                return m != null && st.count(m) > 0;
        }
    }

    public static Encouragement build(PracticeSetup setup, DriveStats st, GuidanceLevel level) {
        List<String> achieved = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        boolean practice = setup != null && !setup.generalDrive;
        if (practice) {
            for (DrivingSkill s : setup.skills) {
                (done(s, st) ? achieved : missed).add(s.label());
            }
            if (setup.destination != null && setup.destination.destination != null) {
                (st.arrived ? achieved : missed).add(setup.destination.destination.name + " 도착");
            }
            if (setup.amount != null && st.minutes() > 0) {
                achieved.add(st.minutes() + "분 주행");
            }
        } else if (st.arrived && setup != null && setup.selectedRoute != null) {
            achieved.add(setup.selectedRoute.title + " 도착");
        }
        if (achieved.isEmpty() && st.minutes() > 0) {
            achieved.add(st.minutes() + "분 주행");
        }

        String title;
        String subtitle;
        if (practice && !achieved.isEmpty() && missed.isEmpty()) {
            title = "오늘 목표를 해냈어요";
            subtitle = "천천히 잘 다녀오셨어요.";
        } else if (practice && !achieved.isEmpty()) {
            title = "한 걸음 더 나아갔어요";
            subtitle = "해낸 것부터 차근차근 늘려 가요.";
        } else if (st.arrived) {
            title = "안전하게 도착했어요";
            subtitle = "오늘도 수고 많으셨어요.";
        } else {
            title = "오늘도 운전대를 잡았어요";
            subtitle = "도전한 것만으로도 충분히 잘했어요.";
        }
        if (st.relaxRequests > 0 || st.rests > 0) {
            subtitle = "긴장될 때 쉬어 가며 끝까지 해냈어요.";
        }

        StringBuilder speech = new StringBuilder(title).append(". ");
        if (level != GuidanceLevel.MINIMAL) {
            if (!achieved.isEmpty()) {
                speech.append(String.join(", ", achieved)).append("을 해냈어요. ");
            }
            if (level == GuidanceLevel.ACTIVE && !missed.isEmpty()) {
                speech.append(String.join(", ", missed)).append("은 다음에 다시 해 봐요. ");
            }
            speech.append(subtitle);
        }

        String next = !missed.isEmpty() ? "다음에는 " + missed.get(0) + "을 다시 연습해 봐요"
                : practice ? "다음에는 조금 더 긴 거리나 새로운 길에 도전해 봐요"
                : "연습 주행으로 어려운 상황을 미리 익혀 봐요";
        return new Encouragement(title, subtitle, achieved, missed, speech.toString().trim(), next);
    }
}
