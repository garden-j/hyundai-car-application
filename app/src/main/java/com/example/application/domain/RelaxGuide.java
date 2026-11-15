package com.example.application.domain;

import com.example.application.model.GuidanceLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Relax(긴장 완화)·진정·휴식 안내 문구. 주행 중에는 짧고 차분하게,
 * 정차(기어 P) 중에만 호흡 안내처럼 긴 안내를 한다. Relax 안내 수준에 따라 길이를 조절한다.
 */
public final class RelaxGuide {
    private RelaxGuide() {
    }

    /** 사용자가 "긴장돼요"를 눌렀거나 말했을 때. */
    public static List<String> calm(GuidanceLevel level, boolean parked) {
        if (parked) {
            return breathing(level);
        }
        if (level == GuidanceLevel.MINIMAL) {
            return Arrays.asList("괜찮아요. 천천히 가도 돼요.");
        }
        List<String> lines = new ArrayList<>(Arrays.asList(
                "괜찮아요. 서두르지 않아도 돼요.",
                "숨을 길게 내쉬고, 앞차와 거리를 넉넉히 두세요."));
        if (level == GuidanceLevel.ACTIVE) {
            lines.add("뒤차가 가까워도 내 속도로 가면 돼요. 쉬고 싶으면 쉬어 갈까요라고 말해 주세요.");
        }
        return lines;
    }

    /** 정차 중 호흡 안내(4초 들이쉬고 6초 내쉬기). */
    public static List<String> breathing(GuidanceLevel level) {
        List<String> lines = new ArrayList<>();
        lines.add("차가 멈춰 있어요. 잠깐 같이 숨을 골라 볼게요.");
        int rounds = level == GuidanceLevel.MINIMAL ? 1 : level == GuidanceLevel.ACTIVE ? 3 : 2;
        for (int i = 0; i < rounds; i++) {
            lines.add("코로 천천히 들이쉬고");
            lines.add("입으로 길게 내쉬어요.");
        }
        lines.add("잘하셨어요. 준비가 되면 다시 출발해요.");
        return lines;
    }

    /** 어려운 구간을 연달아 지난 뒤 자동으로 건네는 짧은 한마디. 끔(최소)이면 null. */
    public static String afterHardSection(GuidanceLevel level) {
        if (level == GuidanceLevel.MINIMAL) {
            return null;
        }
        return level == GuidanceLevel.ACTIVE
                ? "어려운 구간을 잘 지나왔어요. 어깨에 힘을 빼고 천천히 가요."
                : "잘 지나왔어요. 천천히 가요.";
    }

    public static String askRest() {
        return "잠시 쉬어 갈까요? 근처 주차장을 경유지로 넣어 드릴게요.";
    }

    public static String restAdded(String placeName) {
        return Josa.eulReul(placeName) + " 경유지로 넣었어요. 안전하게 들어가서 쉬어요.";
    }

    public static String restartCheck() {
        return "충분히 쉬셨나요? 괜찮으면 천천히 다시 출발해요. 힘들면 오늘은 여기까지 해도 괜찮아요.";
    }
}
