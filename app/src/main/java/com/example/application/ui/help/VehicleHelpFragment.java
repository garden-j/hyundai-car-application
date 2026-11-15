package com.example.application.ui.help;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.core.Callback;
import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.core.pleos.vehicle.VehicleStatus;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.databinding.ItemRowBinding;
import com.example.application.domain.StartDiagnosis;
import com.example.application.domain.VehicleFaq;
import com.example.application.model.PermissionType;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Rows;
import com.example.application.ui.common.SpeakingRows;

import java.util.List;
import java.util.Locale;

/**
 * 차량 도움: 차량 상태 한눈에 · 출발이 안 될 때 점검(조작 실패 원인 확인) · 자주 묻는 기능 안내.
 * 기능 질문은 음성으로 물으면 정리된 안내에서 찾아 읽어 준다(LLM 없이 키워드 검색).
 */
public class VehicleHelpFragment extends BaseStepFragment {
    private LinearLayout statusList;
    private LinearLayout diagnosisList;
    private SpeakingRows speaking;

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setSection("차량 도움");
        setTitle("차량에 대해 물어보세요", "\"와이퍼 어떻게 켜?\"처럼 말하면 찾아서 알려 드려요.");
        showNav(true, false);
        enableVoice("와이퍼 어떻게 켜?");
        setSecondary("출발이 안 돼요", this::diagnose);
        setPrimary("홈으로", () -> nav().home());
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        speaking = new SpeakingRows(services().tts);

        list.addView(Choices.sectionTitle(requireContext(), "차량 상태"));
        statusList = new LinearLayout(requireContext());
        statusList.setOrientation(LinearLayout.VERTICAL);
        list.addView(statusList);
        diagnosisList = new LinearLayout(requireContext());
        diagnosisList.setOrientation(LinearLayout.VERTICAL);
        list.addView(diagnosisList);
        list.addView(Choices.sectionTitle(requireContext(), "자주 묻는 기능"));
        LinearLayout faq = new LinearLayout(requireContext());
        faq.setOrientation(LinearLayout.VERTICAL);
        list.addView(faq);
        for (VehicleFaq.Entry e : VehicleFaq.all()) {
            ItemRowBinding row = Rows.add(faq, null, e.title, null, SpeakingRows.IDLE);
            row.getRoot().setOnClickListener(v -> speaking.toggle(row, e.title, e.answer, e.answer));
        }
        loadStatus();
        return null;
    }

    private void loadStatus() {
        if (!services().consents.isGranted(PermissionType.VEHICLE)) {
            Rows.add(statusList, null, "차량 정보 사용에 동의하지 않았어요", "설정에서 동의하면 차량 상태를 보여 드려요.", null);
            return;
        }
        services().vehicle.readStatus(new Callback<VehicleStatus>() {
            @Override
            public void onSuccess(VehicleStatus st) {
                if (!isAdded()) {
                    return;
                }
                Rows.add(statusList, null, "경고등", null, st.warnings.isEmpty() ? "켜진 경고등 없음" : st.warnings.size() + "개 켜짐");
                Rows.add(statusList, null, "문", null, st.allDoorsClosed == null ? "확인 불가" : st.allDoorsClosed ? "모두 닫힘" : "열린 문 있음");
                Rows.add(statusList, null, "엔진오일", null, level(st.engineOil));
                Rows.add(statusList, null, "브레이크액", null, level(st.brakeFluid));
                services().vehicle.readReadiness(new Callback<VehicleReadiness>() {
                    @Override
                    public void onSuccess(VehicleReadiness r) {
                        if (isAdded()) {
                            Rows.add(statusList, null, "주행 가능 거리", null,
                                    r.rangeKm == null ? "확인 불가" : String.format(Locale.KOREA, "%d km", Math.round(r.rangeKm)));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Rows.add(statusList, null, "차량 상태를 읽지 못했어요", e.getMessage(), null);
                }
            }
        });
    }

    static String level(String v) {
        if (v == null || "ERROR".equals(v)) {
            return "확인 불가";
        }
        switch (v) {
            case "NORMAL":
                return "정상";
            case "LOW":
                return "부족";
            case "CRITICALLY_LOW":
                return "매우 부족";
            case "HIGH":
                return "많음";
            default:
                return v;
        }
    }

    /** 조작 실패 원인 확인: 차량에서 확인된 것과 직접 확인할 것을 나눠 보여 주고 읽어 준다. */
    private void diagnose() {
        services().vehicle.readStatus(new Callback<VehicleStatus>() {
            @Override
            public void onSuccess(VehicleStatus st) {
                show(StartDiagnosis.diagnose(st));
            }

            @Override
            public void onError(Exception e) {
                show(StartDiagnosis.diagnose(null));
            }
        });
    }

    private void show(List<StartDiagnosis.Finding> findings) {
        if (!isAdded()) {
            return;
        }
        diagnosisList.removeAllViews();
        diagnosisList.addView(Choices.sectionTitle(requireContext(), "출발이 안 될 때 확인해요"));
        StringBuilder speech = new StringBuilder();
        boolean anyConfirmed = false;
        for (StartDiagnosis.Finding f : findings) {
            Rows.add(diagnosisList, f.confirmed ? "!" : "?", f.title, f.action, f.confirmed ? "차량 확인" : "직접 확인");
            if (f.confirmed) {
                anyConfirmed = true;
                speech.append(f.title).append(". ").append(f.action).append(' ');
            }
        }
        if (!anyConfirmed) {
            speech.append("차량에서 확인된 문제는 없어요. ").append(findings.get(0).title).append(' ').append(findings.get(0).action);
        }
        speak(speech.toString().trim());
    }

    @Override
    public void onDestroyView() {
        if (speaking != null) {
            speaking.release();
        }
        super.onDestroyView();
    }

    @Override
    protected boolean onVoiceText(String text) {
        if (text.replace(" ", "").contains("출발") || text.contains("시동")) {
            diagnose();
            return true;
        }
        List<VehicleFaq.Entry> found = VehicleFaq.search(text);
        if (found.isEmpty()) {
            speak("그 기능은 아직 정리되어 있지 않아요. 아래 목록에서 골라 주세요.");
            return true;
        }
        speak(found.get(0).answer);
        return true;
    }
}
