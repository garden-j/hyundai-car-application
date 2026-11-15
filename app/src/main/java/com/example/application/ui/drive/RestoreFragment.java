package com.example.application.ui.drive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.core.Callback;
import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.data.PracticeSession;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;
import java.util.List;

/**
 * 연습 모드 차량 환경 복원: 연습 전에 저장한 설정과 지금 설정을 비교해 보여 주고,
 * 사용자가 고르면 바뀐 항목만 원래대로 돌린다(공조 전원은 마지막에 끈다).
 */
public class RestoreFragment extends BaseStepFragment {
    private final List<SettingKey> changed = new ArrayList<>();
    private VehicleSettings before;

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setSection("");
        setTitle("차량 설정을 되돌릴까요?", "연습 전 설정과 지금 설정을 비교했어요.");
        setPrimary("원래대로 돌리기", this::restore);
        setSecondary("이대로 두기", this::done);
        setPrimaryEnabled(false);
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        PracticeSession session = services().sessions.active();
        before = session == null ? null : session.settingsBefore;
        if (before == null) {
            Rows.add(list, null, "저장된 설정이 없어요", null, null);
            return null;
        }
        services().vehicle.readSettings(new Callback<VehicleSettings>() {
            @Override
            public void onSuccess(VehicleSettings now) {
                if (!isAdded()) {
                    return;
                }
                for (SettingKey k : SettingKey.values()) {
                    Object was = before.get(k);
                    Object is = now.get(k);
                    if (was != null && !VehicleSettings.sameValue(k, was, is)) {
                        changed.add(k);
                        Rows.add(list, null, k.label, "지금 " + VehicleSettings.format(k, is),
                                "원래 " + VehicleSettings.format(k, was));
                    }
                }
                if (changed.isEmpty()) {
                    Rows.add(list, null, "바뀐 설정이 없어요", "연습 전과 같아요.", null);
                } else {
                    setPrimaryEnabled(true);
                }
            }

            @Override
            public void onError(Exception e) {
                Rows.add(list, null, "지금 설정을 읽지 못했어요", e.getMessage(), null);
            }
        });
        return null;
    }

    private void restore() {
        setPrimaryEnabled(false);
        // 공조 전원은 다른 공조 항목을 되돌린 뒤 마지막에(꺼져 있으면 온도·풍량을 바꿀 수 없음)
        List<SettingKey> order = new ArrayList<>(changed);
        if (order.remove(SettingKey.HVAC_POWER)) {
            if (Boolean.TRUE.equals(before.get(SettingKey.HVAC_POWER))) {
                order.add(0, SettingKey.HVAC_POWER);
            } else {
                order.add(SettingKey.HVAC_POWER);
            }
        }
        applyNext(order, 0, 0);
    }

    private void applyNext(List<SettingKey> order, int i, int failed) {
        if (i >= order.size()) {
            String msg = failed == 0 ? "차량 설정을 연습 전으로 되돌렸어요." : failed + "개 항목은 되돌리지 못했어요. 직접 확인해 주세요.";
            speak(msg);
            toast(msg);
            done();
            return;
        }
        SettingKey k = order.get(i);
        services().vehicle.applySetting(k, before.get(k), new Callback<Object>() {
            @Override
            public void onSuccess(Object value) {
                applyNext(order, i + 1, failed);
            }

            @Override
            public void onError(Exception e) {
                applyNext(order, i + 1, failed + 1);
            }
        });
    }

    private void done() {
        services().sessions.clear();
        if (isAdded()) {
            nav().home();
        }
    }
}
