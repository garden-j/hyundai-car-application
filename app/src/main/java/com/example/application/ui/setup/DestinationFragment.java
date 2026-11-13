package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.DestinationGoal;
import com.example.application.model.Place;
import com.example.application.ui.common.Choices;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** ③ 어디까지 가 볼까요? — 출발지·목적지, 자주 가는 곳, 편도/왕복. */
public class DestinationFragment extends BaseSetupFragment {
    private final Set<String> frequentSelected = new LinkedHashSet<>();
    private final Set<String> tripSelected = new LinkedHashSet<>();
    private Runnable refreshFrequent;
    private Runnable refreshTrip;
    private TextView originValue;
    private TextView destValue;

    @Override
    protected SetupStep step() {
        return SetupStep.DESTINATION;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("어디까지\n가 볼까요?", "가고 싶은 곳을 알려 주세요.");
        enableVoice("집에서 마트까지 왕복");
        setPrimary("다음", this::goNext);
        if (setup().destination == null) {
            setup().destination = new DestinationGoal();
        }
        LinearLayout list = scrollList(inflater);
        originValue = placeRow(list, "출발지", v -> pickPlace("출발지 바꾸기", true, p -> {
            setup().destination.origin = p;
            render();
        }));
        destValue = placeRow(list, "목적지", v -> pickPlace("목적지 바꾸기", false, p -> {
            setup().destination.destination = p;
            render();
        }));

        Map<String, Place> frequent = services().places.frequentPlaces();
        refreshFrequent = Choices.addChipSection(list, "자주 가는 곳", new ArrayList<>(frequent.keySet()),
                s -> s, frequentSelected, true, () -> {
                    if (!frequentSelected.isEmpty()) {
                        setup().destination.destination = frequent.get(frequentSelected.iterator().next());
                    }
                    render();
                });
        refreshTrip = Choices.addChipSection(list, "어떻게 다녀올까요?", Arrays.asList("편도", "왕복"),
                s -> s, tripSelected, true, () -> {
                    setup().destination.roundTrip = tripSelected.contains("왕복");
                    render();
                });
        render();
        return null;
    }

    private TextView placeRow(LinearLayout list, String label, View.OnClickListener onChange) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, Choices.dp(requireContext(), 8), 0, Choices.dp(requireContext(), 8));
        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView l = new TextView(requireContext());
        l.setTextAppearance(R.style.Text_Caption);
        l.setText(label);
        TextView value = new TextView(requireContext());
        value.setTextAppearance(R.style.Text_Headline);
        texts.addView(l);
        texts.addView(value);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton change = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        change.setText("바꾸기");
        change.setOnClickListener(onChange);
        row.addView(change);
        list.addView(row);
        return value;
    }

    private void render() {
        DestinationGoal d = setup().destination;
        originValue.setText(d.origin == null ? "현재 위치" : d.origin.name);
        destValue.setText(d.destination == null ? "골라 주세요" : d.destination.name);
        frequentSelected.clear();
        for (Map.Entry<String, Place> e : services().places.frequentPlaces().entrySet()) {
            if (e.getValue() == d.destination) {
                frequentSelected.add(e.getKey());
            }
        }
        tripSelected.clear();
        tripSelected.add(d.roundTrip ? "왕복" : "편도");
        refreshFrequent.run();
        refreshTrip.run();
    }

    /** 장소 검색 대화상자. 주행 전(정차 중) 화면이므로 짧은 텍스트 입력을 허용한다. */
    private void pickPlace(String title, boolean allowCurrent, Consumer<Place> onPicked) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Choices.dp(requireContext(), 16);
        box.setPadding(pad, pad, pad, 0);
        EditText query = new EditText(requireContext());
        query.setHint("장소 이름 검색");
        ListView results = new ListView(requireContext());
        box.addView(query);
        box.addView(results, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Choices.dp(requireContext(), 280)));

        List<Place> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_2,
                android.R.id.text1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Place p = items.get(position);
                ((TextView) v.findViewById(android.R.id.text1)).setText(p == null ? "현재 위치" : p.name);
                ((TextView) v.findViewById(android.R.id.text2)).setText(p == null ? "차량 위치에서 출발" : p.address);
                return v;
            }
        };
        results.setAdapter(adapter);
        Runnable search = () -> {
            items.clear();
            if (allowCurrent) {
                items.add(null);
            }
            items.addAll(services().places.search(query.getText().toString()));
            adapter.clear();
            for (int i = 0; i < items.size(); i++) {
                adapter.add("");
            }
        };
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                search.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        search.run();
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(title).setView(box).setNegativeButton("닫기", null).create();
        results.setOnItemClickListener((parent, view, position, id) -> {
            onPicked.accept(items.get(position));
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    protected boolean onVoiceText(String text) {
        Map<String, Place> frequent = services().places.frequentPlaces();
        String norm = VoiceCommandParser.normalize(text);
        // "집에서 마트까지" → 출발지 집, 목적지 마트
        String from = null;
        String to = null;
        int fromIdx = norm.indexOf("에서");
        for (Map.Entry<String, String> e : VoiceVocabulary.PLACES.entrySet()) {
            for (String kw : e.getKey().split(",")) {
                int idx = norm.indexOf(kw);
                if (idx < 0) {
                    continue;
                }
                if (fromIdx >= 0 && idx < fromIdx) {
                    from = e.getValue();
                } else {
                    to = e.getValue();
                }
            }
        }
        boolean round = norm.contains("왕복") || norm.contains("다녀");
        boolean oneWay = norm.contains("편도");
        if (to == null && from == null && !round && !oneWay) {
            return false;
        }
        if (from != null && frequent.containsKey(from)) {
            setup().destination.origin = frequent.get(from);
        }
        if (to != null && frequent.containsKey(to)) {
            setup().destination.destination = frequent.get(to);
        }
        if (round || oneWay) {
            setup().destination.roundTrip = round;
        }
        render();
        return true;
    }
}
