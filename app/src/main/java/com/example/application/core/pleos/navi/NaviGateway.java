package com.example.application.core.pleos.navi;

import com.example.application.core.Callback;
import com.example.application.model.RouteCandidate;

/** NaviHelper SDK 래퍼. 선택한 연습 경로를 Pleos 내비게이션 앱에 전달한다. */
public interface NaviGateway {
    void connect();

    void release();

    boolean isSimulated();

    /** 경로 안내 요청. 내비가 경로 탐색을 시작하면 onSuccess. */
    void startRoute(RouteCandidate route, Callback<Void> callback);

    void cancelRoute();

    /**
     * 내비게이션 앱에서 경로 전체 보기를 띄운다(실제 도로 경로 확인용).
     * 전체 보기는 설정된 경로가 있어야 하므로 경로를 먼저 보내고, 탐색이 끝나면 전체 보기 화면을 요청한다.
     */
    void showRouteOverview(RouteCandidate route, Callback<Void> callback);
}
