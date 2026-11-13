package com.example.application.domain;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.model.PracticePreset;
import com.example.application.model.PresetItem;

import java.util.EnumMap;
import java.util.Map;

/** 프리셋 + 사용자 수정 → 실제로 적용할 차량 설정값. */
public final class PresetTargets {
    /** 사용자가 "그대로 두기"를 고른 항목. */
    public static final String KEEP = "KEEP";

    private PresetTargets() {
    }

    /**
     * @param overrides    "항목 수정"에서 바꾼 값(KEEP이면 적용 제외)
     * @param consentGiven 동의가 필요한 항목에 동의했는지. 아니면 해당 항목 제외
     */
    public static Map<SettingKey, Object> resolve(PracticePreset preset, Map<SettingKey, Object> overrides,
                                                  boolean consentGiven) {
        Map<SettingKey, Object> out = new EnumMap<>(SettingKey.class);
        for (PresetItem item : preset.items) {
            if (item.requiresConsent && !consentGiven) {
                continue;
            }
            out.putAll(item.targets);
        }
        if (overrides != null) {
            for (Map.Entry<SettingKey, Object> e : overrides.entrySet()) {
                if (KEEP.equals(e.getValue())) {
                    out.remove(e.getKey());
                } else if (e.getValue() != null) {
                    out.put(e.getKey(), e.getValue());
                }
            }
        }
        return out;
    }

    public static boolean needsConsent(PracticePreset preset) {
        for (PresetItem item : preset.items) {
            if (item.requiresConsent) {
                return true;
            }
        }
        return false;
    }

    /** 풍량 "약하게" → 차량 범위의 하위 25% 지점(최소값보다 크게). */
    public static int lowFan(int min, int max) {
        int v = min + Math.round((max - min) * 0.25f);
        return Math.max(min, Math.min(max, v));
    }
}
