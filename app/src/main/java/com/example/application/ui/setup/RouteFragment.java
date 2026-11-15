package com.example.application.ui.setup;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.model.RouteCandidate;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Resource;
import com.example.application.ui.common.RoutePreviewView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 이 길은 어떠세요? — 추천 경로 최대 3개(시간·거리·이유·난이도 근거) 비교 후 선택.
 * "다음"을 누르면 선택한 경로를 Pleos 내비게이션에 전달한다.
 */
public class RouteFragment extends BaseSetupFragment {
    private RoutePreviewView preview;
    private android.widget.Button naviPreview;
    /** 내비 경로 전체 보기를 띄웠는지. 앱으로 돌아오면 안내를 취소한다(주행은 출발 화면에서 시작). */
    private boolean previewingInNavi;

    private LinearLayout cardArea;
    private TextView status;
    private final List<View> cards = new ArrayList<>();
    private List<RouteCandidate> routes = new ArrayList<>();

    @Override
    protected SetupStep step() {
        return SetupStep.ROUTE;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("이 길은 어떠세요?", "연습에 맞는 길을 세 가지 골랐어요.");
        enableVoice("첫 번째로 할게");
        // 내비 안내는 모든 설정을 마친 뒤 출발 화면의 '주행 시작'에서 시작한다
        setPrimary("이 길로 할게요", this::goNext);

        LinearLayout list = scrollList(inflater);
        // 고른 경로 미리보기: 상세 버전은 실제 도로 모양, 기본 버전은 들를 지점만 보여 주는 개략도
        preview = new RoutePreviewView(requireContext());
        preview.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Choices.dp(requireContext(), 300));
        previewLp.topMargin = Choices.dp(requireContext(), 40);
        list.addView(preview, previewLp);
        // 기본 버전: 앱에서 실제 도로를 그릴 수 없어 내비 화면에서 확인만 하고 돌아온다
        naviPreview = new android.widget.Button(requireContext(), null, 0, R.style.Button_Secondary);
        naviPreview.setText("내비에서 경로 보기");
        naviPreview.setOnClickListener(v -> showInNavi());
        LinearLayout.LayoutParams naviLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.touch_min));
        naviLp.topMargin = Choices.dp(requireContext(), 40);
        list.addView(naviPreview, naviLp);
        list.addView(Choices.sectionTitle(requireContext(), "추천 경로"));
        status = new TextView(requireContext());
        status.setTextAppearance(R.style.Text_Body);
        status.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
        list.addView(status);
        cardArea = new LinearLayout(requireContext());
        cardArea.setOrientation(LinearLayout.VERTICAL);
        list.addView(cardArea);

        setupVm().routes().observe(getViewLifecycleOwner(), this::render);
        setupVm().loadRoutes(false);
        return null;
    }

    private void render(Resource<List<RouteCandidate>> res) {
        if (res == null) {
            return;
        }
        if (res.state == Resource.State.LOADING) {
            setStatus("연습하기 좋은 길을 찾고 있어요…");
            cardArea.removeAllViews();
            setPrimaryEnabled(false);
            return;
        }
        if (res.state == Resource.State.ERROR) {
            setStatus(res.error);
            setPrimaryEnabled(false);
            askOrigin();
            return;
        }
        routes = res.data;
        setStatus(setup().isHighTension() ? "긴장도를 반영해 쉬운 길을 먼저 보여 드려요." : null);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        cardArea.removeAllViews();
        cards.clear();
        for (int i = 0; i < routes.size(); i++) {
            RouteCandidate r = routes.get(i);
            View card = inflater.inflate(R.layout.item_option_card, cardArea, false);
            ((TextView) card.findViewById(R.id.card_title)).setText(r.title);
            ((TextView) card.findViewById(R.id.card_desc)).setText(r.reason + "\n"
                    + r.minutes + "분 · " + String.format(Locale.KOREA, "%.1f km", r.distanceKm));
            int index = i;
            card.setOnClickListener(x -> select(index));
            cards.add(card);
        }
        Choices.addGrid(cardArea, cards, Choices.isWide(requireContext()) ? 3 : 1);
        int selected = routes.indexOf(setup().selectedRoute);
        select(selected >= 0 ? selected : 0);
    }

    /** 선택한 경로를 내비의 경로 전체 보기로 띄운다. 돌아오면(직접 또는 20초 뒤 자동) onStart에서 안내를 취소한다. */
    private void showInNavi() {
        RouteCandidate r = setup().selectedRoute;
        if (r == null) {
            return;
        }
        naviPreview.setEnabled(false);
        setStatus("내비게이션에서 경로를 여는 중이에요…");
        // 앱이 앞에 있을 때 시작해야 한다: 20초 뒤 안내를 취소하고 앱으로 돌아오게 하는 서비스
        com.example.application.core.pleos.navi.NaviPreviewService.start(requireContext());
        previewingInNavi = true;
        services().navi.showRouteOverview(r, new com.example.application.core.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                previewingInNavi = true;
                if (isAdded()) {
                    naviPreview.setEnabled(true);
                    setStatus("경로를 보여 드린 뒤 20초 후 이 화면으로 돌아와요.");
                }
            }

            @Override
            public void onError(Exception e) {
                // 경로가 설정됐다면 내비 화면에는 떠 있으므로, 돌아오면 취소한다
                previewingInNavi = true;
                if (isAdded()) {
                    naviPreview.setEnabled(true);
                    setStatus(null);
                    toast("내비게이션에서 경로를 열지 못했어요. " + e.getMessage());
                }
            }
        });
    }

    /** 앱 화면이 다시 보이면(직접 돌아오거나 20초 뒤 자동 복귀) 내비 안내를 취소한다. */
    @Override
    public void onStart() {
        super.onStart();
        com.example.application.core.pleos.navi.NaviPreviewService.stop(requireContext());
        if (previewingInNavi) {
            previewingInNavi = false;
            services().navi.cancelRoute();
            setStatus("경로를 확인했어요. 안내는 출발 화면에서 '주행 시작'을 누르면 시작돼요.");
        }
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        status.setPadding(0, 0, 0, text == null ? 0 : Choices.dp(requireContext(), 16));
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
        // 상세 버전만 실제 도로 모양을 보여 준다. 기본 버전은 그림 대신 '내비에서 경로 보기'
        boolean real = !r.isEstimated();
        preview.setVisibility(real ? View.VISIBLE : View.GONE);
        naviPreview.setVisibility(real ? View.GONE : View.VISIBLE);
        if (real) {
            preview.setRoute(r.previewPath, null);
        }
        for (int i = 0; i < cards.size(); i++) {
            Choices.setOptionSelected(cards.get(i), i == index);
        }
        setPrimaryEnabled(true);
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
