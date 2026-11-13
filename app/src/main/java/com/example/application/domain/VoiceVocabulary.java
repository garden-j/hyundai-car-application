package com.example.application.domain;

import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.GuidanceLevel;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.RoadEnvironment;
import com.example.application.model.TensionReason;

import java.util.LinkedHashMap;
import java.util.Map;

/** 화면별 음성 답변 키워드 사전. 키는 쉼표로 구분한 동의어 목록. */
public final class VoiceVocabulary {
    private VoiceVocabulary() {
    }

    public static final Map<String, PracticeGoalType> GOAL_TYPES = new LinkedHashMap<>();
    public static final Map<String, DrivingSkill> SKILLS = new LinkedHashMap<>();
    public static final Map<String, RoadEnvironment> ENVIRONMENTS = new LinkedHashMap<>();
    public static final Map<String, Difficulty> DIFFICULTIES = new LinkedHashMap<>();
    public static final Map<String, Familiarity> FAMILIARITY = new LinkedHashMap<>();
    public static final Map<String, TensionReason> TENSION_REASONS = new LinkedHashMap<>();
    public static final Map<String, GuidanceLevel> GUIDANCE = new LinkedHashMap<>();
    public static final Map<String, String> PLACES = new LinkedHashMap<>();

    static {
        GOAL_TYPES.put("주행기술,기술,조작", PracticeGoalType.SKILL);
        GOAL_TYPES.put("거리,시간,킬로,분동안", PracticeGoalType.DISTANCE_TIME);
        GOAL_TYPES.put("목적지,가고싶,어디", PracticeGoalType.DESTINATION);
        GOAL_TYPES.put("주행환경,환경,야간,좁은길,빗길", PracticeGoalType.ENVIRONMENT);

        SKILLS.put("좌회전,왼쪽", DrivingSkill.LEFT_TURN);
        SKILLS.put("우회전,오른쪽", DrivingSkill.RIGHT_TURN);
        SKILLS.put("유턴,u턴,유-턴", DrivingSkill.U_TURN);
        SKILLS.put("차로변경,차선변경,차선바꾸,차로바꾸,끼어들", DrivingSkill.LANE_CHANGE);
        SKILLS.put("교차로", DrivingSkill.INTERSECTION);
        SKILLS.put("합류,고속도로진입", DrivingSkill.MERGE);
        SKILLS.put("주차", DrivingSkill.PARKING);

        ENVIRONMENTS.put("주택가,좁은,골목", RoadEnvironment.RESIDENTIAL);
        ENVIRONMENTS.put("넓은,다차선,큰길,대로", RoadEnvironment.WIDE_MULTILANE);
        ENVIRONMENTS.put("한산,교통량이적,차없,차가적", RoadEnvironment.LOW_TRAFFIC);
        ENVIRONMENTS.put("복잡한교차로,복잡한", RoadEnvironment.COMPLEX_INTERSECTION);
        ENVIRONMENTS.put("야간,밤,어두", RoadEnvironment.NIGHT);
        ENVIRONMENTS.put("우천,비오,빗길,비올", RoadEnvironment.RAIN);

        DIFFICULTIES.put("쉬움,쉽게,쉬운,쉬워", Difficulty.EASY);
        DIFFICULTIES.put("보통,중간", Difficulty.NORMAL);
        DIFFICULTIES.put("도전,어렵게,어려운,빡세", Difficulty.CHALLENGE);

        FAMILIARITY.put("익숙,아는곳,동네", Familiarity.FAMILIAR);
        FAMILIARITY.put("새로운,처음,모르는", Familiarity.NEW);

        TENSION_REASONS.put("처음가는,모르는길,낯선", TensionReason.UNFAMILIAR_ROAD);
        TENSION_REASONS.put("차가많,막힐,교통", TensionReason.HEAVY_TRAFFIC);
        TENSION_REASONS.put("날씨,비,어두", TensionReason.WEATHER_DARK);
        TENSION_REASONS.put("조작,서툴", TensionReason.SKILL_WORRY);
        TENSION_REASONS.put("지난번,저번,힘들었", TensionReason.PAST_EXPERIENCE);
        TENSION_REASONS.put("컨디션,피곤,졸려", TensionReason.CONDITION);

        GUIDANCE.put("최소,간단,짧게,적게", GuidanceLevel.MINIMAL);
        GUIDANCE.put("표준,보통,중간", GuidanceLevel.STANDARD);
        GUIDANCE.put("적극,자세히,많이,상세", GuidanceLevel.ACTIVE);

        PLACES.put("집", "집");
        PLACES.put("직장,회사", "직장");
        PLACES.put("학교", "학교");
        PLACES.put("마트,시장", "마트");
    }

    /** 긴장도 말 표현 → 1~5. 숫자가 있으면 숫자 우선. */
    public static Integer tensionLevel(String text) {
        Integer n = VoiceCommandParser.parseNumber(text);
        if (n != null && n >= 1 && n <= 5) {
            return n;
        }
        String norm = VoiceCommandParser.normalize(text);
        if (norm.contains("많이") || norm.contains("엄청") || norm.contains("너무")) {
            return 5;
        }
        if (norm.contains("조금") || norm.contains("약간")) {
            return 2;
        }
        if (norm.contains("편안") || norm.contains("괜찮") || norm.contains("안긴장")) {
            return 1;
        }
        if (norm.contains("긴장")) {
            return 4;
        }
        return null;
    }
}
