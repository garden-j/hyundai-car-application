package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.example.application.R;
import com.example.application.databinding.ItemCheckCardBinding;
import com.example.application.domain.ChecklistEvaluator;
import com.example.application.domain.ChecklistItem;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;
import java.util.List;

/**
 * 출발 전에 확인해요 — 차량 데이터로 확인 가능한 항목(안전벨트·문·연료)은 자동 점검,
 * 나머지는 사용자가 눌러서 확인. 필수 항목이 미완료면 출발할 수 없다.
 */
public class ChecklistFragment extends BaseSetupFragment {
    private LinearLayout rows;
    private List<ChecklistItem> items = new ArrayList<>();

    @Override
    protected SetupStep step() {
        return SetupStep.CHECKLIST;
    }

    /** 디자인(Figma L/17)에 있는 항목. 나머지 필수 항목은 확인이 필요할 때만 보여 준다. */
    private static final java.util.EnumSet<ChecklistItem.Id> DESIGN_ITEMS = java.util.EnumSet.of(
            ChecklistItem.Id.SEAT_BELT, ChecklistItem.Id.SIDE_MIRROR, ChecklistItem.Id.FUEL,
            ChecklistItem.Id.SEAT_POSITION, ChecklistItem.Id.GUIDE_VOLUME, ChecklistItem.Id.MUSIC_OFF,
            ChecklistItem.Id.PHONE_MOUNT);

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("출발 전에 확인해요", "다 되면 출발해요.");
        setPrimary("다음", this::tryNext);
        rows = scrollList(inflater);
        setupVm().checklist().observe(getViewLifecycleOwner(), this::render);
        setupVm().refreshChecklist();
        return null;
    }

    private void render(List<ChecklistItem> list) {
        if (list == null) {
            return;
        }
        items = list;
        rows.removeAllViews();
        rows.addView(com.example.application.ui.common.Choices.sectionTitle(requireContext(), "출발 전 체크리스트"));
        List<View> cards = new ArrayList<>();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ChecklistItem item : list) {
            if (!DESIGN_ITEMS.contains(item.id) && item.isDone()) {
                continue; // 문 닫힘·목적지 등은 문제가 있을 때만 표시
            }
            ItemCheckCardBinding b = ItemCheckCardBinding.inflate(inflater, rows, false);
            b.checkLabel.setText(item.id.label);
            boolean done = item.isDone();
            b.checkBox.setSelected(done);
            b.checkIcon.setVisibility(done ? View.VISIBLE : View.GONE);
            b.checkRequired.setVisibility(item.id.required ? View.VISIBLE : View.GONE);
            b.checkAuto.setVisibility(item.autoChecked ? View.VISIBLE : View.GONE);
            if (item.status == ChecklistItem.Status.ATTENTION) {
                b.checkAuto.setVisibility(View.VISIBLE);
                b.checkAuto.setText("확인 필요");
                b.checkAuto.setTextColor(ContextCompat.getColor(requireContext(), R.color.attention));
            }
            b.getRoot().setContentDescription(item.id.label + (item.id.required ? ", 필수" : "")
                    + (item.autoChecked ? ", 자동 확인" : "") + (done ? ", 확인됨" : ", 확인 안 됨"));
            b.getRoot().setOnClickListener(v -> onItemClick(item));
            cards.add(b.getRoot());
        }
        com.example.application.ui.common.Choices.addGrid(rows, cards,
                com.example.application.ui.common.Choices.isWide(requireContext()) ? 2 : 1);
    }

    private void onItemClick(ChecklistItem item) {
        if (item.autoChecked) {
            // 차량이 확인하는 항목은 다시 읽어 온다(예: 안전벨트를 맨 뒤)
            if (!item.isDone()) {
                speak(item.guide);
            }
            setupVm().refreshChecklist();
            return;
        }
        if (item.id == ChecklistItem.Id.GUIDE_VOLUME && !item.isDone()) {
            speak("안내 음성이 잘 들리나요? 잘 들리면 확인으로 표시할게요.");
        }
        setupVm().toggleUserCheck(item.id);
    }

    private void tryNext() {
        List<ChecklistItem> blocking = ChecklistEvaluator.blocking(items);
        if (!blocking.isEmpty()) {
            ChecklistItem first = blocking.get(0);
            String msg = first.guide != null ? first.guide : first.id.label + "을 확인해 주세요.";
            speak(msg);
            new AlertDialog.Builder(requireContext())
                    .setTitle("출발 전에 꼭 확인해 주세요")
                    .setMessage(labels(blocking) + "\n\n" + msg)
                    .setPositiveButton("확인할게요", (d, w) -> setupVm().refreshChecklist())
                    .show();
            return;
        }
        List<ChecklistItem> incomplete = ChecklistEvaluator.incomplete(items);
        if (!incomplete.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("아직 확인하지 않은 항목이 있어요")
                    .setMessage(labels(incomplete) + "\n\n그래도 다음으로 넘어갈까요?")
                    .setPositiveButton("계속", (d, w) -> goNext())
                    .setNegativeButton("확인할게요", null)
                    .show();
            return;
        }
        goNext();
    }

    private static String labels(List<ChecklistItem> list) {
        StringBuilder sb = new StringBuilder();
        for (ChecklistItem i : list) {
            sb.append("• ").append(i.id.label).append('\n');
        }
        return sb.toString().trim();
    }

    @Override
    protected boolean onVoiceText(String text) {
        String n = VoiceCommandParser.normalize(text);
        if (n.contains("다확인") || n.contains("다했") || n.contains("확인했")) {
            for (ChecklistItem i : items) {
                if (!i.autoChecked && !i.isDone()) {
                    setupVm().toggleUserCheck(i.id);
                }
            }
            tryNext();
            return true;
        }
        return false;
    }
}
