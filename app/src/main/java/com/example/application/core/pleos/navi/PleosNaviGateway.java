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
import ai.pleos.playground.navi.constants.NavigationStatus;
import ai.pleos.playground.navi.constants.RouteOption;
import ai.pleos.playground.navi.constants.WaypointIndex;
import ai.pleos.playground.navi.data.RequestWaypointInfo;
import ai.pleos.playground.navi.constants.CautionPointType;
import ai.pleos.playground.navi.constants.TurnType;
import ai.pleos.playground.navi.data.CautionPoint;
import ai.pleos.playground.navi.data.DestinationArrivedInfo;
import ai.pleos.playground.navi.data.NavigationScreenRequest;
import ai.pleos.playground.navi.data.TBTInfo;
import ai.pleos.playground.navi.data.WaypointArrivedInfo;
import com.example.application.domain.DriveGuide;
import ai.pleos.playground.navi.data.RouteInfo;
import ai.pleos.playground.navi.data.RouteOverviewShowInfo;
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

    private volatile DriveListener driveListener;
    /**
     * 내비가 지금 경로를 안내 중인지(취소를 보내도 되는지). 내비가 알려 주는 상태(onNavigationStatus)를 따른다.
     * onRouteEnded·onRouteCancelled는 경로를 시작하거나 경유지를 넣을 때도 함께 와서(이전 경로 정리) 기준으로 쓰지 않는다.
     */
    private volatile boolean routeActive;

    private final NaviHelperEventListener listener = new NaviHelperEventListener() {
        @Override
        public void onTBTInfo(List<TBTInfo> list) {
            DriveListener l = driveListener;
            if (l == null || list == null) {
                return;
            }
            List<DriveGuide.Upcoming> out = new ArrayList<>();
            for (TBTInfo t : list) {
                out.add(new DriveGuide.Upcoming(maneuver(t.getType()), t.getDistance(), t.getDescription()));
            }
            MainThread.post(() -> l.onUpcoming(out));
        }

        @Override
        public void onCautionPointInfo(List<CautionPoint> list) {
            DriveListener l = driveListener;
            if (l == null || list == null) {
                return;
            }
            List<DriveGuide.CautionPoint> out = new ArrayList<>();
            for (CautionPoint c : list) {
                out.add(new DriveGuide.CautionPoint(c.getId(), caution(c.getType()), c.getRemainDistance()));
            }
            MainThread.post(() -> l.onCautions(out));
        }

        @Override
        public void onWayPointArrived(WaypointArrivedInfo info) {
            DriveListener l = driveListener;
            if (l != null) {
                MainThread.post(l::onWaypointArrived);
            }
        }

        @Override
        public void onDestinationArrived(DestinationArrivedInfo info) {
            routeActive = false;
            DriveListener l = driveListener;
            if (l != null) {
                MainThread.post(l::onArrived);
            }
        }

        @Override
        public void onRouteStarted(RouteStartInfo routeStartInfo) {
            routeActive = true;
            MainThread.post(() -> finish(null));
        }

        @Override
        public void onNavigationStatus(NavigationStatus status) {
            Log.i(TAG, "navigation status " + status);
            if (status != null) {
                routeActive = status == NavigationStatus.ROUTE_DRIVE;
            }
        }

        @Override
        public void onRoutingCompleted() {
            routeActive = true;
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
    public void showRouteOverview(RouteCandidate route, Callback<Void> callback) {
        startRoute(route, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                try {
                    navi.requestNavigationScreen(
                            new NavigationScreenRequest.OverviewRoute(new RouteOverviewShowInfo()));
                    callback.onSuccess(null);
                } catch (RuntimeException e) {
                    Log.w(TAG, "overview request failed", e);
                    callback.onError(e);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    @Override
    public void addWaypoint(Place place, Callback<Void> callback) {
        try {
            navi.addWaypoint(new RequestWaypointInfo(place.point.latitude, place.point.longitude, place.name,
                    WaypointIndex.FIRST, "", place.address == null ? "" : place.address, "", ""));
            callback.onSuccess(null);
        } catch (RuntimeException e) {
            Log.w(TAG, "addWaypoint failed", e);
            callback.onError(e);
        }
    }

    @Override
    public void useComfortableRoute() {
        try {
            navi.changeRouteOption(RouteOption.COMFORTABLE);
        } catch (RuntimeException e) {
            Log.w(TAG, "changeRouteOption failed", e);
        }
    }

    @Override
    public void setDriveListener(DriveListener listener) {
        driveListener = listener;
    }

    static DriveGuide.Maneuver maneuver(TurnType t) {
        if (t == null) {
            return DriveGuide.Maneuver.OTHER;
        }
        switch (t) {
            case TURN_LEFT:
            case TURN_SHARP_L:
                return DriveGuide.Maneuver.LEFT;
            case TURN_RIGHT:
            case TURN_SHARP_R:
                return DriveGuide.Maneuver.RIGHT;
            case TURN_UTURN:
                return DriveGuide.Maneuver.U_TURN;
            case TURN_KEEP_L:
                return DriveGuide.Maneuver.KEEP_LEFT;
            case TURN_KEEP_R:
                return DriveGuide.Maneuver.KEEP_RIGHT;
            case TURN_ENTER_IC_R:
            case TURN_ENTER_IC_L:
            case TURN_ENTER_IC_C:
            case TURN_DEVIDE_MERGE:
                return DriveGuide.Maneuver.MERGE;
            case TURN_EXIT_IC_R:
            case TURN_EXIT_IC_L:
            case TURN_EXIT_IC_C:
                return DriveGuide.Maneuver.EXIT;
            case TURN_ENTER_ROUNDABOUT_02:
            case TURN_ENTER_ROUNDABOUT_03:
            case TURN_ENTER_ROUNDABOUT_04:
            case TURN_ENTER_ROUNDABOUT_06:
            case TURN_ENTER_ROUNDABOUT_08:
            case TURN_ENTER_ROUNDABOUT_09:
            case TURN_ENTER_ROUNDABOUT_10:
            case TURN_ENTER_ROUNDABOUT_12:
                return DriveGuide.Maneuver.ROUNDABOUT;
            case TURN_STRAIGHT:
                return DriveGuide.Maneuver.STRAIGHT;
            case TURN_ARRIVE:
                return DriveGuide.Maneuver.ARRIVE;
            default:
                return DriveGuide.Maneuver.OTHER;
        }
    }

    static DriveGuide.Caution caution(CautionPointType t) {
        if (t == null) {
            return DriveGuide.Caution.OTHER;
        }
        switch (t) {
            case SCHOOL_ZONE:
                return DriveGuide.Caution.SCHOOL_ZONE;
            case SPEED_BUMP:
                return DriveGuide.Caution.SPEED_BUMP;
            case ACCIDENT_PRONE_AREA:
                return DriveGuide.Caution.ACCIDENT_PRONE;
            case NARROW_ROAD:
                return DriveGuide.Caution.NARROW_ROAD;
            case SLIPPERY_ROAD_ALERT:
                return DriveGuide.Caution.SLIPPERY;
            case DOWNHILL_CAUTION_AREA:
                return DriveGuide.Caution.DOWNHILL;
            case RAILROAD_CROSSING:
                return DriveGuide.Caution.RAILROAD;
            default:
                return DriveGuide.Caution.OTHER;
        }
    }

    @Override
    public void cancelRoute() {
        // 안내 중인 경로가 없을 때 취소를 보내면 내비가 NOT_CANCEL_ABLE_TRIP으로 실패하면서
        // 내부 상태가 멈춰, 이후 내비 화면의 '안내 종료'가 동작하지 않는다(에뮬레이터 v2.1.0에서 재현).
        // 그래서 실제로 안내 중일 때만 보낸다.
        if (!routeActive) {
            Log.i(TAG, "cancelRoute skipped: no active route");
            return;
        }
        routeActive = false;
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
