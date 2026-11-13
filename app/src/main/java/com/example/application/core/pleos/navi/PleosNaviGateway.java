package com.example.application.core.pleos.navi;

import android.content.Context;
import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.model.Place;
import com.example.application.model.RouteCandidate;

import java.util.ArrayList;
import java.util.List;

import ai.pleos.playground.navi.constants.NaviErrorCode;
import ai.pleos.playground.navi.constants.RouteOption;
import ai.pleos.playground.navi.constants.WaypointIndex;
import ai.pleos.playground.navi.data.RequestWaypointInfo;
import ai.pleos.playground.navi.data.RouteInfo;
import ai.pleos.playground.navi.data.RouteStartInfo;
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest;
import ai.pleos.playground.navi.helper.NaviHelper;
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener;

/**
 * NaviHelper 실제 구현. 요청은 Pleos 내비 앱에 전달되고 결과는 이벤트로 돌아온다.
 * 응답이 없을 때를 대비해 시간 제한을 둔다.
 */
public class PleosNaviGateway implements NaviGateway {
    private static final String TAG = "PleosNavi";
    private static final long TIMEOUT_MS = 10_000;

    private final NaviHelper navi;
    private Callback<Void> pending;
    private final Runnable timeout = () -> finish(new IllegalStateException("내비게이션 응답이 없어요"));

    private final NaviHelperEventListener listener = new NaviHelperEventListener() {
        @Override
        public void onRouteStarted(RouteStartInfo routeStartInfo) {
            MainThread.post(() -> finish(null));
        }

        @Override
        public void onRoutingCompleted() {
            MainThread.post(() -> finish(null));
        }

        @Override
        public void onError(NaviErrorCode errorCode) {
            Log.w(TAG, "navi error " + errorCode);
            MainThread.post(() -> finish(new IllegalStateException(message(errorCode))));
        }
    };

    public PleosNaviGateway(Context context) {
        navi = new NaviHelper(context.getApplicationContext());
    }

    @Override
    public void connect() {
        navi.initialize();
        navi.addListener(listener);
    }

    @Override
    public void release() {
        navi.removeListener(listener);
        navi.release();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    public void startRoute(RouteCandidate route, Callback<Void> callback) {
        if (pending != null) {
            finish(new IllegalStateException("새 경로 요청으로 취소됨"));
        }
        pending = callback;
        MainThread.postDelayed(timeout, TIMEOUT_MS);
        try {
            RouteInfo destination = routeInfo(route.destination);
            if (route.waypoints.isEmpty()) {
                navi.requestRoute(destination);
            } else {
                List<RequestWaypointInfo> wps = new ArrayList<>();
                WaypointIndex[] idx = {WaypointIndex.FIRST, WaypointIndex.SECOND};
                for (int i = 0; i < Math.min(2, route.waypoints.size()); i++) {
                    Place p = route.waypoints.get(i);
                    wps.add(new RequestWaypointInfo(p.point.latitude, p.point.longitude, p.name, idx[i],
                            "", p.address, "", ""));
                }
                navi.requestRouteWithWaypoints(new RouteWithWaypointsRequest(destination, wps));
            }
        } catch (RuntimeException e) {
            finish(e);
        }
    }

    private static RouteInfo routeInfo(Place p) {
        return new RouteInfo(p.point.latitude, p.point.longitude, null, null,
                p.name, "", p.address, "", RouteOption.RECOMMENDED, "", false);
    }

    @Override
    public void cancelRoute() {
        navi.cancelRoute();
    }

    private void finish(Exception error) {
        MainThread.cancel(timeout);
        Callback<Void> cb = pending;
        pending = null;
        if (cb == null) {
            return;
        }
        if (error == null) {
            cb.onSuccess(null);
        } else {
            cb.onError(error);
        }
    }

    private static String message(NaviErrorCode code) {
        switch (code) {
            case CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE:
                return "목적지가 너무 가까워요";
            case CAUSE_SEARCH_ROUTE_OVER_DISTANCE:
                return "목적지가 너무 멀어요";
            case CAUSE_LOW_BATTERY:
                return "배터리가 부족해 경로를 찾을 수 없어요";
            default:
                return "경로를 찾지 못했어요";
        }
    }
}
