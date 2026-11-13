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
        setTitle("준비 끝!\n출발해요", "안전하게 천천히 출발하세요.");
        showNav(false, false);
        setPrimary("연습 종료", this::finishPractice);
        setSecondary("홈으로", () -> nav().home());

        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        if (s.selectedRoute != null) {
            Rows.add(list, null, "경로", s.selectedRoute.title + " · " + s.selectedRoute.reason,
                    String.format(Locale.KOREA, "%d분 · %.1fkm", s.selectedRoute.minutes, s.selectedRoute.distanceKm));
        }
        Rows.add(list, null, "난이도", s.isHighTension() ? "긴장도를 반영해 한 단계 낮췄어요" : null,
                s.effectiveDifficulty().label());
        Rows.add(list, null, "안내 수준", s.isHighTension() ? "긴장도를 반영해 한 단계 높였어요" : null, guidance.label());
        if (s.preset != null) {
            Rows.add(list, null, "연습 모드", s.preset.description(), s.preset.label());
        }
        PracticeSession session = services().sessions.active();
        Rows.add(list, null, "연습 전 차량 설정",
                session != null && session.settingsBefore != null ? "연습이 끝나면 원래대로 돌릴 수 있어요" : "저장된 설정 없음",
                null);
        speak("준비가 끝났어요. 천천히 출발해 볼까요?");
        return null;
    }

    private void finishPractice() {
        // 3단계: 도착·격려 → 연습 전·현재 설정 비교 → 복원 흐름으로 교체 예정
        services().sessions.clear();
        nav().home();
    }
}
