package com.example.application.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.application.databinding.ItemRowBinding;

/** item_row 레이아웃을 채우는 도우미. null인 칸은 숨긴다. */
public final class Rows {
    private Rows() {
    }

    public static ItemRowBinding add(ViewGroup parent, String badge, String title, String subtitle, String trailing) {
        ItemRowBinding b = ItemRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        bind(b, badge, title, subtitle, trailing);
        parent.addView(b.getRoot());
        return b;
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
