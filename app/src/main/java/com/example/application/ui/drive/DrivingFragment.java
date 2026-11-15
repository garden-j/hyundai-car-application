package com.example.application.ui.drive;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.MainActivity;
import com.example.application.core.drive.DriveSession;
import com.example.application.core.pleos.navi.DriveGuideService;
import com.example.application.databinding.FragmentDrivingBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.DriveGuide;
import com.example.application.domain.EnvironmentWatch;
import com.example.application.ui.setup.SetupViewModel;

import java.util.Locale;

/**
 * 주행 중 화면(Figma L/19). 주행 안내 서비스가 갱신하는 {@link DriveSession}을 보여 주고,
 * 긴장 완화·휴식·주유/충전·주행 마치기로 이어진다. 화면 조작보다 음성 안내가 중심이다.
 */
public class DrivingFragment extends Fragment {
    private FragmentDrivingBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentDrivingBinding.inflate(inflater, container, false);
        b.btnRelax.setOnClickListener(v -> nav().push(new RelaxFragment()));
        b.btnRest.setOnClickListener(v -> nav().push(new RestFragment()));
        b.btnEnergy.setOnClickListener(v -> nav().push(new EnergyFragment()));
        b.btnFinish.setOnClickListener(v -> confirmFinish());
        DriveSession.get().state().observe(getViewLifecycleOwner(), this::render);
        return b.getRoot();
    }

    private void render(DriveSession.State s) {
        if (s == null || b == null) {
            return;
        }
        b.mode.setText(s.modeLabel);
        boolean turn = s.maneuver == DriveGuide.Maneuver.LEFT || s.maneuver == DriveGuide.Maneuver.RIGHT
                || s.maneuver == DriveGuide.Maneuver.KEEP_LEFT || s.maneuver == DriveGuide.Maneuver.KEEP_RIGHT;
        b.turnBox.setVisibility(turn ? View.VISIBLE : View.GONE);
        boolean left = s.maneuver == DriveGuide.Maneuver.LEFT || s.maneuver == DriveGuide.Maneuver.KEEP_LEFT;
        b.turnIcon.setScaleX(left ? -1f : 1f);
        b.distance.setVisibility(s.distanceM >= 0 ? View.VISIBLE : View.GONE);
        b.distance.setText(formatDistance(s.distanceM));
        b.headline.setText(s.headline);
        b.tip.setText(s.tip);
        b.tip.setVisibility(s.tip == null ? View.GONE : View.VISIBLE);
        b.lastSpoken.setText(s.lastSpoken == null ? "다음 회전이 가까워지면 알려 드릴게요" : "“" + s.lastSpoken + "”");
        b.btnFinish.setText(s.arrived ? "주행 마치기" : "주행 그만하기");

        if (s.procedure != null) {
            b.alert.setVisibility(View.VISIBLE);
            b.alertText.setText(String.join("\n", numbered(s.procedure)));
            b.alertAction.setText("다시 듣기");
            b.alertAction.setOnClickListener(v -> ServiceLocator.get().tts.speakAll(s.procedure));
        } else if (s.energyAlert != null) {
            b.alert.setVisibility(View.VISIBLE);
            b.alertText.setText(s.energyAlert);
            b.alertAction.setText(DriveSession.get().isEv() ? "충전소 찾기" : "주유소 찾기");
            b.alertAction.setOnClickListener(v -> nav().push(new EnergyFragment()));
        } else if (s.envChange != null) {
            b.alert.setVisibility(View.VISIBLE);
            b.alertText.setText(String.format("%s · 초보자에게 편한 길로 바꿀 수 있어요", EnvironmentWatch.label(s.envChange)));
            b.alertAction.setText("편안한 길로 바꾸기");
            b.alertAction.setOnClickListener(v -> {
                ServiceLocator.get().navi.useComfortableRoute();
                ServiceLocator.get().tts.speak("편안한 길로 다시 찾을게요.");
                DriveSession.get().update(st -> st.envChange = null);
            });
        } else {
            b.alert.setVisibility(View.GONE);
        }
    }

    private static java.util.List<String> numbered(java.util.List<String> lines) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            out.add((i + 1) + ". " + lines.get(i));
        }
        return out;
    }

    static String formatDistance(double m) {
        if (m < 0) {
            return "";
        }
        return m >= 1000 ? String.format(Locale.KOREA, "%.1fkm", m / 1000) : Math.round(m / 10) * 10 + "m";
    }

    private void confirmFinish() {
        if (DriveSession.get().current().arrived) {
            finishDrive(requireActivity());
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("주행을 그만할까요?")
                .setMessage("내비게이션 안내도 함께 끝나요.")
                .setPositiveButton("그만하기", (d, w) -> finishDrive(requireActivity()))
                .setNegativeButton("계속 갈게요", null)
                .show();
    }

    /** 주행을 마친다: 안내 서비스·내비 안내 종료 → 격려 화면. */
    static void finishDrive(androidx.fragment.app.FragmentActivity activity) {
        ServiceLocator sl = ServiceLocator.get();
        DriveGuideService.stop(activity);
        sl.navi.cancelRoute();
        SetupViewModel vm = new ViewModelProvider(activity).get(SetupViewModel.class);
        vm.setup().naviStarted = false;
        ((MainActivity) activity).navigator().push(new EncourageFragment());
    }

    private com.example.application.ui.Navigator nav() {
        return ((MainActivity) requireActivity()).navigator();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
