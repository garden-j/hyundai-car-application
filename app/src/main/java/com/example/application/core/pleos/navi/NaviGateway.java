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

    /** 주행 중 이벤트(다음 회전, 주의 지점, 도착). null이면 받지 않는다. */
    interface DriveListener {
        void onUpcoming(java.util.List<com.example.application.domain.DriveGuide.Upcoming> upcoming);

        void onCautions(java.util.List<com.example.application.domain.DriveGuide.CautionPoint> cautions);

        void onArrived();

        /** 경유지(주유소·휴식 장소 등)에 도착. */
        void onWaypointArrived();
    }

    void setDriveListener(DriveListener listener);

    /** 안내 중인 경로에 경유지를 추가한다(휴식 장소·주유소). */
    void addWaypoint(com.example.application.model.Place place, Callback<Void> callback);

    /** 경로 옵션을 '편안한 길'로 바꿔 다시 탐색한다(비·야간 등 환경 변화 시). */
    void useComfortableRoute();
}
