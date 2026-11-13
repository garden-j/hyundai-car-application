package com.example.application.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.model.Labeled;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 선택지 UI 도우미. 칩(작은 선택지)과 카드(설명이 있는 큰 선택지) 두 형태를 지원한다.
 * 선택 상태는 호출자가 가진 Set에 직접 반영하고, 바뀔 때마다 onChange를 부른다.
 */
public final class Choices {
    private Choices() {
    }

    /**
     * 섹션 제목 + 칩 그룹을 container에 추가한다.
     *
     * @return 외부에서 selected를 바꿨을 때(음성 답변 등) 화면을 다시 그리는 함수
     */
    public static <T> Runnable addChipSection(LinearLayout container, String sectionTitle, List<T> values,
                                               Function<T, String> label, Set<T> selected, boolean single,
                                               Runnable onChange) {
        Context c = container.getContext();
        if (sectionTitle != null) {
            container.addView(sectionTitle(c, sectionTitle));
        }
        ChipGroup group = new ChipGroup(c);
        group.setChipSpacingHorizontal(dp(c, 12));
        group.setChipSpacingVertical(dp(c, 8));
        container.addView(group, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return bindChips(group, values, label, selected, single, onChange);
    }

    public static <T> Runnable bindChips(ChipGroup group, List<T> values, Function<T, String> label,
                                     Set<T> selected, boolean single, Runnable onChange) {
        group.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(group.getContext());
        List<Chip> chips = new ArrayList<>();
        for (T v : values) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_choice_chip, group, false);
            chip.setText(label.apply(v));
            chip.setChecked(selected.contains(v));
            chip.setOnClickListener(view -> {
                toggle(selected, v, single);
                for (int i = 0; i < chips.size(); i++) {
                    chips.get(i).setChecked(selected.contains(values.get(i)));
                }
                if (onChange != null) {
                    onChange.run();
                }
            });
            chips.add(chip);
            group.addView(chip);
        }
        return () -> {
            for (int i = 0; i < chips.size(); i++) {
                chips.get(i).setChecked(selected.contains(values.get(i)));
            }
        };
    }

    public static <T extends Labeled> Runnable addChipSection(LinearLayout container, String sectionTitle,
                                                               List<T> values, Set<T> selected, boolean single,
                                                               Runnable onChange) {
        return addChipSection(container, sectionTitle, values, Labeled::label, selected, single, onChange);
    }

    /** 설명이 있는 큰 카드 선택지를 격자로 추가한다. 화면을 다시 그리는 함수를 돌려준다. */
    public static <T extends Labeled> Runnable addCardGrid(LinearLayout container, List<T> values,
                                                                       Set<T> selected, boolean single,
                                                                       int columns, Runnable onChange) {
        Context c = container.getContext();
        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(columns);
        container.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutInflater inflater = LayoutInflater.from(c);
        List<MaterialCardView> cards = new ArrayList<>();
        for (T v : values) {
            MaterialCardView card = (MaterialCardView) inflater.inflate(R.layout.item_option_card, grid, false);
            ((TextView) card.findViewById(R.id.card_title)).setText(v.label());
            TextView desc = card.findViewById(R.id.card_desc);
            if (v.description() == null) {
                desc.setVisibility(View.GONE);
            } else {
                desc.setText(v.description());
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.setMargins(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8));
            card.setLayoutParams(lp);
            card.setChecked(selected.contains(v));
            card.setStrokeWidth(dp(c, card.isChecked() ? 3 : 1));
            cards.add(card);
            grid.addView(card);
        }
        Runnable refresh = () -> {
            for (int i = 0; i < cards.size(); i++) {
                cards.get(i).setChecked(selected.contains(values.get(i)));
                cards.get(i).setStrokeWidth(dp(c, cards.get(i).isChecked() ? 3 : 1));
            }
        };
        for (int i = 0; i < cards.size(); i++) {
            T v = values.get(i);
            cards.get(i).setOnClickListener(view -> {
                toggle(selected, v, single);
                refresh.run();
                if (onChange != null) {
                    onChange.run();
                }
            });
        }
        return refresh;
    }

    private static <T> void toggle(Set<T> selected, T v, boolean single) {
        if (single) {
            // 단일 선택은 라디오 버튼처럼: 이미 고른 것을 다시 눌러도 해제되지 않는다
            selected.clear();
            selected.add(v);
        } else if (!selected.remove(v)) {
            selected.add(v);
        }
    }

    public static TextView sectionTitle(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setTextAppearance(R.style.Text_Section);
        tv.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 20);
        lp.bottomMargin = dp(c, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }
}
