package com.example.application.core.pleos.location;

import com.example.application.core.Callback;
import com.example.application.model.GeoPoint;

/** FusedLocation SDK 래퍼. */
public interface LocationGateway {
    void connect();

    void release();

    boolean isSimulated();

    /** 마지막으로 알려진 위치. 위치를 모르면 onError. */
    void lastKnownLocation(Callback<GeoPoint> callback);
}
