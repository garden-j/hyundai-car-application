package com.example.application.ui.depart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.data.PracticeSession;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.model.GuidanceLevel;
import com.example.application.model.PracticeSetup;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Rows;

import java.util.Locale;

/**
 * 출발 — 출발 전 설정 결과 요약. 주행 중 화면(안내 엔진·Relax·환경 변화)은 3단계에서 이 자리에 들어간다.
 */
public class DepartFragment extends BaseStepFragment {
    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        PracticeSetup s = setupVm().setup();
        GuidanceLevel guidance = s.effectiveGuidance(services().profiles.get().guidanceLevel);
        setTitle("준비 끝! 출발해요", "주행 시작을 누르면 내비게이션 안내가 시작돼요.");
        showNav(true, true);
        setPrimary("주행 시작", this::startDriving);
        setSecondary("홈으로", () -> nav().home());
        setupVm().naviRequest().observe(getViewLifecycleOwner(), this::onNaviResult);

        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        if (s.selectedRoute != null) {
            String time = s.selectedRoute.minutes <= 0 ? null
                    : String.format(Locale.KOREA, s.generalDrive ? "약 %d분 · %.1fkm" : "%d분 · %.1fkm",
                    s.selectedRoute.minutes, s.selectedRoute.distanceKm);
            Rows.add(list, null, s.generalDrive ? "목적지" : "경로",
                    s.selectedRoute.title + " · " + s.selectedRoute.reason, time);
        }
        if (!s.generalDrive) {
            Rows.add(list, null, "난이도", s.isHighTension() ? "긴장도를 반영해 한 단계 낮췄어요" : null,
                    s.effectiveDifficulty().label());
        }
        Rows.add(list, null, "안내 수준", s.isHighTension() ? "긴장도를 반영해 한 단계 높였어요" : null, guidance.label());
        if (s.preset != null) {
            Rows.add(list, null, s.generalDrive ? "차량 환경" : "연습 모드", s.preset.description(), s.preset.label());
        }
        PracticeSession session = services().sessions.active();
        String word = s.generalDrive ? "주행" : "연습";
        Rows.add(list, null, word + " 전 차량 설정",
                session != null && session.settingsBefore != null ? word + "이 끝나면 원래대로 돌릴 수 있어요" : "저장된 설정 없음",
                null);
        if (s.naviStarted) {
            showStarted();
        } else {
            speak("준비가 끝났어요. 주행 시작을 누르면 내비게이션 안내가 시작돼요.");
        }
        return null;
    }

    /** 모든 설정을 마친 뒤에만 내비게이션에 경로를 보내 안내를 시작한다. */
    private void startDriving() {
        PracticeSetup s = setupVm().setup();
        if (s.selectedRoute == null) {
            toast("경로를 먼저 골라 주세요.");
            return;
        }
        // 앱이 앞에 있을 때 시작: 내비가 앞으로 나와도 설정한 안내 수준으로 음성 안내를 이어 간다
        com.example.application.model.DriverProfile profile = services().profiles.get();
        com.example.application.core.pleos.navi.DriveGuideService.start(requireContext(),
                s.generalDrive ? "일반 주행" : "연습 모드",
                s.effectiveGuidance(profile.guidanceLevel),
                s.isHighTension() ? profile.relaxLevelOrDefault().raised() : profile.relaxLevelOrDefault(),
                profile.encourageLevelOrDefault());
        setupVm().sendRouteToNavi();
    }

    private void onNaviResult(com.example.application.ui.common.Resource<Void> res) {
        if (res == null) {
            return;
        }
        switch (res.state) {
            case LOADING:
                setPrimaryEnabled(false);
                setSubtitle("내비게이션에 경로를 보내고 있어요…");
                break;
            case SUCCESS:
                setupVm().clearNaviRequest();
                setPrimaryEnabled(true);
                setupVm().setup().naviStarted = true;
                showStarted();
                speak("내비게이션 안내를 시작할게요. 천천히 출발하세요.");
                nav().push(new com.example.application.ui.drive.DrivingFragment());
                break;
            case ERROR:
                com.example.application.core.pleos.navi.DriveGuideService.stop(requireContext());
                setupVm().clearNaviRequest();
                setPrimaryEnabled(true);
                setSubtitle("주행 시작을 누르면 내비게이션 안내가 시작돼요.");
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("내비게이션에 경로를 보내지 못했어요")
                        .setMessage(res.error + "\n내비게이션에서 목적지를 직접 설정할 수도 있어요.")
                        .setPositiveButton("다시 시도", (d, w) -> startDriving())
                        .setNegativeButton("닫기", null)
                        .show();
                break;
        }
    }

    /** 안내 시작 후: 연습을 마칠 때 누르는 버튼으로 바꾼다. */
    private void showStarted() {
        String word = setupVm().setup().generalDrive ? "주행" : "연습";
        setSubtitle("내비게이션 안내 중이에요. " + word + "을 마치면 " + word + " 종료를 눌러 주세요.");
        setPrimary("주행 화면 보기", () -> nav().push(new com.example.application.ui.drive.DrivingFragment()));
        setSecondary("내비게이션 다시 열기", this::startDriving);
    }

    private void finishPractice() {
        setupVm().setup().naviStarted = false;
        com.example.application.core.pleos.navi.DriveGuideService.stop(requireContext());
        services().navi.cancelRoute();
        // 3단계: 도착·격려 → 연습 전·현재 설정 비교 → 복원 흐름으로 교체 예정
        services().sessions.clear();
        nav().home();
    }
}
