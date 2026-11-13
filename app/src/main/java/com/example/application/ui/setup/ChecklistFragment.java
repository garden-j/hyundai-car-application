package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.example.application.R;
import com.example.application.databinding.ItemRowBinding;
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

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("출발 전에\n확인해요", "다 되면 출발해요.");
        enableVoice("다 확인했어");
        setPrimary("다음", this::tryNext);
        setSecondary("차량 상태 다시 확인", () -> setupVm().refreshChecklist());
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
        for (ChecklistItem item : list) {
            String badge = item.status == ChecklistItem.Status.DONE ? "✓"
                    : item.status == ChecklistItem.Status.ATTENTION ? "!" : "";
            String trailing = item.autoChecked ? "자동 확인" : item.isDone() ? "확인함" : "눌러서 확인";
            if (item.id == ChecklistItem.Id.GUIDE_VOLUME && !item.isDone()) {
                trailing = "눌러서 들어보기";
            }
            ItemRowBinding b = Rows.add(rows, badge, item.id.label + (item.id.required ? " (필수)" : ""),
                    item.guide, trailing);
            if (item.status == ChecklistItem.Status.ATTENTION) {
                b.rowBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.attention));
            } else if (item.isDone()) {
                b.rowBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.positive));
            }
            b.getRoot().setOnClickListener(v -> onItemClick(item));
        }
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
