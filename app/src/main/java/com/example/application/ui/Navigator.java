package com.example.application.ui;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.application.domain.SetupStep;
import com.example.application.ui.home.HomeFragment;
import com.example.application.ui.setup.SetupScreens;

/** 단일 Activity 안의 Fragment 화면 전환. */
public class Navigator {
    private final FragmentManager fm;
    private final int containerId;

    public Navigator(FragmentManager fm, int containerId) {
        this.fm = fm;
        this.containerId = containerId;
    }

    /** 뒤로 가기 기록을 지우고 루트 화면으로 바꾼다. */
    public void setRoot(Fragment fragment) {
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction().replace(containerId, fragment).commit();
    }

    public void push(Fragment fragment) {
        fm.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    public void back() {
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }

    public void home() {
        setRoot(new HomeFragment());
    }

    public void openSetupStep(SetupStep step) {
        push(SetupScreens.create(step));
    }
}
