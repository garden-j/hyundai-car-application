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
        if (setup().generalDrive) {
            setTitle("어디로 갈까요?", "목적지를 고르면 출발 전 준비를 함께 확인해요.");
        } else {
            setTitle("어디까지 가 볼까요?", "가고 싶은 곳을 알려 주세요.");
        }
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
        refreshFrequent = addQuickPicks(list, new ArrayList<>(frequent.keySet()), frequentSelected, () -> {
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

    /** Figma Field: 필드 제목 + 흰 카드(높이 100, 모서리 32) 안에 값과 파란 '바꾸기'. */
    private TextView placeRow(LinearLayout list, String label, View.OnClickListener onChange) {
        list.addView(Choices.sectionTitle(requireContext(), label));
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setMinimumHeight(Choices.dp(requireContext(), 100));
        int pad = Choices.dp(requireContext(), 32);
        row.setPadding(pad, 0, pad, 0);
        TextView value = new TextView(requireContext());
        value.setTextAppearance(R.style.Text_Body);
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView change = new TextView(requireContext());
        change.setTextAppearance(R.style.Text_Body);
        change.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.pleos_accent));
        change.setText("바꾸기");
        change.setPadding(Choices.dp(requireContext(), 16), Choices.dp(requireContext(), 24), 0, Choices.dp(requireContext(), 24));
        row.addView(change);
        row.setOnClickListener(onChange);
        change.setOnClickListener(onChange);
        list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return value;
    }

    /** Figma Quick Picks: 목적지 아래 작은 칩(높이 64, 간격 12). 선택 시 검은 배경. */
    private Runnable addQuickPicks(LinearLayout list, List<String> names, Set<String> selected, Runnable onChange) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        List<TextView> chips = new ArrayList<>();
        Runnable refresh = () -> {
            for (int i = 0; i < chips.size(); i++) {
                boolean on = selected.contains(names.get(i));
                chips.get(i).setSelected(on);
                chips.get(i).setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),
                        on ? R.color.white : R.color.text_primary));
            }
        };
        for (String name : names) {
            TextView chip = new TextView(requireContext());
            chip.setTextAppearance(R.style.Text_Caption);
            chip.setText(name);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setBackgroundResource(R.drawable.bg_quick_chip);
            int h = Choices.dp(requireContext(), 24);
            chip.setPadding(h, 0, h, 0);
            chip.setOnClickListener(v -> {
                selected.clear();
                selected.add(name);
                refresh.run();
                onChange.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    Choices.dp(requireContext(), 64));
            if (!chips.isEmpty()) {
                lp.setMarginStart(Choices.dp(requireContext(), 12));
            }
            row.addView(chip, lp);
            chips.add(chip);
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = Choices.dp(requireContext(), 12);
        list.addView(row, rowLp);
        refresh.run();
        return refresh;
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
        int[] requestSeq = {0};
        Runnable search = () -> {
            int seq = ++requestSeq[0];
            services().places.search(query.getText().toString(), setupVm().knownOrigin(),
                    com.example.application.core.Callback.success(found -> {
                if (seq != requestSeq[0] || !isAdded()) {
                    return; // 더 최근 검색어의 결과만 반영
                }
                items.clear();
                if (allowCurrent) {
                    items.add(null);
                }
                items.addAll(found);
                adapter.clear();
                for (int i = 0; i < items.size(); i++) {
                    adapter.add("");
                }
            }));
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
