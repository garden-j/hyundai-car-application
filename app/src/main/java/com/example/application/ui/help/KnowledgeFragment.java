package com.example.application.ui.help;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.databinding.ContentScrollBinding;
import com.example.application.databinding.ItemRowBinding;
import com.example.application.domain.KnowledgeCatalog;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.DriverProfile;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Rows;
import com.example.application.ui.common.SpeakingRows;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 운전 지식(주행 전 기본 안내사항): 추천 항목 + 카테고리별 항목. 누르면 짧게 음성으로 설명한다.
 */
public class KnowledgeFragment extends BaseStepFragment {
    private LinearLayout items;
    private SpeakingRows speaking;
    private final Set<KnowledgeCatalog.Category> category = EnumSet.of(KnowledgeCatalog.Category.BASICS);

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setSection("운전 지식");
        setTitle("출발 전에 알아 두면 좋아요", "항목을 누르면 짧게 읽어 드려요.");
        showNav(true, false);
        enableVoice("비보호 좌회전 알려 줘");
        setPrimary("홈으로", () -> nav().home());
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        speaking = new SpeakingRows(services().tts);

        DriverProfile p = services().profiles.get();
        list.addView(Choices.sectionTitle(requireContext(), "나에게 맞는 추천"));
        LinearLayout rec = new LinearLayout(requireContext());
        rec.setOrientation(LinearLayout.VERTICAL);
        list.addView(rec);
        for (KnowledgeCatalog.Item i : KnowledgeCatalog.recommended(setupVm().setup().skills, p.difficulties)) {
            addItem(rec, i);
        }
        Choices.addChipSection(list, "카테고리", Arrays.asList(KnowledgeCatalog.Category.values()),
                category, true, this::renderItems);
        items = new LinearLayout(requireContext());
        items.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Choices.dp(requireContext(), 16);
        list.addView(items, lp);
        renderItems();
        return null;
    }

    private void renderItems() {
        items.removeAllViews();
        for (KnowledgeCatalog.Item i : KnowledgeCatalog.of(category.iterator().next())) {
            addItem(items, i);
        }
    }

    private void addItem(LinearLayout parent, KnowledgeCatalog.Item item) {
        ItemRowBinding row = Rows.add(parent, null, item.title, item.summary, SpeakingRows.IDLE);
        row.getRoot().setOnClickListener(v -> speaking.toggle(row, item.title, item.speech, item.speech));
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
        String q = VoiceCommandParser.normalize(text);
        for (KnowledgeCatalog.Item i : KnowledgeCatalog.all()) {
            String key = i.title.replace(" ", "");
            if (q.contains(key) || (key.length() > 3 && q.contains(key.substring(0, 4)))) {
                speak(i.speech);
                return true;
            }
        }
        return false;
    }
}
