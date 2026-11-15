package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.core.vehiclecontrol.ApplyReport;
import com.example.application.domain.PresetTargets;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.PresetItem;
import com.example.application.ui.common.Resource;
import com.example.application.ui.common.Rows;

import java.util.List;
import java.util.Map;

/**
 * 차 안을 이렇게 맞춰 둘까요? — 목표·날씨·시간 기반 연습 모드 프리셋 추천 → 적용 예정 환경 확인
 * → (항목 수정) → 지원 항목 일괄 적용 → 실패 항목 안내 → 출발.
 */
public class PresetFragment extends BaseSetupFragment {
    private LinearLayout rows;

    @Override
    protected SetupStep step() {
        return SetupStep.PRESET;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setupVm().recommendPreset();
        setTitle("차 안을 이렇게 맞춰 둘까요?", setupVm().presetReason());
        enableVoice("이대로 해 줘");
        setPrimary("이대로 적용", this::apply);
        setSecondary("항목 수정", () -> nav().push(new PresetEditFragment()));
        rows = scrollList(inflater);
        render();
        setupVm().applyResult().observe(getViewLifecycleOwner(), this::onApplied);
        return null;
    }

    private void render() {
        rows.removeAllViews();
        Map<SettingKey, Object> targets = setupVm().presetTargets();
        for (PresetItem item : setup().preset.items) {
            String value = item.summary;
            // 사용자가 수정한 값이 있으면 그 항목의 실제 적용값을 보여준다
            boolean overridden = false;
            Map<SettingKey, Object> itemTargets = new java.util.LinkedHashMap<>();
            for (SettingKey k : item.targets.keySet()) {
                overridden |= setup().presetOverrides.get(k) != null;
                if (targets.containsKey(k)) {
                    itemTargets.put(k, targets.get(k));
                }
            }
            if (overridden) {
                value = itemTargets.isEmpty() ? "현재 설정 유지" : describe(itemTargets);
                if (value.isEmpty()) {
                    value = item.summary;
                }
            }
            Rows.add(rows, null, item.category.label, null,
                    value);
            if (item.requiresConsent) {
                CheckBox consent = new CheckBox(requireContext());
                consent.setText(item.category.label + " 켜는 데 동의해요");
                consent.setTextSize(18);
                consent.setChecked(setup().presetConsentGiven);
                consent.setOnCheckedChangeListener((b, checked) -> setup().presetConsentGiven = checked);
                rows.addView(consent);
            }
        }
        // 프리셋에 없던 항목을 "항목 수정"에서 추가로 바꾼 경우
        Map<SettingKey, Object> extra = new java.util.LinkedHashMap<>();
        for (Map.Entry<SettingKey, Object> e : targets.entrySet()) {
            boolean inPreset = false;
            for (PresetItem item : setup().preset.items) {
                inPreset |= item.targets.containsKey(e.getKey());
            }
            if (!inPreset) {
                extra.put(e.getKey(), e.getValue());
            }
        }
        if (!extra.isEmpty()) {
            Rows.add(rows, null, "추가 설정", null, describe(extra));
        }
        if (!setupVm().vehicleControlAllowed()) {
            Rows.add(rows, "!", "차량 제어에 동의하지 않았어요", "설정은 직접 맞춰 주세요. 설정에서 동의할 수 있어요.", null);
        }
    }

    private static String describe(Map<SettingKey, Object> targets) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SettingKey, Object> e : targets.entrySet()) {
            if (e.getValue() instanceof String) {
                continue; // FAN_LOW 같은 상대값은 적용 시 결정
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(e.getKey().label).append(' ').append(VehicleSettings.format(e.getKey(), e.getValue()));
        }
        return sb.toString();
    }

    private void apply() {
        if (!setupVm().vehicleControlAllowed()) {
            setupVm().skipPreset();
            speak("차량 설정은 직접 맞춰 주세요. 출발할게요.");
            goNext();
            return;
        }
        setupVm().applyPreset();
    }

    private void onApplied(Resource<ApplyReport> res) {
        if (res == null) {
            return;
        }
        if (res.state == Resource.State.LOADING) {
            setPrimaryEnabled(false);
            setSubtitle("차량 환경을 맞추고 있어요…");
            return;
        }
        setPrimaryEnabled(true);
        if (res.state == Resource.State.ERROR) {
            speak(res.error);
            new AlertDialog.Builder(requireContext()).setTitle("차량 설정을 적용하지 못했어요")
                    .setMessage(res.error).setPositiveButton("출발", (d, w) -> goNext()).show();
            return;
        }
        List<ApplyReport.Result> failures = res.data.failures();
        if (failures.isEmpty()) {
            speak("차량 환경을 맞췄어요. 출발해요.");
            goNext();
            return;
        }
        StringBuilder msg = new StringBuilder();
        for (ApplyReport.Result r : failures) {
            msg.append("• ").append(r.key.label).append(": ").append(r.message).append('\n');
        }
        speak(failures.size() + "개 항목은 적용하지 못했어요. 필요하면 직접 바꿔 주세요.");
        new AlertDialog.Builder(requireContext())
                .setTitle("일부 항목은 적용하지 못했어요")
                .setMessage(msg.toString().trim() + "\n\n나머지는 적용했어요.")
                .setPositiveButton("출발", (d, w) -> goNext())
                .setCancelable(false)
                .show();
    }

    @Override
    protected boolean onVoiceText(String text) {
        if (VoiceCommandParser.isYes(text)) {
            apply();
            return true;
        }
        if (VoiceCommandParser.normalize(text).contains("수정")) {
            nav().push(new PresetEditFragment());
            return true;
        }
        return false;
    }

}
