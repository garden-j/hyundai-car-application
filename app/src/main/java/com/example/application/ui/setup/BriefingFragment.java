package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.domain.BriefingGenerator;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.BriefingPoint;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Resource;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;
import java.util.List;

/**
 * 주의할 점을 미리 알려드려요 — 복잡 구간을 주행 순서대로 지도(미리보기)와 음성으로 브리핑.
 * 화면에 들어오면 자동으로 읽고, 구간을 누르면 그 구간만 다시 읽는다.
 */
public class BriefingFragment extends BaseSetupFragment {
    private LinearLayout rows;
    private TextView status;
    private List<BriefingPoint> points = new ArrayList<>();
    private boolean spokenOnce;

    @Override
    protected SetupStep step() {
        return SetupStep.BRIEFING;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("주의할 점을 미리 알려 드려요", "순서대로 알려 드릴게요.");
        setPrimary("다음", this::goNext);
        setSecondary("다시 듣기", this::speakAll);

        LinearLayout list = scrollList(inflater);
        status = new TextView(requireContext());
        status.setTextAppearance(R.style.Text_Body);
        list.addView(status);
        rows = new LinearLayout(requireContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        list.addView(rows);

        setupVm().briefing().observe(getViewLifecycleOwner(), this::render);
        setupVm().loadBriefing();
        return null;
    }

    private void render(Resource<List<BriefingPoint>> res) {
        if (res == null) {
            return;
        }
        if (res.state == Resource.State.LOADING) {
            status.setText("경로를 살펴보고 있어요…");
            return;
        }
        if (res.state == Resource.State.ERROR) {
            status.setText(res.error);
            return;
        }
        points = res.data;
        status.setText(points.isEmpty() ? "특별히 복잡한 구간이 없어요." : "");
        rows.removeAllViews();
        for (BriefingPoint p : points) {
            Rows.add(rows, String.valueOf(p.order), p.type.label,
                    BriefingGenerator.listDistance(p.distanceKm) + " · " + p.instruction, null)
                    .getRoot().setOnClickListener(v -> speak(p.speech));
        }
        if (!spokenOnce) {
            spokenOnce = true;
            speakAll();
        }
    }

    private void speakAll() {
        services().tts.speakAll(BriefingGenerator.speechScript(points));
    }

    @Override
    protected boolean onVoiceText(String text) {
        String n = VoiceCommandParser.normalize(text);
        if (n.contains("다시") || n.contains("한번더")) {
            Integer which = VoiceCommandParser.parseNumber(text);
            if (which != null && which >= 1 && which <= points.size()) {
                speak(points.get(which - 1).speech);
            } else {
                speakAll();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        services().tts.stop();
        super.onPause();
    }
}
