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

    protected PracticeSetup setup() {
        return setupVm().setup();
    }

    protected void goNext() {
        if (!check(SetupFlow.validate(step(), setup()))) {
            return;
        }
        services().tts.stop();
        nav().openSetupStep(SetupFlow.next(step(), setup()));
    }

    /** 오른쪽에 세로 스크롤 목록을 만들고 그 안의 LinearLayout을 돌려준다. */
    protected LinearLayout scrollList(LayoutInflater inflater) {
        ContentScrollBinding b = ContentScrollBinding.inflate(inflater, frame.content, true);
        return b.list;
    }
}
