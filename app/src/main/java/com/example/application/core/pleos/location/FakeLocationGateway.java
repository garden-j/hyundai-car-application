package com.example.application.core.pleos.location;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.model.GeoPoint;

/** 위치 서비스가 없을 때: PT 시연 장소(현대자동차 강남대로사옥) 좌표를 현재 위치로 쓴다. */
public class FakeLocationGateway implements LocationGateway {
    public static final GeoPoint DEMO_LOCATION = new GeoPoint(37.4920, 127.0300);

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
    public void lastKnownLocation(Callback<GeoPoint> callback) {
        MainThread.postDelayed(() -> callback.onSuccess(DEMO_LOCATION), 100);
    }
}
