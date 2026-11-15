package com.example.application.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.MainActivity;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.databinding.ItemRowBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.model.ConsentRecord;
import com.example.application.model.DriverProfile;
import com.example.application.model.GuidanceLevel;
import com.example.application.model.PermissionType;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Rows;
import com.example.application.ui.onboarding.ExperienceFragment;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** 설정 — 안내량 변경, 권한별 동의 변경·철회(일시 표시), 운전 경험 다시 입력, 연결 상태. */
public class SettingsFragment extends BaseStepFragment {
    private LinearLayout consentList;

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("설정", "안내량과 권한을 바꿀 수 있어요.");
        showNav(true, false);
        setPrimary("운전 경험 다시 입력", () -> nav().push(new ExperienceFragment()));
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;

        DriverProfile p = services().profiles.get();
        Set<GuidanceLevel> level = EnumSet.of(p.guidanceLevelOrDefault());
        Choices.addChipSection(list, "안내량", Arrays.asList(GuidanceLevel.values()), level, true, () -> {
            if (!level.isEmpty()) {
                p.guidanceLevel = level.iterator().next();
                services().profiles.save(p);
            }
        });

        list.addView(Choices.sectionTitle(requireContext(), "권한 (눌러서 변경)"));
        consentList = new LinearLayout(requireContext());
        consentList.setOrientation(LinearLayout.VERTICAL);
        list.addView(consentList);
        renderConsents();

        list.addView(Choices.sectionTitle(requireContext(), "연결 상태"));
        ServiceLocator sl = services();
        Rows.add(list, null, "Vehicle", null, sl.vehicle.isSimulated() ? "시뮬레이션" : "Pleos 연결");
        Rows.add(list, null, "NaviHelper", null, sl.navi.isSimulated() ? "시뮬레이션" : "Pleos 연결");
        Rows.add(list, null, "FusedLocation", null, sl.location.isSimulated() ? "시뮬레이션" : "Pleos 연결");
        Rows.add(list, null, "음성 안내(TTS)", null, sl.tts.isSimulated() ? "안드로이드 기본" : "Pleos 연결");
        Rows.add(list, null, "음성 인식(STT)", null, sl.stt.isSimulated() ? "사용 불가" : "Pleos 연결");
        return null;
    }

    private void renderConsents() {
        consentList.removeAllViews();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        for (PermissionType t : PermissionType.values()) {
            ConsentRecord r = services().consents.latest(t);
            boolean granted = services().consents.isGranted(t);
            String when = r == null ? "아직 묻지 않음"
                    : (granted ? "동의 " : "미동의 ") + fmt.format(new Date(r.timestampMillis));
            ItemRowBinding row = Rows.add(consentList, null, t.label(),
                    granted ? t.description() : t.limitationWhenDenied + "\n" + when, granted ? "켜짐" : "꺼짐");
            row.getRoot().setOnClickListener(v -> toggle(t, !granted));
        }
    }

    private void toggle(PermissionType t, boolean grant) {
        if (grant && t == PermissionType.MICROPHONE) {
            ((MainActivity) requireActivity()).ensureMicPermission(ok -> {
                services().consents.record(t, ok);
                if (!ok) {
                    toast("안드로이드 설정에서 마이크 권한을 허용해 주세요.");
                }
                renderConsents();
            });
            return;
        }
        if (grant && t == PermissionType.LOCATION) {
            ((MainActivity) requireActivity()).ensureLocationPermission(ok -> {
                services().consents.record(t, ok);
                if (!ok) {
                    toast("안드로이드 설정에서 위치 권한을 허용해 주세요.");
                }
                renderConsents();
            });
            return;
        }
        // 철회 시 해당 정보의 신규 수집과 기능 사용을 중단(각 기능이 동의 여부를 확인)
        services().consents.record(t, grant);
        renderConsents();
    }
}
