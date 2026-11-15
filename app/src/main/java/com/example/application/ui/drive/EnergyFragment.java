package com.example.application.ui.drive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.core.Callback;
import com.example.application.core.drive.DriveSession;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.databinding.ItemRowBinding;
import com.example.application.domain.FuelGuide;
import com.example.application.domain.Josa;
import com.example.application.model.GeoPoint;
import com.example.application.model.Place;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;
import com.example.application.ui.common.Rows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 주유·충전 안내: 가까운 주유소(전기차는 충전소)를 찾아 경유지로 넣고,
 * 도착하면 주행 안내 서비스가 절차를 단계별로 읽어 준다.
 */
public class EnergyFragment extends BaseStepFragment {
    private LinearLayout list;
    private TextView status;

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        boolean ev = DriveSession.get().isEv();
        setSection("주행");
        setTitle(ev ? "가까운 충전소예요" : "가까운 주유소예요", "고르면 지금 경로에 경유지로 넣어 드려요.");
        setPrimary("돌아가기", () -> nav().back());
        showNav(true, false);
        list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        list.addView(Choices.sectionTitle(requireContext(), ev ? "충전소" : "주유소"));
        status = new TextView(requireContext());
        status.setTextAppearance(R.style.Text_Body);
        status.setText("찾고 있어요…");
        list.addView(status);
        LinearLayout results = new LinearLayout(requireContext());
        results.setOrientation(LinearLayout.VERTICAL);
        list.addView(results);
        list.addView(Choices.sectionTitle(requireContext(), "도착하면 이렇게 해요"));
        List<String> steps = FuelGuide.procedure(ev);
        for (int i = 0; i < steps.size(); i++) {
            Rows.add(list, String.valueOf(i + 1), steps.get(i), null, null);
        }
        services().location.lastKnownLocation(new Callback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint here) {
                search(results, here, ev);
            }

            @Override
            public void onError(Exception e) {
                search(results, null, ev);
            }
        });
        return null;
    }

    private void search(LinearLayout results, GeoPoint near, boolean ev) {
        services().places.search(FuelGuide.searchKeyword(ev), near, Callback.success(found -> {
            if (!isAdded()) {
                return;
            }
            List<Place> stations = new ArrayList<>();
            for (Place p : found) {
                if (p.point != null && (p.name.contains("주유") || p.name.contains("충전") || p.name.contains("오일")
                        || p.name.contains("에너지") || p.name.contains("SK") || p.name.contains("GS"))) {
                    stations.add(p);
                }
            }
            if (stations.isEmpty()) {
                status.setText("주변 시설을 찾지 못했어요. 상세 버전에서는 카카오 장소 검색으로 찾아 드려요. 내비게이션에서 직접 검색할 수도 있어요.");
                return;
            }
            if (near != null) {
                // 검색 결과는 정확도순이라 가까운 순으로 다시 정렬한다
                stations.sort(Comparator.comparingDouble(p -> near.distanceKmTo(p.point)));
            }
            status.setVisibility(View.GONE);
            for (int i = 0; i < Math.min(5, stations.size()); i++) {
                Place p = stations.get(i);
                String km = near == null ? null : String.format(Locale.KOREA, "%.1fkm", near.distanceKmTo(p.point));
                ItemRowBinding row = Rows.add(results, null, p.name, p.address, km);
                row.getRoot().setOnClickListener(v -> addStop(p));
            }
        }));
    }

    private void addStop(Place p) {
        services().navi.addWaypoint(p, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                DriveSession.get().setWaypointPurpose(DriveSession.WaypointPurpose.ENERGY);
                DriveSession.get().update(s -> s.energyAlert = null);
                speak(Josa.eulReul(p.name) + " 경유지로 넣었어요. 도착하면 순서대로 알려 드릴게요.");
                if (isAdded()) {
                    nav().back();
                }
            }

            @Override
            public void onError(Exception e) {
                toast("경유지를 넣지 못했어요. 내비게이션에서 직접 추가해 주세요.");
            }
        });
    }
}
