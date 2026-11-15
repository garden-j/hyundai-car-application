package com.example.application.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.R;
import com.example.application.databinding.ItemRowBinding;

/**
 * item_row 레이아웃을 채우는 도우미. null인 칸은 숨긴다.
 * 같은 부모에 연달아 추가한 행은 흰 카드 하나(Figma List)에 구분선과 함께 묶인다.
 */
public final class Rows {
    private static final String GROUP_TAG = "row_group";

    private Rows() {
    }

    public static ItemRowBinding add(ViewGroup parent, String badge, String title, String subtitle, String trailing) {
        ViewGroup group = groupFor(parent);
        if (group.getChildCount() > 0) {
            View divider = new View(parent.getContext());
            divider.setBackgroundResource(R.drawable.bg_divider);
            group.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    Choices.dp(parent.getContext(), 2)));
        }
        ItemRowBinding b = ItemRowBinding.inflate(LayoutInflater.from(parent.getContext()), group, false);
        bind(b, badge, title, subtitle, trailing);
        group.addView(b.getRoot());
        return b;
    }

    /** 부모의 마지막 자식이 행 묶음이면 그것을, 아니면 새 흰 카드 묶음을 만든다. */
    private static ViewGroup groupFor(ViewGroup parent) {
        int n = parent.getChildCount();
        if (n > 0 && GROUP_TAG.equals(parent.getChildAt(n - 1).getTag())) {
            return (ViewGroup) parent.getChildAt(n - 1);
        }
        Context c = parent.getContext();
        LinearLayout group = new LinearLayout(c);
        group.setTag(GROUP_TAG);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundResource(R.drawable.bg_card);
        group.setPadding(Choices.dp(c, 32), Choices.dp(c, 8), Choices.dp(c, 32), Choices.dp(c, 8));
        LinearLayout.LayoutParams lp = Choices.matchWrap();
        // 필드 제목 바로 아래가 아니면(제목 묶음 다음 등) 위 간격 40
        boolean afterSection = n > 0 && "section".equals(parent.getChildAt(n - 1).getTag());
        if (n == 0 && parent.getParent() instanceof ViewGroup) {
            // 빈 컨테이너에 처음 추가: 컨테이너 바로 앞이 필드 제목이면 그 아래에 붙인다
            ViewGroup outer = (ViewGroup) parent.getParent();
            int at = outer.indexOfChild(parent);
            afterSection = at > 0 && "section".equals(outer.getChildAt(at - 1).getTag());
        }
        lp.topMargin = afterSection ? 0 : Choices.dp(c, n == 0 ? 40 : 24);
        parent.addView(group, lp);
        return group;
    }

    public static void bind(ItemRowBinding b, String badge, String title, String subtitle, String trailing) {
        set(b.rowBadge, badge);
        set(b.rowTitle, title);
        set(b.rowSubtitle, subtitle);
        set(b.rowTrailing, trailing);
    }

    private static void set(android.widget.TextView v, String text) {
        v.setVisibility(text == null ? View.GONE : View.VISIBLE);
        v.setText(text);
    }
}
