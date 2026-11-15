package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.example.application.databinding.ContentScrollBinding;
import com.example.application.domain.SetupFlow;
import com.example.application.domain.SetupStep;
import com.example.application.model.PracticeSetup;
import com.example.application.ui.common.BaseStepFragment;

/** 출발 전 설정 단계 화면 공통: 단계 검증 후 다음 단계로 이동. */
public abstract class BaseSetupFragment extends BaseStepFragment {
    protected abstract SetupStep step();

    @Override
    public void onViewCreated(@androidx.annotation.NonNull android.view.View view,
                              @androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (setup().generalDrive) {
            setSection("일반 주행");
        }
        showStepProgress();
    }

    /** 앱 바 오른쪽 "n / 전체": 고른 목표에 따라 실제로 거치는 단계만 센다(출발 화면 제외). */
    private void showStepProgress() {
        java.util.List<SetupStep> path = new java.util.ArrayList<>();
        SetupStep s = SetupFlow.first(setup());
        for (int guard = 0; s != null && s != SetupStep.DEPART && guard < 20; guard++) {
            path.add(s);
            s = SetupFlow.next(s, setup());
        }
        int index = path.indexOf(step());
        if (index >= 0) {
            setProgress(index + 1, path.size());
        }
    }

    protected PracticeSetup setup() {
        return setupVm().setup();
    }

    protected void goNext() {
        if (!check(SetupFlow.validate(step(), setup()))) {
            return;
        }
        services().tts.stop();
        if (setup().generalDrive && step() == SetupStep.DESTINATION) {
            setupVm().prepareGeneralDriveRoute();
        }
        nav().openSetupStep(SetupFlow.next(step(), setup()));
    }

    /** 오른쪽에 세로 스크롤 목록을 만들고 그 안의 LinearLayout을 돌려준다. */
    protected LinearLayout scrollList(LayoutInflater inflater) {
        ContentScrollBinding b = ContentScrollBinding.inflate(inflater, frame.content, true);
        return b.list;
    }
}
