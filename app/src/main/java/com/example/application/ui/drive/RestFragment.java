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
import com.example.application.core.MainThread;
import com.example.application.core.drive.DriveSession;
import com.example.application.core.pleos.voice.SttGateway;
import com.example.application.databinding.FragmentDriveMessageBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.RelaxGuide;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.GeoPoint;
import com.example.application.model.Place;

import java.util.List;

/**
 * 휴식 권유(Figma L/21 음성 확인 질문). "네"면 가까운 주차장을 경유지로 넣고,
 * 답이 없으면 지금 경로를 유지한다.
 */
public class RestFragment extends Fragment {
    private static final long NO_ANSWER_MS = 12_000;
    private FragmentDriveMessageBinding b;
    private final Runnable keepRoute = this::keepRoute;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentDriveMessageBinding.inflate(inflater, container, false);
        b.mode.setText(DriveSession.get().current().modeLabel);
        b.message.setText("잠시\n쉬어갈까요?");
        b.sub.setText("“네”     “아니요”");
        b.voiceStatus.setText("듣고 있어요");
        b.voiceLine.setText("답이 없으면 지금 경로를 유지해요");
        b.btnSecondary.setText("아니요");
        b.btnSecondary.setOnClickListener(v -> keepRoute());
        b.btnPrimary.setText("네, 쉬어 갈래요");
        b.btnPrimary.setOnClickListener(v -> findRestPlace());
        b.btnMic.setOnClickListener(v -> listen());
        ServiceLocator.get().tts.speak(RelaxGuide.askRest());
        MainThread.postDelayed(keepRoute, NO_ANSWER_MS);
        MainThread.postDelayed(this::listen, 3_500);
        return b.getRoot();
    }

    private void listen() {
        if (b == null) {
            return;
        }
        ServiceLocator.get().stt.listen(new SttGateway.Listener() {
            @Override
            public void onListeningChanged(boolean listening) {
            }

            @Override
            public void onPartial(String text) {
            }

            @Override
            public void onResult(String text) {
                String n = VoiceCommandParser.normalize(text);
                if (n.contains("아니") || n.contains("괜찮") || n.contains("계속")) {
                    keepRoute();
                } else if (n.contains("네") || n.contains("응") || n.contains("쉬") || n.contains("좋아")) {
                    findRestPlace();
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void keepRoute() {
        MainThread.cancel(keepRoute);
        if (b == null || !isAdded()) {
            return;
        }
        ServiceLocator.get().tts.speak("알겠어요. 지금 경로로 계속 갈게요.");
        nav().back();
    }

    private void findRestPlace() {
        MainThread.cancel(keepRoute);
        if (b == null) {
            return;
        }
        b.voiceStatus.setText("근처 주차장을 찾고 있어요");
        ServiceLocator sl = ServiceLocator.get();
        sl.location.lastKnownLocation(new Callback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint here) {
                search(here);
            }

            @Override
            public void onError(Exception e) {
                search(null);
            }
        });
    }

    private void search(GeoPoint near) {
        ServiceLocator sl = ServiceLocator.get();
        sl.places.search("주차장", near, Callback.success(found -> {
            Place place = nearest(found, near);
            if (place == null) {
                sl.tts.speak("근처 주차장을 찾지 못했어요. 비상등을 켜고 안전한 곳에 세워 쉬어요.");
                if (isAdded()) {
                    nav().back();
                }
                return;
            }
            sl.navi.addWaypoint(place, new Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    DriveSession.get().setWaypointPurpose(DriveSession.WaypointPurpose.REST);
                    DriveSession.get().stats().rests++;
                    sl.tts.speak(RelaxGuide.restAdded(place.name));
                    if (isAdded()) {
                        nav().back();
                    }
                }

                @Override
                public void onError(Exception e) {
                    sl.tts.speak("경유지를 넣지 못했어요. 비상등을 켜고 안전한 곳에 세워 쉬어요.");
                }
            });
        }));
    }

    /** 이름에 '주차'가 들어간 결과 중 가장 가까운 곳. */
    static Place nearest(List<Place> places, GeoPoint near) {
        Place best = null;
        double bestKm = Double.MAX_VALUE;
        for (Place p : places) {
            if (p.point == null || !p.name.contains("주차")) {
                continue;
            }
            double km = near == null ? 0 : near.distanceKmTo(p.point);
            if (km < bestKm && km < 5) {
                best = p;
                bestKm = km;
            }
        }
        return best;
    }

    private com.example.application.ui.Navigator nav() {
        return ((MainActivity) requireActivity()).navigator();
    }

    @Override
    public void onDestroyView() {
        MainThread.cancel(keepRoute);
        ServiceLocator.get().stt.stop();
        super.onDestroyView();
        b = null;
    }
}
