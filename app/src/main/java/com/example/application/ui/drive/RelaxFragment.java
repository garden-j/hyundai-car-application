package com.example.application.ui.drive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.application.MainActivity;
import com.example.application.core.Callback;
import com.example.application.core.drive.DriveSession;
import com.example.application.core.pleos.vehicle.VehicleStatus;
import com.example.application.databinding.FragmentDriveMessageBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.RelaxGuide;

import java.util.List;

/**
 * Relax 안내(Figma L/20). 주행 중에는 짧고 차분한 말만, 기어가 P면 호흡 안내까지 한다.
 * Relax 안내 수준에 따라 길이가 달라진다.
 */
public class RelaxFragment extends Fragment {
    private FragmentDriveMessageBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentDriveMessageBinding.inflate(inflater, container, false);
        DriveSession session = DriveSession.get();
        session.stats().relaxRequests++;
        b.mode.setText(session.current().modeLabel);
        b.message.setText("숨을 길게\n내쉬어 볼까요");
        b.sub.setText("천천히 가도 괜찮아요. 제가 옆에서 안내할게요.");
        b.voiceStatus.setText("음성으로 안내하고 있어요");
        b.btnSecondary.setText("쉬어 갈래요");
        b.btnSecondary.setOnClickListener(v -> nav().push(new RestFragment()));
        b.btnPrimary.setText("괜찮아졌어요");
        b.btnPrimary.setOnClickListener(v -> {
            ServiceLocator.get().tts.stop();
            nav().back();
        });
        ServiceLocator.get().vehicle.readStatus(new Callback<VehicleStatus>() {
            @Override
            public void onSuccess(VehicleStatus st) {
                start(st.isParked());
            }

            @Override
            public void onError(Exception e) {
                start(false);
            }
        });
        return b.getRoot();
    }

    private void start(boolean parked) {
        if (b == null) {
            return;
        }
        List<String> lines = RelaxGuide.calm(DriveSession.get().relaxLevel(), parked);
        if (parked) {
            b.message.setText("같이 숨을\n골라 볼까요");
            b.sub.setText("차가 멈춰 있어요. 코로 들이쉬고 입으로 길게 내쉬어요.");
        }
        b.voiceLine.setText("“" + lines.get(0) + "”");
        ServiceLocator.get().tts.speakAll(lines);
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
