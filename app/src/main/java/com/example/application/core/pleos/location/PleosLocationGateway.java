package com.example.application.core.pleos.location;

import android.content.Context;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.model.GeoPoint;

import ai.pleos.playground.location.fused.FusedLocation;
import ai.pleos.playground.location.fused.listener.FusedLocationListener;
import ai.pleos.playground.location.fused.model.LocationData;

public class PleosLocationGateway implements LocationGateway {
    private static final String PERMISSION = "pleos.car.permission.FUSED_LOCATION";
    private final Context context;
    private final FusedLocation fused;
    private boolean supported;

    public PleosLocationGateway(Context context) {
        this.context = context.getApplicationContext();
        fused = new FusedLocation(this.context);
    }

    @Override
    public void connect() {
        fused.initialize();
        supported = fused.isSupported();
    }

    @Override
    public void release() {
        fused.release();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    public void lastKnownLocation(Callback<GeoPoint> callback) {
        if (!supported) {
            callback.onError(new UnsupportedOperationException("이 환경은 위치 기능을 지원하지 않아요"));
            return;
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, PERMISSION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 사용자가 차량 위치 접근을 막아 둔 경우 시스템 안내 팝업을 띄운다(Pleos 가이드)
            try {
                fused.showLocationAccessBlockedPopup();
            } catch (RuntimeException ignored) {
                // 팝업이 지원되지 않는 환경
            }
            callback.onError(new SecurityException("위치 권한이 없어 출발지를 직접 골라야 해요."));
            return;
        }
        try {
            requestLast(callback);
        } catch (SecurityException e) {
            callback.onError(new SecurityException("위치 권한이 없어 출발지를 직접 골라야 해요."));
        }
    }

    private void requestLast(Callback<GeoPoint> callback) {
        fused.getLastKnownLocation(new FusedLocationListener() {
            @Override
            public void onChanged(LocationData location) {
                android.util.Log.i("PleosLocation", "lastKnown=" + location);
                MainThread.post(() -> {
                    if (location == null) {
                        callback.onError(new IllegalStateException("현재 위치를 아직 알 수 없어요"));
                    } else {
                        callback.onSuccess(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                MainThread.post(() -> callback.onError(exception));
            }
        });
    }
}
