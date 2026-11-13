package com.example.application.ui.setup;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.GoalAmount;
import com.example.application.ui.common.Choices;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** ② 원하는 만큼 정해 볼까요? — 거리(3/5/10km) 또는 시간(10/20/30분) 빠른 선택 + 직접 입력(+/-). */
public class DistanceTimeFragment extends BaseSetupFragment {
    private static final List<GoalAmount> QUICK = Arrays.asList(
            new GoalAmount(GoalAmount.Unit.DISTANCE_KM, 3), new GoalAmount(GoalAmount.Unit.DISTANCE_KM, 5),
            new GoalAmount(GoalAmount.Unit.DISTANCE_KM, 10), new GoalAmount(GoalAmount.Unit.TIME_MIN, 10),
            new GoalAmount(GoalAmount.Unit.TIME_MIN, 20), new GoalAmount(GoalAmount.Unit.TIME_MIN, 30));

    private final Set<GoalAmount> quickSelected = new LinkedHashSet<>();
    private final Set<GoalAmount.Unit> unitSelected = new LinkedHashSet<>();
    private Runnable refreshQuick;
    private Runnable refreshUnit;
    private TextView valueView;
    private TextView stepHint;

    @Override
    protected SetupStep step() {
        return SetupStep.DISTANCE_TIME;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("원하는 만큼\n정해 볼까요?", "거리나 시간 중 하나를 정해 주세요.");
        enableVoice("25분");
        setPrimary("다음", this::goNext);
        if (setup().amount == null) {
            setup().amount = new GoalAmount(GoalAmount.Unit.TIME_MIN, 20);
        }
        LinearLayout list = scrollList(inflater);
        syncSelections();
        refreshQuick = Choices.addChipSection(list, "빠르게 고르기", QUICK, GoalAmount::display, quickSelected,
                true, () -> {
                    if (!quickSelected.isEmpty()) {
                        GoalAmount g = quickSelected.iterator().next();
                        setup().amount = new GoalAmount(g.unit, g.value);
                    }
                    render();
                });
        refreshUnit = Choices.addChipSection(list, "직접 정하기", Arrays.asList(GoalAmount.Unit.values()),
                u -> u == GoalAmount.Unit.DISTANCE_KM ? "거리" : "시간", unitSelected, true, () -> {
                    GoalAmount.Unit u = unitSelected.isEmpty() ? setup().amount.unit : unitSelected.iterator().next();
                    if (u != setup().amount.unit) {
                        setup().amount = new GoalAmount(u, u == GoalAmount.Unit.DISTANCE_KM ? 5 : 20);
                    }
                    render();
                });
        list.addView(stepper());
        render();
        return null;
    }

    private View stepper() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, Choices.dp(requireContext(), 16), 0, 0);
        MaterialButton minus = navButton("−", v -> change(-1));
        MaterialButton plus = navButton("+", v -> change(+1));
        valueView = new TextView(requireContext());
        valueView.setTextSize(64);
        valueView.setGravity(Gravity.CENTER);
        valueView.setMinWidth(Choices.dp(requireContext(), 220));
        row.addView(minus);
        row.addView(valueView);
        row.addView(plus);

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(row);
        stepHint = new TextView(requireContext());
        stepHint.setTextAppearance(R.style.Text_Caption);
        stepHint.setGravity(Gravity.CENTER);
        box.addView(stepHint);
        return box;
    }

    private MaterialButton navButton(String text, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setTextSize(28);
        b.setMinWidth(Choices.dp(requireContext(), 80));
        b.setMinHeight(Choices.dp(requireContext(), 80));
        b.setOnClickListener(l);
        return b;
    }

    private void change(int dir) {
        GoalAmount a = setup().amount;
        int v = a.value + dir * a.unit.step;
        a.value = Math.max(a.unit.min, Math.min(a.unit.max, v));
        render();
    }

    private void syncSelections() {
        quickSelected.clear();
        GoalAmount a = setup().amount;
        for (GoalAmount q : QUICK) {
            if (q.unit == a.unit && q.value == a.value) {
                quickSelected.add(q);
            }
        }
        unitSelected.clear();
        unitSelected.add(a.unit);
    }

    private void render() {
        syncSelections();
        if (refreshQuick != null) {
            refreshQuick.run();
            refreshUnit.run();
        }
        GoalAmount a = setup().amount;
        valueView.setText(a.display());
        stepHint.setText(a.unit.step + a.unit.suffix + "씩 바뀌어요 · 예상 "
                + (a.unit == GoalAmount.Unit.DISTANCE_KM ? "약 " + a.estimatedMinutes() + "분"
                : String.format(java.util.Locale.KOREA, "약 %.1fkm", a.estimatedKm())));
    }

    @Override
    protected boolean onVoiceText(String text) {
        Integer n = VoiceCommandParser.parseNumber(text);
        if (n == null) {
            return false;
        }
        String norm = VoiceCommandParser.normalize(text);
        GoalAmount.Unit unit = norm.contains("km") || norm.contains("킬로") || norm.contains("키로")
                ? GoalAmount.Unit.DISTANCE_KM : GoalAmount.Unit.TIME_MIN;
        if (norm.contains("시간") && !norm.contains("분")) {
            n = n * 60; // "1시간" → 60분
        }
        setup().amount = new GoalAmount(unit, Math.max(unit.min, Math.min(unit.max, n)));
        render();
        speak(setup().amount.display() + "로 정했어요.");
        return true;
    }
}
