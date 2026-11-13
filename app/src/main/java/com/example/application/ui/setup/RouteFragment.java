package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.domain.RouteDifficultyScorer;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.RouteCandidate;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Resource;
import com.example.application.ui.common.RoutePreviewView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 이 길은 어떠세요? — 추천 경로 최대 3개(시간·거리·이유·난이도 근거) 비교 후 선택.
 * "다음"을 누르면 선택한 경로를 Pleos 내비게이션에 전달한다.
 */
public class RouteFragment extends BaseSetupFragment {
    private RoutePreviewView preview;
    private LinearLayout cardRow;
    private TextView status;
    private TextView factors;
    private final List<MaterialCardView> cards = new ArrayList<>();
    private List<RouteCandidate> routes = new ArrayList<>();

    @Override
    protected SetupStep step() {
        return SetupStep.ROUTE;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("이 길은\n어떠세요?", "연습에 알맞은 길을 골랐어요.");
        enableVoice("첫 번째 길로 할래");
        setPrimary("이 길로 갈게요", this::sendToNavi);
        setSecondary("다시 찾기", () -> setupVm().loadRoutes(true));

        LinearLayout list = scrollList(inflater);
        preview = new RoutePreviewView(requireContext());
        list.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Choices.dp(requireContext(), 200)));
        status = new TextView(requireContext());
        status.setTextAppearance(R.style.Text_Body);
        status.setPadding(0, Choices.dp(requireContext(), 12), 0, 0);
        list.addView(status);
        cardRow = new LinearLayout(requireContext());
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        list.addView(cardRow);
        factors = new TextView(requireContext());
        factors.setTextAppearance(R.style.Text_Caption);
        factors.setPadding(Choices.dp(requireContext(), 8), Choices.dp(requireContext(), 8), 0, 0);
        list.addView(factors);

        setupVm().routes().observe(getViewLifecycleOwner(), this::render);
        setupVm().naviRequest().observe(getViewLifecycleOwner(), this::onNaviResult);
        setupVm().loadRoutes(false);
        return null;
    }

    private void render(Resource<List<RouteCandidate>> res) {
        if (res == null) {
            return;
        }
        if (res.state == Resource.State.LOADING) {
            status.setText("연습하기 좋은 길을 찾고 있어요…");
            cardRow.removeAllViews();
            setPrimaryEnabled(false);
            return;
        }
        if (res.state == Resource.State.ERROR) {
            status.setText(res.error);
            setPrimaryEnabled(false);
            askOrigin();
            return;
        }
        routes = res.data;
        status.setText(setup().isHighTension() ? "긴장도를 반영해 쉬운 길을 먼저 보여드려요." : "");
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        cardRow.removeAllViews();
        cards.clear();
        for (int i = 0; i < routes.size(); i++) {
            RouteCandidate r = routes.get(i);
            View v = inflater.inflate(R.layout.item_route_card, cardRow, false);
            ((TextView) v.findViewById(R.id.route_no)).setText(String.valueOf(i + 1));
            ((TextView) v.findViewById(R.id.route_title)).setText(r.title);
            ((TextView) v.findViewById(R.id.route_reason)).setText(r.reason);
            ((TextView) v.findViewById(R.id.route_minutes)).setText(r.minutes + "분");
            ((TextView) v.findViewById(R.id.route_distance)).setText(
                    String.format(Locale.KOREA, "%.1f km", r.distanceKm));
            ((TextView) v.findViewById(R.id.route_level)).setText(
                    "난이도 " + RouteDifficultyScorer.levelLabel(r.difficultyScore) + " · " + r.difficultyScore + "점");
            MaterialCardView card = (MaterialCardView) v;
            int index = i;
            card.setOnClickListener(x -> select(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Choices.dp(requireContext(), 6), Choices.dp(requireContext(), 12),
                    Choices.dp(requireContext(), 6), 0);
            cardRow.addView(card, lp);
            cards.add(card);
        }
        int selected = routes.indexOf(setup().selectedRoute);
        select(selected >= 0 ? selected : 0);
    }

    /** 현재 위치를 모를 때(권한 거부 등) 출발지를 직접 고르게 한다. */
    private void askOrigin() {
        java.util.List<com.example.application.model.Place> places = services().places.search("");
        String[] names = new String[places.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = places.get(i).name;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("출발지를 골라 주세요")
                .setItems(names, (d, which) -> {
                    setup().manualOrigin = places.get(which);
                    setupVm().loadRoutes(true);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void select(int index) {
        if (index < 0 || index >= routes.size()) {
            return;
        }
        RouteCandidate r = routes.get(index);
        setup().selectedRoute = r;
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setChecked(i == index);
            cards.get(i).setStrokeWidth(Choices.dp(requireContext(), i == index ? 3 : 1));
        }
        preview.setRoute(r.previewPath, null);
        factors.setText("점수 근거: " + String.join(", ", r.scoreFactors));
        setPrimaryEnabled(true);
    }

    private void sendToNavi() {
        if (!check(com.example.application.domain.SetupFlow.validate(step(), setup()))) {
            return;
        }
        setupVm().sendRouteToNavi();
    }

    private void onNaviResult(Resource<Void> res) {
        if (res == null) {
            return;
        }
        switch (res.state) {
            case LOADING:
                setPrimaryEnabled(false);
                status.setText("내비게이션에 경로를 보내고 있어요…");
                break;
            case SUCCESS:
                setupVm().clearNaviRequest();
                setPrimaryEnabled(true);
                goNext();
                break;
            case ERROR:
                setupVm().clearNaviRequest();
                setPrimaryEnabled(true);
                status.setText("");
                new AlertDialog.Builder(requireContext())
                        .setTitle("내비게이션에 경로를 보내지 못했어요")
                        .setMessage(res.error + "\n브리핑은 계속 볼 수 있고, 내비게이션은 나중에 직접 설정할 수 있어요.")
                        .setPositiveButton("다시 시도", (d, w) -> setupVm().sendRouteToNavi())
                        .setNegativeButton("그대로 진행", (d, w) -> goNext())
                        .show();
                break;
        }
    }

    @Override
    protected boolean onVoiceText(String text) {
        Integer n = VoiceCommandParser.parseNumber(text);
        String norm = VoiceCommandParser.normalize(text);
        if (n == null) {
            if (norm.contains("첫")) {
                n = 1;
            } else if (norm.contains("넓은")) {
                n = indexOfKind(RouteCandidate.Kind.WIDE_EASY) + 1;
            } else if (norm.contains("최단") || norm.contains("빠른") || norm.contains("짧은")) {
                n = indexOfKind(RouteCandidate.Kind.SHORTEST) + 1;
            } else if (norm.contains("목표") || norm.contains("연습")) {
                n = indexOfKind(RouteCandidate.Kind.GOAL_FOCUSED) + 1;
            }
        }
        if (n == null || n < 1 || n > routes.size()) {
            return false;
        }
        select(n - 1);
        speak(n + "번 " + routes.get(n - 1).title + ". " + routes.get(n - 1).reason + ". 이 길로 갈까요?");
        return true;
    }

    private int indexOfKind(RouteCandidate.Kind kind) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).kind == kind) {
                return i;
            }
        }
        return -1;
    }
}
