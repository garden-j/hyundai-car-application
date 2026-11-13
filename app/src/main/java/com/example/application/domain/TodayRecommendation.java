package com.example.application.domain;

import com.example.application.model.DifficultSituation;
import com.example.application.model.Difficulty;
import com.example.application.model.DriverProfile;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;

/** 홈 화면 "연습 주행 · 오늘의 추천" 카드. 온보딩에서 고른 어려운 상황을 우선 연습하도록 제안한다. */
public class TodayRecommendation {
    public final String title;
    public final String meta;
    public final String reason;
    public final DrivingSkill skill;

    private TodayRecommendation(String title, String meta, String reason, DrivingSkill skill) {
        this.title = title;
        this.meta = meta;
        this.reason = reason;
        this.skill = skill;
    }

    public static TodayRecommendation from(DriverProfile profile) {
        if (profile != null) {
            for (DifficultSituation d : profile.difficulties) {
                if (d.relatedSkill != null) {
                    String skill = d.relatedSkill.label();
                    return new TodayRecommendation("동네에서\n" + skill + " 1번",
                            "약 15분 · 넓은 길 · 한산한 시간",
                            "어렵다고 고른 " + skill + "부터 천천히 해요",
                            d.relatedSkill);
                }
            }
        }
        return new TodayRecommendation("동네 한 바퀴\n기본 주행", "약 15분 · 넓은 길 · 한산한 시간",
                "익숙한 길에서 기본 주행부터 시작해요", null);
    }

    /** "연습 시작"을 누르면 추천 내용을 설정 초안에 미리 채운다(사용자가 단계마다 바꿀 수 있음). */
    public void prefill(PracticeSetup s) {
        if (skill != null) {
            s.goalTypes.add(PracticeGoalType.SKILL);
            s.skills.add(skill);
        } else {
            s.goalTypes.add(PracticeGoalType.DISTANCE_TIME);
        }
        s.difficulty = Difficulty.EASY;
        s.familiarity = Familiarity.FAMILIAR;
    }
}
