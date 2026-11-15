package com.example.application.core.pleos.navi;

import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.model.RouteCandidate;

/** 내비 서비스가 없을 때: 요청 내용을 로그로 남기고 성공으로 응답한다. */
public class FakeNaviGateway implements NaviGateway {
    private static final String TAG = "FakeNavi";

    @Override
    public void connect() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public void startRoute(RouteCandidate route, Callback<Void> callback) {
        Log.i(TAG, "startRoute → " + route.destination.name + " via " + route.waypoints.size() + " waypoint(s)");
        MainThread.postDelayed(() -> callback.onSuccess(null), 300);
    }

    @Override
    public void showRouteOverview(RouteCandidate route, Callback<Void> callback) {
        Log.i(TAG, "showRouteOverview (simulated)");
        MainThread.postDelayed(() -> callback.onSuccess(null), 300);
    }

    @Override
    public void cancelRoute() {
        Log.i(TAG, "cancelRoute");
    }
}
