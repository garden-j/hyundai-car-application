package com.example.application.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.MainActivity;
import com.example.application.core.pleos.voice.SttGateway;
import com.example.application.databinding.FragmentStepBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.PermissionType;
import com.example.application.ui.Navigator;
import com.example.application.ui.setup.SetupViewModel;

/**
 * 단계 화면 공통 틀. 하위 클래스는 {@link #onCreateContent}에서 오른쪽 내용만 만들고,
 * 제목·버튼·음성 답변은 이 클래스의 도우미로 설정한다.
 */
public abstract class BaseStepFragment extends Fragment {
    protected FragmentStepBinding frame;
    private String voiceHint;

    protected abstract View onCreateContent(LayoutInflater inflater, ViewGroup container);

    /** 음성 답변 처리. 처리했으면 true. "다음"/"이전"은 공통 처리된다. */
    protected boolean onVoiceText(String text) {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        frame = FragmentStepBinding.inflate(inflater, container, false);
        frame.btnBack.setOnClickListener(v -> nav().back());
        frame.btnHome.setOnClickListener(v -> nav().home());
        frame.sectionTitle.setText(defaultSection());
        if (getResources().getConfiguration().screenWidthDp < WIDE_MIN_DP) {
            stackFooter();
        }
        View content = onCreateContent(inflater, frame.content);
        if (content != null && content.getParent() == null) {
            frame.content.addView(content);
        }
        return frame.getRoot();
    }

    @Override
    public void onDestroyView() {
        ServiceLocator.get().stt.stop();
        super.onDestroyView();
        frame = null;
    }

    /** 이 폭 이상이면 가로(L) 배치, 미만이면 세로(P) 배치. 앱 영역은 가로 1682dp, 분할 시 약 840dp. */
    static final int WIDE_MIN_DP = 1100;

    /** 앱 바 제목(구역 이름). 화면 묶음별 기본값이며 setSection으로 바꿀 수 있다. */
    private String defaultSection() {
        String pkg = getClass().getName();
        if (pkg.contains(".onboarding.")) {
            return "시작하기";
        } else if (pkg.contains(".setup.")) {
            return "연습 설정";
        } else if (pkg.contains(".drive.")) {
            return "주행";
        }
        return "";
    }

    /** 세로(P) 배치: 음성 안내를 위에, 버튼을 아래 전체 폭으로 쌓는다. */
    private void stackFooter() {
        frame.footer.setOrientation(LinearLayout.VERTICAL);
        frame.footer.setGravity(android.view.Gravity.NO_GRAVITY);
        LinearLayout.LayoutParams voice = (LinearLayout.LayoutParams) frame.voiceArea.getLayoutParams();
        voice.width = ViewGroup.LayoutParams.MATCH_PARENT;
        voice.weight = 0;
        voice.bottomMargin = Choices.dp(requireContext(), 16);
        LinearLayout.LayoutParams actions = (LinearLayout.LayoutParams) frame.actions.getLayoutParams();
        actions.width = ViewGroup.LayoutParams.MATCH_PARENT;
        actions.setMarginStart(0);
        for (View b : new View[]{frame.btnSecondary, frame.btnPrimary}) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.getLayoutParams();
            lp.width = 0;
            lp.weight = b == frame.btnPrimary ? 3 : 2;
        }
        frame.voiceArea.setVisibility(View.GONE);
    }

    // ---- 화면 설정 도우미 ----

    protected void setSection(String section) {
        frame.sectionTitle.setText(section);
    }

    protected void setTitle(String title, String subtitle) {
        // 새 디자인은 제목을 한 줄로 쓴다(좁으면 자동 줄바꿈)
        frame.title.setText(title == null ? null : title.replace("\n", " "));
        frame.subtitle.setText(subtitle);
        frame.subtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
    }

    protected void setSubtitle(String subtitle) {
        frame.subtitle.setText(subtitle);
        frame.subtitle.setVisibility(View.VISIBLE);
    }

    protected void setProgress(int step, int total) {
        frame.progress.setVisibility(View.VISIBLE);
        frame.progress.setText(step + " / " + total);
    }

    protected void showNav(boolean back, boolean home) {
        frame.btnBack.setVisibility(back ? View.VISIBLE : View.GONE);
        frame.btnHome.setVisibility(home ? View.VISIBLE : View.GONE);
    }

    protected void setPrimary(String text, Runnable action) {
        frame.btnPrimary.setText(text);
        frame.btnPrimary.setOnClickListener(v -> action.run());
    }

    protected void setPrimaryEnabled(boolean enabled) {
        frame.btnPrimary.setEnabled(enabled);
    }

    protected void setSecondary(String text, Runnable action) {
        frame.btnSecondary.setVisibility(text == null ? View.GONE : View.VISIBLE);
        frame.btnSecondary.setText(text);
        frame.btnSecondary.setOnClickListener(action == null ? null : v -> action.run());
    }

    /** "말로 답해도 돼요" 영역을 켠다. example은 따옴표 안 예시 문장. */
    protected void enableVoice(String example) {
        voiceHint = "말로 답해도 돼요: “" + example + "”";
        frame.voiceArea.setVisibility(View.VISIBLE);
        frame.voiceHint.setText(voiceHint);
        frame.btnMic.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        ServiceLocator sl = ServiceLocator.get();
        if (!sl.consents.isGranted(PermissionType.MICROPHONE)) {
            toast("마이크 사용에 동의하지 않았어요. 설정에서 켤 수 있어요.");
            return;
        }
        ((MainActivity) requireActivity()).ensureMicPermission(granted -> {
            if (!granted || frame == null) {
                toast("마이크 권한이 필요해요.");
                return;
            }
            sl.tts.stop();
            sl.stt.listen(new SttGateway.Listener() {
                @Override
                public void onListeningChanged(boolean listening) {
                    if (frame == null) {
                        return;
                    }
                    frame.btnMic.setActivated(listening);
                    if (listening) {
                        frame.voiceHint.setText("듣고 있어요…");
                    }
                }

                @Override
                public void onPartial(String text) {
                    if (frame != null) {
                        frame.voiceHint.setText("“" + text + "”");
                    }
                }

                @Override
                public void onResult(String text) {
                    if (frame == null) {
                        return;
                    }
                    frame.voiceHint.setText("“" + text + "”");
                    handleVoice(text);
                }

                @Override
                public void onError(String message) {
                    if (frame == null) {
                        return;
                    }
                    frame.btnMic.setActivated(false);
                    frame.voiceHint.setText(voiceHint);
                    toast(message);
                }
            });
        });
    }

    private void handleVoice(String text) {
        if (onVoiceText(text)) {
            return;
        }
        if (VoiceCommandParser.isBack(text)) {
            nav().back();
        } else if (VoiceCommandParser.isNext(text)) {
            frame.btnPrimary.performClick();
        } else {
            speak("잘 못 알아들었어요. 화면을 눌러 골라 주셔도 돼요.");
        }
    }

    // ---- 공통 도우미 ----

    protected Navigator nav() {
        return ((MainActivity) requireActivity()).navigator();
    }

    protected SetupViewModel setupVm() {
        return new ViewModelProvider(requireActivity()).get(SetupViewModel.class);
    }

    protected ServiceLocator services() {
        return ServiceLocator.get();
    }

    protected void speak(String text) {
        services().tts.speak(text);
    }

    protected void toast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
    }

    /** 검증 실패 문구를 화면과 음성으로 알린다. 통과하면 true. */
    protected boolean check(String validationMessage) {
        if (validationMessage == null) {
            return true;
        }
        toast(validationMessage);
        speak(validationMessage);
        return false;
    }
}
