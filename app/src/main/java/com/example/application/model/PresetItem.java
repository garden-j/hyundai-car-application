package com.example.application.model;

import com.example.application.core.pleos.vehicle.SettingKey;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 연습 모드 프리셋의 확인 항목 한 줄(음악·공조·창문·좌석·핸들 열선).
 * targets가 비어 있으면 차량 제어 없이 안내만 한다(음악: API 없음, 좌석: 저장된 설정 유지).
 */
public class PresetItem {
    public enum Category {
        MUSIC("음악"), CLIMATE("공조"), WINDOW("창문"), SEAT("좌석"), WHEEL_HEAT("핸들 열선"), DEFROST("김서림 제거");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final Category category;
    /** 화면에 보이는 설정 설명(예: "쾌적한 온도 · 약한 풍량"). */
    public final String summary;
    /** 사용자가 직접 해야 하는 항목이면 true(예: 음악 끄기). */
    public final boolean manual;
    /** 적용 전 별도 동의가 필요한 항목(핸들 열선 등). */
    public final boolean requiresConsent;
    /** 적용할 차량 설정. 값이 {@link #FAN_LOW} 같은 상대값이면 적용 시 차량 범위에 맞춰 바꾼다. */
    public final Map<SettingKey, Object> targets;

    /** 풍량 "약하게": 차량 풍량 범위의 하위 25% 지점. */
    public static final String FAN_LOW = "FAN_LOW";

    public PresetItem(Category category, String summary, boolean manual, boolean requiresConsent,
                      Map<SettingKey, Object> targets) {
        this.category = category;
        this.summary = summary;
        this.manual = manual;
        this.requiresConsent = requiresConsent;
        this.targets = targets;
    }

    public static PresetItem manual(Category c, String summary) {
        return new PresetItem(c, summary, true, false, new LinkedHashMap<>());
    }

    public static PresetItem keep(Category c, String summary) {
        return new PresetItem(c, summary, false, false, new LinkedHashMap<>());
    }

    public static PresetItem control(Category c, String summary, boolean requiresConsent, Object... keyValues) {
        Map<SettingKey, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((SettingKey) keyValues[i], keyValues[i + 1]);
        }
        return new PresetItem(c, summary, false, requiresConsent, m);
    }
}
