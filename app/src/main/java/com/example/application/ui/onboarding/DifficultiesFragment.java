package com.example.application.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.databinding.ContentScrollBinding;
import com.example.application.model.DifficultSituation;
import com.example.application.model.DriverProfile;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;

import java.util.Arrays;

/** 0-3 평소 어려움이 있었던 부분을 알려주세요 — 어려운 상황 복수 선택. 오늘의 추천에 사용. */
public class DifficultiesFragment extends BaseStepFragment {
    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("평소 어려움이 있었던\n부분을 알려 주세요", "운전할 때 힘들었던 부분을 모두 골라 주세요.");
        setProgress(3, 4);
        showNav(true, false);
        DriverProfile p = services().profiles.get();
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        Choices.addChipSection(list, "어려운 상황", Arrays.asList(DifficultSituation.values()), p.difficulties,
                false, null);
        setPrimary("다음", () -> {
            services().profiles.save(p);
            nav().push(new GuidanceLevelFragment());
        });
        return null;
    }
}
