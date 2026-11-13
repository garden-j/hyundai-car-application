package com.example.application.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.MainActivity;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.databinding.ItemRowBinding;
import com.example.application.model.PermissionType;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

/**
 * 0-1 시작 전에 권한 확인이 필요해요 — 목적별로 권한을 보여주고 항목별로 고를 수 있다.
 * 마이크는 안드로이드 런타임 권한도 함께 요청한다. 일부라도 거부하면 기능 제한 안내로 이동.
 */
public class PermissionFragment extends BaseStepFragment {
    private final Set<PermissionType> checked = EnumSet.allOf(PermissionType.class);
    private final java.util.Map<PermissionType, ItemRowBinding> rows = new java.util.EnumMap<>(PermissionType.class);

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("시작 전에\n권한 확인이 필요해요", "운전을 돕는 데 필요한 것만 사용해요.");
        setProgress(1, 4);
        showNav(false, false);
        setPrimary("동의하고 시작", this::agree);
        setSecondary("거부", this::denyAll);

        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        int i = 1;
        for (PermissionType t : PermissionType.values()) {
            ItemRowBinding row = Rows.add(list, String.valueOf(i++), t.label(), t.description(), "");
            row.getRoot().setOnClickListener(v -> {
                if (!checked.remove(t)) {
                    checked.add(t);
                }
                render();
            });
            rows.put(t, row);
        }
        render();
        return null;
    }

    private void render() {
        for (java.util.Map.Entry<PermissionType, ItemRowBinding> e : rows.entrySet()) {
            e.getValue().rowTrailing.setText(checked.contains(e.getKey()) ? "✓ 동의" : "동의 안 함");
        }
        frame.btnPrimary.setText(checked.size() == PermissionType.values().length ? "동의하고 시작" : "선택한 항목만 동의");
    }

    /** 동의한 항목 중 안드로이드 런타임 권한이 필요한 것(마이크·위치)은 한 번에 요청한다. */
    private void agree() {
        java.util.List<String> runtime = new java.util.ArrayList<>();
        if (checked.contains(PermissionType.MICROPHONE)) {
            runtime.add(android.Manifest.permission.RECORD_AUDIO);
        }
        if (checked.contains(PermissionType.LOCATION)) {
            runtime.add(MainActivity.PERMISSION_FUSED_LOCATION);
        }
        if (runtime.isEmpty()) {
            record();
            return;
        }
        ((MainActivity) requireActivity()).ensurePermissions(runtime.toArray(new String[0]), result -> {
            if (Boolean.FALSE.equals(result.get(android.Manifest.permission.RECORD_AUDIO))) {
                checked.remove(PermissionType.MICROPHONE);
            }
            if (Boolean.FALSE.equals(result.get(MainActivity.PERMISSION_FUSED_LOCATION))) {
                checked.remove(PermissionType.LOCATION);
            }
            record();
        });
    }

    private void denyAll() {
        checked.clear();
        record();
    }

    private void record() {
        ArrayList<String> denied = new ArrayList<>();
        for (PermissionType t : PermissionType.values()) {
            boolean granted = checked.contains(t);
            services().consents.record(t, granted);
            if (!granted) {
                denied.add(t.name());
            }
        }
        if (denied.isEmpty()) {
            nav().push(new ExperienceFragment());
        } else {
            nav().push(PermissionDeniedFragment.newInstance(denied));
        }
    }
}
