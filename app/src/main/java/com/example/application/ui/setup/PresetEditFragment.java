package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.domain.PresetTargets;
import com.example.application.model.PresetItem;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 항목별 수정 — 공조 온도·풍량·창문·핸들 열선·김서림 제거를 바꾸거나 "그대로 두기". */
public class PresetEditFragment extends BaseStepFragment {

    private static final class Option {
        final String label;
        final Object value;

        Option(String label, Object value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final Option KEEP = new Option("그대로 두기", PresetTargets.KEEP);

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("항목별로\n바꿔 볼까요?", "고른 값으로 적용해요.");
        setPrimary("저장", () -> nav().back());
        LinearLayout list = com.example.application.databinding.ContentScrollBinding
                .inflate(inflater, frame.content, true).list;
        Map<SettingKey, Object> overrides = setupVm().setup().presetOverrides;
        Map<SettingKey, Object> current = setupVm().presetTargets();

        section(list, SettingKey.CABIN_TEMPERATURE, overrides, current, Arrays.asList(
                new Option("20℃", 20.0f), new Option("21℃", 21.0f), new Option("22℃", 22.0f),
                new Option("23℃", 23.0f), new Option("24℃", 24.0f), KEEP));
        section(list, SettingKey.FAN_SPEED, overrides, current, Arrays.asList(
                new Option("약하게", PresetItem.FAN_LOW), new Option("보통", 3), new Option("세게", 5), KEEP));
        section(list, SettingKey.DRIVER_WINDOW, overrides, current, Arrays.asList(
                new Option("닫기", 0), KEEP));
        section(list, SettingKey.STEERING_WHEEL_HEAT, overrides, current, Arrays.asList(
                new Option("끄기", 0), new Option("켜기", 1), KEEP));
        section(list, SettingKey.DEFROSTER, overrides, current, Arrays.asList(
                new Option("끄기", false), new Option("켜기", true), KEEP));
        return null;
    }

    private void section(LinearLayout list, SettingKey key, Map<SettingKey, Object> overrides,
                         Map<SettingKey, Object> current, List<Option> options) {
        Set<Option> selected = new LinkedHashSet<>();
        Object now = overrides.containsKey(key) ? overrides.get(key) : current.getOrDefault(key, PresetTargets.KEEP);
        for (Option o : options) {
            if (equalsValue(o.value, now)) {
                selected.add(o);
            }
        }
        Choices.addChipSection(list, key.label, options, o -> o.label, selected, true, () -> {
            if (selected.isEmpty()) {
                overrides.remove(key);
            } else {
                Option o = selected.iterator().next();
                overrides.put(key, o.value);
                if (key == SettingKey.STEERING_WHEEL_HEAT && Integer.valueOf(1).equals(o.value)) {
                    setupVm().setup().presetConsentGiven = true; // 직접 켜기를 고른 것은 동의로 본다
                }
            }
        });
    }

    private static boolean equalsValue(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Math.abs(((Number) a).doubleValue() - ((Number) b).doubleValue()) < 0.01;
        }
        return a != null && a.equals(b);
    }
}
