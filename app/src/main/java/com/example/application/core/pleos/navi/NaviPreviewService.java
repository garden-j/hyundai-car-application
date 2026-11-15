package com.example.application.core.pleos.navi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.application.MainActivity;
import com.example.application.R;
import com.example.application.di.ServiceLocator;

/**
 * 내비 '경로 보기' 동안 앱이 멈추지(freeze) 않도록 잠깐 실행하는 포그라운드 서비스(shortService).
 * 정해진 시간이 지나면 내비 안내를 취소하고 앱을 다시 앞으로 불러온다.
 * 사용자가 먼저 앱으로 돌아오면 화면이 {@link #stop}으로 끝낸다.
 */
public class NaviPreviewService extends Service {
    private static final String TAG = "NaviPreview";
    private static final String CHANNEL = "navi_preview";
    private static final int NOTIFICATION_ID = 1001;
    public static final long PREVIEW_MS = 20_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finishPreview;

    public static void start(Context context) {
        Intent i = new Intent(context, NaviPreviewService.class);
        context.startForegroundService(i);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, NaviPreviewService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "경로 보기", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("내비에서 경로를 보여 주고 있어요")
                .setContentText("잠시 후 차차로 돌아가요")
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        handler.removeCallbacks(finish);
        handler.postDelayed(finish, PREVIEW_MS);
        return START_NOT_STICKY;
    }

    private void finishPreview() {
        try {
            ServiceLocator.get().navi.cancelRoute();
        } catch (RuntimeException e) {
            Log.w(TAG, "cancelRoute failed", e);
        }
        try {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } catch (RuntimeException e) {
            Log.w(TAG, "could not bring app to front", e);
        }
        stopSelf();
    }

    @Override
    public void onTimeout(int startId) {
        // shortService 제한 시간(약 3분) 초과 시
        finishPreview();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(finish);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
