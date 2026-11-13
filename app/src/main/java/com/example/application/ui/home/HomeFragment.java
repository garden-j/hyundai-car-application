package com.example.application.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.MainActivity;
import com.example.application.core.Callback;
import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.databinding.FragmentHomeBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.ChecklistEvaluator;
import com.example.application.domain.SetupFlow;
import com.example.application.domain.TodayRecommendation;
import com.example.application.model.PermissionType;
import com.example.application.ui.settings.SettingsFragment;
import com.example.application.ui.setup.SetupViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 홈 — 오늘의 추천 연습, 일반 주행, 오늘의 운전 지식, 차량 상태·연료. */
public class HomeFragment extends Fragment {
    private static final String[] TIPS = {
            "비보호 좌회전은\n맞은편 차가 끊길 때 해요",
            "차로를 바꾸기 전엔\n방향지시등을 3초 먼저 켜요",
            "합류할 땐 본선 차와\n속도를 맞춰 들어가요",
            "유턴은 1차로에서\n유턴 표지를 보고 해요",
            "비 오는 날엔 앞차와의 거리를\n평소의 두 배로 둬요"
    };

    private FragmentHomeBinding b;
    private TodayRecommendation recommendation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        ServiceLocator sl = ServiceLocator.get();

        b.date.setText(new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(new Date()));
        recommendation = TodayRecommendation.from(sl.profiles.get());
        b.recTitle.setText(recommendation.title);
        b.recMeta.setText(recommendation.meta);
        b.recReason.setText(recommendation.reason);
        b.tip.setText(TIPS[(int) ((System.currentTimeMillis() / 86_400_000L) % TIPS.length)]);

        b.btnPractice.setOnClickListener(v -> startPractice());
        b.btnDrive.setOnClickListener(v -> soon("일반 주행"));
        b.navHome.setOnClickListener(v -> {
        });
        b.navKnowledge.setOnClickListener(v -> soon("운전 지식"));
        b.navHelp.setOnClickListener(v -> soon("차량 도움"));
        b.navSettings.setOnClickListener(v -> ((MainActivity) requireActivity()).navigator().push(new SettingsFragment()));

        loadVehicle(sl);
        return b.getRoot();
    }

    private void startPractice() {
        SetupViewModel vm = new ViewModelProvider(requireActivity()).get(SetupViewModel.class);
        vm.start(recommendation);
        ((MainActivity) requireActivity()).navigator().openSetupStep(SetupFlow.first());
    }

    private void loadVehicle(ServiceLocator sl) {
        String connection = sl.vehicle.isSimulated() ? "가상 차량(시뮬레이션)" : "차량 연결됨";
        b.vehicleConnection.setText(connection);
        if (!sl.consents.isGranted(PermissionType.VEHICLE)) {
            b.vehicleStatus.setText("확인 안 함");
            b.vehicleConnection.setText("차량 정보 사용에 동의하지 않았어요");
            b.fuel.setText("--");
            b.cabinTemp.setText("");
            return;
        }
        sl.vehicle.readSettings(new Callback<VehicleSettings>() {
            @Override
            public void onSuccess(VehicleSettings s) {
                if (b != null) {
                    b.cabinTemp.setText(s.cabinTemperature == null ? ""
                            : String.format(Locale.KOREA, "차량 내부 %.0f℃", s.cabinTemperature));
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
        sl.vehicle.readReadiness(new Callback<VehicleReadiness>() {
            @Override
            public void onSuccess(VehicleReadiness r) {
                if (b == null) {
                    return;
                }
                boolean doorsOpen = Boolean.FALSE.equals(r.allDoorsClosed);
                b.vehicleStatus.setText(doorsOpen ? "문 열림" : "이상 없음");
                Integer pct = r.fuelPercent != null ? r.fuelPercent
                        : r.evBatteryLevel != null && r.evBatteryLevel <= 100 ? Math.round(r.evBatteryLevel) : null;
                String energy = r.fuelPercent != null ? "연료" : "배터리";
                b.fuel.setText(r.rangeKm != null ? Math.round(r.rangeKm) + " km" : pct != null ? energy + " " + pct + "%" : "--");
                b.fuelNote.setText(pct == null ? (r.rangeKm != null ? "" : "확인할 수 없어요")
                        : energy + " " + pct + "% · " + (pct < ChecklistEvaluator.LOW_FUEL_PERCENT
                        ? (r.fuelPercent != null ? "주유가 필요해요" : "충전이 필요해요") : "충분해요"));
            }

            @Override
            public void onError(Exception e) {
                if (b != null) {
                    b.vehicleStatus.setText("확인 불가");
                }
            }
        });
    }

    private void soon(String what) {
        Toast.makeText(requireContext(), what + " 기능은 다음 단계에서 연결돼요.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
