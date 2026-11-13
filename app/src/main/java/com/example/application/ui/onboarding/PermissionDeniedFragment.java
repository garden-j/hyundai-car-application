package com.example.application.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.databinding.ContentScrollBinding;
import com.example.application.model.PermissionType;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;

/** 0-1-1 일부 기능은 사용이 불가능해요 — 거부한 권한별 제한 기능과 대안 안내. */
public class PermissionDeniedFragment extends BaseStepFragment {
    private static final String ARG = "denied";

    public static PermissionDeniedFragment newInstance(ArrayList<String> denied) {
        PermissionDeniedFragment f = new PermissionDeniedFragment();
        Bundle b = new Bundle();
        b.putStringArrayList(ARG, denied);
        f.setArguments(b);
        return f;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("일부 기능은\n사용할 수 없어요", "나중에 설정에서 켤 수 있어요.");
        showNav(true, false);
        setPrimary("이대로 시작", () -> nav().push(new ExperienceFragment()));
        setSecondary("다시 선택", () -> nav().back());
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        ArrayList<String> denied = requireArguments().getStringArrayList(ARG);
        int i = 1;
        for (String name : denied) {
            PermissionType t = PermissionType.valueOf(name);
            Rows.add(list, String.valueOf(i++), t.label() + " 미동의", t.limitationWhenDenied, null);
        }
        return null;
    }
}
