package com.example.application.ui.drive;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.core.drive.DriveSession;
import com.example.application.data.PracticeSession;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.domain.Encouragement;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;

import java.util.ArrayList;
import java.util.List;

/** 주행 후 격려(Figma L/22): 해낸 목표와 격려 멘트, 다음 연습 추천. 이어서 차량 설정 복원으로 갈 수 있다. */
public class EncourageFragment extends BaseStepFragment {

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        DriveSession session = DriveSession.get();
        Encouragement e = Encouragement.build(setupVm().setup(), session.stats(), session.encourageLevel());
        showNav(false, false);
        setSection("");
        setTitle(e.title, e.subtitle);
        enableVoice("홈으로 가자");
        PracticeSession saved = services().sessions.active();
        boolean canRestore = saved != null && saved.settingsBefore != null;
        if (canRestore) {
            setSecondary("차량 설정 되돌리기", () -> nav().push(new RestoreFragment()));
        }
        setPrimary("홈으로", this::goHome);

        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = Choices.dp(requireContext(), 32);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = Choices.dp(requireContext(), 40);
        list.addView(card, cardLp);

        TextView label = text(R.style.Text_Caption, "오늘 해낸 목표");
        card.addView(label);
        LinearLayout countRow = new LinearLayout(requireContext());
        countRow.setGravity(Gravity.BOTTOM);
        TextView count = text(R.style.Text_Title, String.valueOf(e.achieved.size()));
        count.setTextSize(88);
        countRow.addView(count);
        TextView unit = text(R.style.Text_Headline, " 가지");
        unit.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
        unit.setPadding(0, 0, 0, Choices.dp(requireContext(), 12));
        countRow.addView(unit);
        card.addView(countRow);
        if (!e.achieved.isEmpty()) {
            List<View> chips = new ArrayList<>();
            for (String a : e.achieved) {
                TextView chip = text(R.style.Text_Body, "✓  " + a);
                chip.setGravity(Gravity.CENTER);
                chip.setBackgroundResource(R.drawable.bg_chip_muted);
                chip.setMinHeight(Choices.dp(requireContext(), 80));
                chips.add(chip);
            }
            LinearLayout grid = new LinearLayout(requireContext());
            grid.setOrientation(LinearLayout.VERTICAL);
            grid.setPadding(0, Choices.dp(requireContext(), 16), 0, 0);
            card.addView(grid);
            Choices.addGrid(grid, chips, Choices.isWide(requireContext()) ? 2 : 1);
        }
        list.addView(Choices.sectionTitle(requireContext(), "다음 연습"));
        TextView next = text(R.style.Text_Body, e.next);
        next.setBackgroundResource(R.drawable.bg_card);
        next.setPadding(pad, pad, pad, pad);
        list.addView(next);

        speak(e.speech);
        return null;
    }

    private TextView text(int style, String value) {
        TextView tv = new TextView(requireContext());
        tv.setTextAppearance(style);
        tv.setText(value);
        return tv;
    }

    private void goHome() {
        services().tts.stop();
        services().sessions.clear();
        nav().home();
    }

    @Override
    protected boolean onVoiceText(String text) {
        if (VoiceCommandParser.normalize(text).contains("홈")) {
            goHome();
            return true;
        }
        return false;
    }
}
