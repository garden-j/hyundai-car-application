package com.example.application.domain;

import com.example.application.model.GuidanceLevel;

/**
 * 주행 중 환경 변화 감지: 와이퍼 작동 → 비, 전조등 켜짐 → 야간·터널.
 * 꺼짐→켜짐으로 바뀐 순간에만 한 번 안내한다.
 */
public final class EnvironmentWatch {
    public enum Change { RAIN, DARK }

    private Boolean lastWiper;
    private Boolean lastLowBeam;

    /** 새 상태를 넣고, 새로 생긴 변화를 돌려준다(없으면 null). 처음 값은 기준으로만 쓴다. */
    public Change update(Boolean wiperOn, Boolean lowBeamOn) {
        Change change = null;
        if (wiperOn != null) {
            if (Boolean.FALSE.equals(lastWiper) && wiperOn) {
                change = Change.RAIN;
            }
            lastWiper = wiperOn;
        }
        if (lowBeamOn != null) {
            if (change == null && Boolean.FALSE.equals(lastLowBeam) && lowBeamOn) {
                change = Change.DARK;
            }
            lastLowBeam = lowBeamOn;
        }
        return change;
    }

    public static String message(Change c, GuidanceLevel level) {
        boolean minimal = level == GuidanceLevel.MINIMAL;
        if (c == Change.RAIN) {
            return minimal ? "비가 와요. 천천히 가요."
                    : "비가 오기 시작했어요. 앞차와 거리를 평소의 두 배로 두고 천천히 가요. 화면에서 편안한 길로 바꿀 수 있어요.";
        }
        return minimal ? "어두워졌어요. 천천히 가요."
                : "주변이 어두워졌어요. 보행자와 차선을 더 천천히 살피며 가요. 화면에서 편안한 길로 바꿀 수 있어요.";
    }

    public static String label(Change c) {
        return c == Change.RAIN ? "비가 와요" : "어두워졌어요";
    }
}
