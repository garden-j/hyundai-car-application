package com.example.application.ui.common;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.application.R;
import com.example.application.model.Labeled;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 선택지 UI 도우미(Figma 컴포넌트 기준).
 * <ul>
 *   <li>단일 선택(항목이 적을 때): Segmented — 흰 바탕(모서리 32, 안쪽 8) 안에 높이 84 항목</li>
 *   <li>여러 개 선택 또는 항목이 많을 때: 칩 격자 — 높이 100, 모서리 24, 간격 16</li>
 *   <li>설명이 있는 선택지: OptionCard 격자 — 제목 40 + 원형 체크</li>
 * </ul>
 * 선택 상태는 호출자가 가진 Set에 직접 반영하고, 바뀔 때마다 onChange를 부른다.
 */
public final class Choices {
    private static final int GAP_DP = 16;
    private static final int MAX_SEGMENTS_WIDE = 6;
    private static final int MAX_SEGMENTS_NARROW = 3;

    private Choices() {
    }

    /**
     * 섹션 제목 + 선택지를 container에 추가한다.
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
        int maxSegments = isWide(c) ? MAX_SEGMENTS_WIDE : MAX_SEGMENTS_NARROW;
        if (single && values.size() <= maxSegments) {
            return addSegmented(container, values, label, selected, onChange);
        }
        return addChipGrid(container, values, label, selected, single, onChange);
    }

    public static <T extends Labeled> Runnable addChipSection(LinearLayout container, String sectionTitle,
                                                              List<T> values, Set<T> selected, boolean single,
                                                              Runnable onChange) {
        return addChipSection(container, sectionTitle, values, Labeled::label, selected, single, onChange);
    }

    private static <T> Runnable addSegmented(LinearLayout container, List<T> values, Function<T, String> label,
                                             Set<T> selected, Runnable onChange) {
        Context c = container.getContext();
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(c, 8);
        bar.setPadding(pad, pad, pad, pad);
        container.addView(bar, matchWrap());
        List<TextView> items = new ArrayList<>();
        for (T v : values) {
            TextView item = choiceText(c, label.apply(v), R.drawable.bg_segment_item);
            bar.addView(item, new LinearLayout.LayoutParams(0, dp(c, 84), 1f));
            items.add(item);
        }
        Runnable refresh = () -> {
            for (int i = 0; i < items.size(); i++) {
                boolean on = selected.contains(values.get(i));
                items.get(i).setSelected(on);
                items.get(i).setTextColor(ContextCompat.getColor(c, on ? R.color.text_primary : R.color.text_secondary));
            }
        };
        wire(items, values, selected, true, refresh, onChange);
        refresh.run();
        return refresh;
    }

    private static <T> Runnable addChipGrid(LinearLayout container, List<T> values, Function<T, String> label,
                                            Set<T> selected, boolean single, Runnable onChange) {
        Context c = container.getContext();
        int n = values.size();
        int columns = n <= 3 ? n : n == 4 ? 4 : n <= 6 ? 3 : 4;
        if (!isWide(c)) {
            columns = Math.min(columns, 2);
        }
        List<TextView> items = new ArrayList<>();
        List<View> cells = new ArrayList<>();
        for (T v : values) {
            TextView item = choiceText(c, label.apply(v), R.drawable.bg_chip);
            item.setMinHeight(dp(c, 100));
            items.add(item);
            cells.add(item);
        }
        addGrid(container, cells, columns);
        Runnable refresh = () -> {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setSelected(selected.contains(values.get(i)));
            }
        };
        wire(items, values, selected, single, refresh, onChange);
        refresh.run();
        return refresh;
    }

    /** 설명이 있는 큰 카드 선택지를 격자로 추가한다(세로 화면은 한 줄에 하나). 화면을 다시 그리는 함수를 돌려준다. */
    public static <T extends Labeled> Runnable addCardGrid(LinearLayout container, List<T> values,
                                                           Set<T> selected, boolean single,
                                                           int columns, Runnable onChange) {
        Context c = container.getContext();
        LayoutInflater inflater = LayoutInflater.from(c);
        List<View> cards = new ArrayList<>();
        for (T v : values) {
            View card = inflater.inflate(R.layout.item_option_card, container, false);
            ((TextView) card.findViewById(R.id.card_title)).setText(v.label());
            TextView desc = card.findViewById(R.id.card_desc);
            desc.setVisibility(v.description() == null ? View.GONE : View.VISIBLE);
            desc.setText(v.description());
            cards.add(card);
        }
        addGrid(container, cards, isWide(c) ? columns : 1);
        Runnable refresh = () -> {
            for (int i = 0; i < cards.size(); i++) {
                setOptionSelected(cards.get(i), selected.contains(values.get(i)));
            }
        };
        wire(cards, values, selected, single, refresh, onChange);
        refresh.run();
        return refresh;
    }

    /** OptionCard의 선택 표시(배경·원형 체크·설명 색)를 바꾼다. */
    public static void setOptionSelected(View card, boolean on) {
        card.setSelected(on);
        card.findViewById(R.id.card_check).setSelected(on);
        card.findViewById(R.id.card_check_icon).setVisibility(on ? View.VISIBLE : View.GONE);
        ((TextView) card.findViewById(R.id.card_desc)).setTextColor(
                ContextCompat.getColor(card.getContext(), on ? R.color.text_primary : R.color.text_secondary));
    }

    /** 같은 너비 칸으로 나눈 격자(행 간격·열 간격 16). 마지막 줄의 빈칸은 비워 둔다. */
    public static void addGrid(LinearLayout container, List<View> cells, int columns) {
        Context c = container.getContext();
        int gap = dp(c, GAP_DP);
        columns = Math.max(1, columns);
        for (int start = 0; start < cells.size(); start += columns) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = matchWrap();
            if (start > 0) {
                rowLp.topMargin = gap;
            }
            container.addView(row, rowLp);
            for (int col = 0; col < columns; col++) {
                int i = start + col;
                View cell = i < cells.size() ? cells.get(i) : new Space(c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (col > 0) {
                    lp.setMarginStart(gap);
                }
                row.addView(cell, lp);
            }
        }
    }

    private static <T> void wire(List<? extends View> views, List<T> values, Set<T> selected, boolean single,
                                 Runnable refresh, Runnable onChange) {
        for (int i = 0; i < views.size(); i++) {
            T v = values.get(i);
            views.get(i).setOnClickListener(view -> {
                toggle(selected, v, single);
                refresh.run();
                if (onChange != null) {
                    onChange.run();
                }
            });
        }
    }

    private static TextView choiceText(Context c, String text, int background) {
        TextView tv = new TextView(c);
        tv.setTextAppearance(R.style.Text_Body);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(background);
        int pad = dp(c, 16);
        tv.setPadding(pad, 0, pad, 0);
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
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

    /** 필드 제목(Body L 30, 보조 색). 필드 사이 40, 제목과 내용 사이 12. */
    public static TextView sectionTitle(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setTextAppearance(R.style.Text_Section);
        tv.setText(text);
        tv.setTag("section");
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(c, 40);
        lp.bottomMargin = dp(c, 12);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** 가로(L) 배치 여부. */
    public static boolean isWide(Context c) {
        return c.getResources().getConfiguration().screenWidthDp >= 1100;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }
}
