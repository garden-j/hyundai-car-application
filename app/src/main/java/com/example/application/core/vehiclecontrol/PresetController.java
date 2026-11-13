package com.example.application.core.vehiclecontrol;

import com.example.application.core.Callback;
import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleGateway;
import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.domain.PresetTargets;
import com.example.application.model.PresetItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 연습 모드 차량 환경을 적용한다.
 * 1) 현재 설정을 읽어 저장(연습 후 복원용) → 2) 상대값(풍량 약하게)을 차량 범위로 변환
 * → 3) 지원 항목을 순서대로 적용하고 항목별 성공·실패를 모은다.
 */
public class PresetController {
    private final VehicleGateway vehicle;

    public PresetController(VehicleGateway vehicle) {
        this.vehicle = vehicle;
    }

    /** 결과는 실패 항목이 있어도 onSuccess로 전달한다(항목별 결과는 report에). */
    public void apply(Map<SettingKey, Object> targets, Callback<ApplyReport> callback) {
        ApplyReport report = new ApplyReport();
        vehicle.readSettings(new Callback<VehicleSettings>() {
            @Override
            public void onSuccess(VehicleSettings before) {
                report.before = before;
                report.applied = before.copy();
                resolveRelative(targets, resolved -> applyAll(new ArrayList<>(resolved.entrySet()), 0, report, callback));
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private interface Resolved {
        void accept(Map<SettingKey, Object> resolved);
    }

    private void resolveRelative(Map<SettingKey, Object> targets, Resolved next) {
        if (!PresetItem.FAN_LOW.equals(targets.get(SettingKey.FAN_SPEED))) {
            next.accept(targets);
            return;
        }
        vehicle.readFanSpeedRange(new Callback<int[]>() {
            @Override
            public void onSuccess(int[] range) {
                targets.put(SettingKey.FAN_SPEED, PresetTargets.lowFan(range[0], range[1]));
                next.accept(targets);
            }

            @Override
            public void onError(Exception e) {
                targets.put(SettingKey.FAN_SPEED, 2); // 범위를 모르면 일반적인 약한 풍량
                next.accept(targets);
            }
        });
    }

    private void applyAll(List<Map.Entry<SettingKey, Object>> entries, int index, ApplyReport report,
                          Callback<ApplyReport> callback) {
        if (index >= entries.size()) {
            callback.onSuccess(report);
            return;
        }
        SettingKey key = entries.get(index).getKey();
        Object value = entries.get(index).getValue();
        if (VehicleSettings.sameValue(key, report.before.get(key), value)) {
            report.results.add(new ApplyReport.Result(key, value, true, "이미 맞춰져 있어요"));
            applyAll(entries, index + 1, report, callback);
            return;
        }
        vehicle.applySetting(key, value, new Callback<Object>() {
            @Override
            public void onSuccess(Object actual) {
                report.applied.set(key, actual);
                report.results.add(new ApplyReport.Result(key, value, true, VehicleSettings.format(key, actual)));
                applyAll(entries, index + 1, report, callback);
            }

            @Override
            public void onError(Exception e) {
                report.results.add(new ApplyReport.Result(key, value, false, friendly(e)));
                applyAll(entries, index + 1, report, callback);
            }
        });
    }

    private static String friendly(Exception e) {
        String m = e.getMessage();
        if (e instanceof SecurityException || (m != null && m.contains("ermission"))) {
            return "차량 제어 권한이 없어요";
        }
        if (e instanceof UnsupportedOperationException && m != null) {
            return m;
        }
        return "이 차량에서는 바꿀 수 없어요";
    }
}
