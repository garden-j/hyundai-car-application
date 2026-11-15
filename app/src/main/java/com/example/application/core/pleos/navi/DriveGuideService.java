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

import com.example.application.R;
import com.example.application.core.Callback;
import com.example.application.core.drive.DriveSession;
import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.core.pleos.vehicle.VehicleStatus;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.DriveGuide;
import com.example.application.domain.EnvironmentWatch;
import com.example.application.domain.FuelGuide;
import com.example.application.domain.RelaxGuide;
import com.example.application.model.GuidanceLevel;

import java.util.List;

/**
 * 주행 중 음성 안내 서비스. 내비가 앞에 있는 동안에도 앱이 멈추지 않고
 * 내비 이벤트(다음 회전·주의 지점·도착)와 차량 상태(잔량·와이퍼·전조등)를 받아
 * 설정한 안내 수준으로 말하고 {@link DriveSession}을 갱신한다.
 * 출발 화면의 '주행 시작'에서 시작하고 주행을 마치면 끝낸다.
 */
public class DriveGuideService extends Service {
    private static final String TAG = "DriveGuide";
    private static final String CHANNEL = "drive_guide";
    private static final int NOTIFICATION_ID = 1002;
    private static final long CHECK_INTERVAL_MS = 30_000;
    /** 어려운 구간(유턴·합류·회전교차로)을 이 시간 안에 두 번 지나면 Relax 한마디. */
    private static final long HARD_WINDOW_MS = 3 * 60_000;

    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_GUIDE = "guide";
    private static final String EXTRA_RELAX = "relax";
    private static final String EXTRA_ENCOURAGE = "encourage";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodic = this::checkVehicle;
    private final EnvironmentWatch envWatch = new EnvironmentWatch();
    private DriveGuide guide;
    private String lastFirstKey;
    private DriveGuide.Maneuver lastFirstManeuver;
    private double lastFirstDistance = -1;
    private long lastHardAt;
    private boolean energyAlerted;

    public static void start(Context context, String modeLabel, GuidanceLevel guide, GuidanceLevel relax,
                             GuidanceLevel encourage) {
        Intent i = new Intent(context, DriveGuideService.class)
                .putExtra(EXTRA_MODE, modeLabel)
                .putExtra(EXTRA_GUIDE, guide.name())
                .putExtra(EXTRA_RELAX, relax.name())
                .putExtra(EXTRA_ENCOURAGE, encourage.name());
        context.startForegroundService(i);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, DriveGuideService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "주행 안내", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("차차가 주행을 함께 안내하고 있어요")
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        GuidanceLevel level = level(intent, EXTRA_GUIDE);
        String mode = intent != null && intent.getStringExtra(EXTRA_MODE) != null ? intent.getStringExtra(EXTRA_MODE) : "연습 모드";
        guide = new DriveGuide(level);
        energyAlerted = false;
        lastFirstKey = null;
        DriveSession.get().begin(mode, level, level(intent, EXTRA_RELAX), level(intent, EXTRA_ENCOURAGE));
        Log.i(TAG, "drive guidance started, level=" + level);
        ServiceLocator.get().navi.setDriveListener(new NaviGateway.DriveListener() {
            @Override
            public void onUpcoming(List<DriveGuide.Upcoming> upcoming) {
                handleUpcoming(upcoming);
            }

            @Override
            public void onCautions(List<DriveGuide.CautionPoint> cautions) {
                sayUrgent(guide.onCautions(cautions));
            }

            @Override
            public void onArrived() {
                DriveSession.get().stats().arrived = true;
                DriveSession.get().update(s -> {
                    s.arrived = true;
                    s.headline = "목적지에 도착했어요";
                    s.tip = "안전하게 정차한 뒤 주행 마치기를 눌러 주세요";
                });
                sayUrgent(guide.onArrived());
            }

            @Override
            public void onWaypointArrived() {
                handleWaypointArrived();
            }
        });
        handler.removeCallbacks(periodic);
        handler.postDelayed(periodic, 5_000);
        return START_NOT_STICKY;
    }

    private static GuidanceLevel level(Intent intent, String key) {
        String v = intent == null ? null : intent.getStringExtra(key);
        return v == null ? GuidanceLevel.STANDARD : GuidanceLevel.valueOf(v);
    }

    private void handleUpcoming(List<DriveGuide.Upcoming> upcoming) {
        if (upcoming == null || upcoming.isEmpty()) {
            return;
        }
        DriveGuide.Upcoming first = upcoming.get(0);
        String key = first.maneuver + ":" + first.description;
        if (lastFirstKey != null && !lastFirstKey.equals(key) && lastFirstDistance >= 0 && lastFirstDistance < 200) {
            // 앞서 안내하던 회전을 지나갔다 → 이번 주행에서 겪은 회전으로 센다
            DriveSession.get().stats().countManeuver(lastFirstManeuver);
            onPassed(lastFirstManeuver);
        }
        lastFirstKey = key;
        lastFirstManeuver = first.maneuver;
        lastFirstDistance = first.distanceM;
        DriveSession.get().update(s -> {
            s.maneuver = first.maneuver;
            s.distanceM = first.distanceM;
            s.headline = DriveGuide.label(first.maneuver);
            s.tip = first.description.isEmpty() ? null : first.description;
        });
        say(guide.onUpcoming(upcoming));
    }

    /** 어려운 구간을 연달아 지나면 짧은 Relax 한마디. */
    private void onPassed(DriveGuide.Maneuver m) {
        boolean hard = m == DriveGuide.Maneuver.U_TURN || m == DriveGuide.Maneuver.MERGE
                || m == DriveGuide.Maneuver.ROUNDABOUT;
        if (!hard) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastHardAt > 0 && now - lastHardAt < HARD_WINDOW_MS) {
            say(RelaxGuide.afterHardSection(DriveSession.get().relaxLevel()));
            lastHardAt = 0;
        } else {
            lastHardAt = now;
        }
    }

    private void handleWaypointArrived() {
        DriveSession session = DriveSession.get();
        if (session.waypointPurpose() == DriveSession.WaypointPurpose.ENERGY) {
            List<String> steps = FuelGuide.procedure(session.isEv());
            session.update(s -> {
                s.procedure = steps;
                s.energyAlert = null;
                s.headline = session.isEv() ? "충전소에 도착했어요" : "주유소에 도착했어요";
                s.tip = "화면의 순서대로 천천히 해요";
            });
            ServiceLocator.get().tts.speakAll(steps);
        } else if (session.waypointPurpose() == DriveSession.WaypointPurpose.REST) {
            session.update(s -> {
                s.headline = "쉴 곳에 도착했어요";
                s.tip = "기어를 P에 두고 편하게 쉬어요";
            });
            sayUrgent("쉴 곳에 도착했어요. 안전하게 세우고 기어를 P에 둔 뒤 편하게 쉬어요.");
        }
        session.setWaypointPurpose(DriveSession.WaypointPurpose.NONE);
    }

    /** 30초마다: 잔량 부족(한 번만), 비·야간 변화. */
    private void checkVehicle() {
        ServiceLocator sl = ServiceLocator.get();
        if (!energyAlerted) {
            sl.vehicle.readReadiness(new Callback<VehicleReadiness>() {
                @Override
                public void onSuccess(VehicleReadiness r) {
                    boolean ev = r.fuelPercent == null && r.evBatteryLevel != null;
                    DriveSession.get().setEv(ev);
                    Integer pct = r.fuelPercent != null ? r.fuelPercent
                            : r.evBatteryLevel != null && r.evBatteryLevel <= 100 ? Math.round(r.evBatteryLevel) : null;
                    if (FuelGuide.needsEnergy(pct, r.rangeKm)) {
                        energyAlerted = true;
                        String text = FuelGuide.alert(ev, pct, r.rangeKm);
                        DriveSession.get().update(s -> s.energyAlert = text);
                        say(text);
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
        sl.vehicle.readStatus(new Callback<VehicleStatus>() {
            @Override
            public void onSuccess(VehicleStatus st) {
                EnvironmentWatch.Change c = envWatch.update(st.wiperOn, st.lowBeamOn);
                if (c != null) {
                    DriveSession.get().update(s -> s.envChange = c);
                    say(EnvironmentWatch.message(c, DriveSession.get().guidance()));
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
        handler.postDelayed(periodic, CHECK_INTERVAL_MS);
    }

    /** 일반 안내: 말하던 것 뒤에 잇는다. */
    private void say(String text) {
        if (text != null) {
            Log.i(TAG, "say: " + text);
            DriveSession.get().update(s -> s.lastSpoken = text);
            ServiceLocator.get().tts.enqueue(text);
        }
    }

    /** 안전 안내(주의 지점·도착): 말하던 것을 멈추고 바로 말한다. */
    private void sayUrgent(String text) {
        if (text != null) {
            Log.i(TAG, "say(urgent): " + text);
            DriveSession.get().update(s -> s.lastSpoken = text);
            ServiceLocator.get().tts.speak(text);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(periodic);
        ServiceLocator.get().navi.setDriveListener(null);
        DriveSession.get().end();
        Log.i(TAG, "drive guidance stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
