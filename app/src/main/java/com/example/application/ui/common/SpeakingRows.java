package com.example.application.ui.common;

import com.example.application.core.pleos.voice.TtsGateway;
import com.example.application.databinding.ItemRowBinding;

/**
 * 목록 행을 눌러 음성으로 듣는 화면용 도우미.
 * 누르면 "듣는 중", 음성이 끝나거나 같은 행을 한 번 더 누르면 "다시 듣기"로 바뀐다.
 * 다른 행을 누르면 앞 행은 "다시 듣기"로 돌리고 새 행을 읽는다.
 */
public final class SpeakingRows {
    public static final String IDLE = "듣기";
    public static final String PLAYING = "듣는 중";
    public static final String AGAIN = "다시 듣기";

    private final TtsGateway tts;
    private ItemRowBinding current;
    private final TtsGateway.Listener listener = speaking -> {
        if (!speaking) {
            markDone();
        }
    };

    public SpeakingRows(TtsGateway tts) {
        this.tts = tts;
        tts.addListener(listener);
    }

    /** 행을 눌렀을 때. expanded는 읽는 동안 행에 보여 줄 설명(없으면 null). */
    public void toggle(ItemRowBinding row, String title, String expanded, String speech) {
        if (row == current) {
            tts.stop();
            markDone();
            return;
        }
        markDone();
        current = row;
        Rows.bind(row, null, title, expanded != null ? expanded : row.rowSubtitle.getText().toString(), PLAYING);
        if (expanded == null && row.rowSubtitle.getText().length() == 0) {
            row.rowSubtitle.setVisibility(android.view.View.GONE);
        }
        tts.speak(speech);
    }

    private void markDone() {
        if (current != null) {
            current.rowTrailing.setText(AGAIN);
            current = null;
        }
    }

    /** 화면을 떠날 때: 읽기를 멈추고 리스너를 해제한다. */
    public void release() {
        tts.removeListener(listener);
        tts.stop();
        current = null;
    }
}
