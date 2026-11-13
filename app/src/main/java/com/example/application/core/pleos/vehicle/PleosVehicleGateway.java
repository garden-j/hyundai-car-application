package com.example.application.core.pleos.vehicle;

import android.content.Context;
import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.core.pleos.Kt;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.api.model.area.DoorArea;
import ai.pleos.playground.vehicle.api.model.area.SeatArea;
import ai.pleos.playground.vehicle.api.model.area.WindowArea;
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds;
import ai.pleos.playground.vehicle.constant.area.DoorValue;
import ai.pleos.playground.vehicle.constant.area.SeatValue;
import ai.pleos.playground.vehicle.constant.area.WindowValue;
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit;

/**
 * Vehicle SDK 실제 구현. 국내 좌핸들 차량 기준으로 운전석은 ROW_1_LEFT.
 * SDK 콜백은 IPC 스레드에서 오므로 결과는 모두 메인 스레드로 넘긴다.
 */
public class PleosVehicleGateway implements VehicleGateway {
    private static final String TAG = "PleosVehicle";

    private static final SeatArea DRIVER_SEAT = new SeatArea(SeatValue.ROW_1_LEFT);
    private static final WindowArea DRIVER_WINDOW = new WindowArea(WindowValue.ROW_1_LEFT);
    private static final WindowArea WINDSHIELD = new WindowArea(WindowValue.FRONT_WINDSHIELD);
    private static final DoorValue[] DOORS = {
            DoorValue.ROW_1_LEFT, DoorValue.ROW_1_RIGHT, DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT
    };

    private final Vehicle vehicle;
    /**
     * 공조 온도·풍량이 실제로 지원하는 영역(운전석 포함). 차량마다 다르다
     * (Pleos 에뮬레이터는 운전석 단독 0x1을 지원하지 않음) → capability로 조회해 캐시.
     */
    private SeatArea tempArea;
    private SeatArea fanArea;
    private SeatArea powerArea;
    private boolean hvacAreasResolved;

    public PleosVehicleGateway(Context context) {
        this.vehicle = new Vehicle(context.getApplicationContext());
    }

    @Override
    public void connect() {
        vehicle.initialize();
    }

    @Override
    public void release() {
        vehicle.release();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    /** 공조 영역을 한 번만 조회한 뒤 next를 실행한다(실패하면 운전석 단독 영역으로 시도). */
    private void withHvacAreas(Runnable next) {
        if (hvacAreasResolved) {
            next.run();
            return;
        }
        Join join = new Join(3, () -> {
            hvacAreasResolved = true;
            if (powerArea == null) {
                powerArea = fanArea != null ? fanArea : DRIVER_SEAT;
            }
            if (tempArea == null) {
                tempArea = DRIVER_SEAT;
            }
            if (fanArea == null) {
                fanArea = DRIVER_SEAT;
            }
            next.run();
        });
        vehicle.getHVAC().checkHvacTemperatureCapabilities(
                Kt.f2((config, bounds) -> join.done(() -> tempArea = driverArea(bounds))),
                Kt.f1(e -> join.failed("temperature capability", e)));
        vehicle.getHVAC().checkHvacFanSpeedCapabilities(
                Kt.f2((config, bounds) -> join.done(() -> fanArea = driverArea(bounds))),
                Kt.f1(e -> join.failed("fan capability", e)));
        vehicle.getHVAC().checkHvacPowerCapabilities(
                Kt.f2((config, areas) -> join.done(() -> powerArea = driverAreaOf(areas))),
                Kt.f1(e -> join.failed("power capability", e)));
    }

    private static SeatArea driverAreaOf(java.util.List<SeatArea> areas) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }
        java.util.Map<SeatArea, Object> m = new java.util.LinkedHashMap<>();
        for (SeatArea a : areas) {
            m.put(a, Boolean.TRUE);
        }
        return driverArea(m);
    }

    private static SeatArea driverArea(Map<SeatArea, ?> bounds) {
        if (bounds == null) {
            return null;
        }
        for (SeatArea area : bounds.keySet()) {
            for (SeatValue v : area.getValues()) {
                if (v == SeatValue.ROW_1_LEFT) {
                    return area;
                }
            }
        }
        return bounds.isEmpty() ? null : bounds.keySet().iterator().next();
    }

    @Override
    public void readSettings(Callback<VehicleSettings> callback) {
        withHvacAreas(() -> readSettingsNow(callback));
    }

    private void readSettingsNow(Callback<VehicleSettings> callback) {
        VehicleSettings result = new VehicleSettings();
        Join join = new Join(6, () -> callback.onSuccess(result));

        vehicle.getHVAC().getHvacPowerState(powerArea,
                Kt.f1(v -> join.done(() -> result.hvacPower = v)),
                Kt.f1(e -> join.failed("hvacPower", e)));
        vehicle.getHVAC().getHvacTemperature(tempArea, TemperatureUnit.CELSIUS,
                Kt.f1(v -> join.done(() -> result.cabinTemperature = v)),
                Kt.f1(e -> join.failed("temperature", e)));
        vehicle.getHVAC().getHvacFanSpeed(fanArea,
                Kt.f1(v -> join.done(() -> result.fanSpeed = v)),
                Kt.f1(e -> join.failed("fanSpeed", e)));
        vehicle.getWindow().getCurrentWindowPosition(DRIVER_WINDOW,
                Kt.f1(v -> join.done(() -> result.driverWindow = v)),
                Kt.f1(e -> join.failed("window", e)));
        vehicle.getSteeringWheel().getHvacSteeringWheelHeatState(
                Kt.f1(v -> join.done(() -> result.steeringWheelHeat = v)),
                Kt.f1(e -> join.failed("wheelHeat", e)));
        vehicle.getHVAC().getHvacDefrosterState(WINDSHIELD,
                Kt.f1(v -> join.done(() -> result.defroster = v)),
                Kt.f1(e -> join.failed("defroster", e)));
    }

    @Override
    public void applySetting(SettingKey key, Object value, Callback<Object> callback) {
        withHvacAreas(() -> applySettingNow(key, value, callback));
    }

    private void applySettingNow(SettingKey key, Object value, Callback<Object> callback) {
        java.util.function.Consumer<Exception> onFailed = e -> MainThread.post(() -> callback.onError(e));
        try {
            switch (key) {
                case HVAC_POWER:
                    vehicle.getHVAC().setHvacPower(powerArea, (Boolean) value,
                            Kt.f2((area, v) -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                case CABIN_TEMPERATURE:
                    vehicle.getHVAC().setHvacTemperature(tempArea, ((Number) value).floatValue(),
                            TemperatureUnit.CELSIUS,
                            Kt.f2((area, v) -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                case FAN_SPEED:
                    vehicle.getHVAC().setHvacFanSpeed(fanArea, ((Number) value).intValue(),
                            Kt.f2((area, v) -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                case DRIVER_WINDOW:
                    vehicle.getWindow().setWindowPosition(DRIVER_WINDOW, ((Number) value).intValue(),
                            Kt.f2((area, v) -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                case STEERING_WHEEL_HEAT:
                    vehicle.getSteeringWheel().setHvacSteeringWheelHeat(((Number) value).intValue(),
                            Kt.f1(v -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                case DEFROSTER:
                    vehicle.getHVAC().setHvacDefroster(WINDSHIELD, (Boolean) value,
                            Kt.f2((area, v) -> deliver(callback, v, value)), Kt.f1(onFailed));
                    break;
                default:
                    onFailed.accept(new IllegalArgumentException(key.name()));
            }
        } catch (RuntimeException e) {
            // 권한 누락 등 SDK가 동기적으로 던지는 예외
            onFailed.accept(e);
        }
    }

    private static void deliver(Callback<Object> callback, Object reported, Object requested) {
        MainThread.post(() -> callback.onSuccess(reported != null ? reported : requested));
    }

    @Override
    public void readFanSpeedRange(Callback<int[]> callback) {
        vehicle.getHVAC().checkHvacFanSpeedCapabilities(
                Kt.f2((config, bounds) -> {
                    AreaBounds<Integer> b = findBounds(bounds);
                    MainThread.post(() -> {
                        if (b == null) {
                            callback.onError(new IllegalStateException("fan speed bounds unavailable"));
                        } else {
                            callback.onSuccess(new int[]{b.getMinValue(), b.getMaxValue()});
                        }
                    });
                }),
                Kt.f1(e -> MainThread.post(() -> callback.onError(e))));
    }

    private static AreaBounds<Integer> findBounds(Map<SeatArea, AreaBounds<Integer>> bounds) {
        if (bounds == null || bounds.isEmpty()) {
            return null;
        }
        for (Map.Entry<SeatArea, AreaBounds<Integer>> e : bounds.entrySet()) {
            for (SeatValue v : e.getKey().getValues()) {
                if (v == SeatValue.ROW_1_LEFT) {
                    return e.getValue();
                }
            }
        }
        return bounds.values().iterator().next();
    }

    @Override
    public void readReadiness(Callback<VehicleReadiness> callback) {
        VehicleReadiness result = new VehicleReadiness();
        AtomicInteger openDoors = new AtomicInteger();
        AtomicInteger knownDoors = new AtomicInteger();
        Float[] evRaw = new Float[2]; // [잔량(Wh), 용량(Wh)]
        Join join = new Join(5 + DOORS.length, () -> {
            if (knownDoors.get() > 0) {
                result.allDoorsClosed = openDoors.get() == 0;
            }
            // EV_BATTERY_LEVEL은 Wh 단위 → 용량 대비 %로 변환
            if (evRaw[0] != null && evRaw[1] != null && evRaw[1] > 0) {
                result.evBatteryLevel = Math.min(100f, evRaw[0] / evRaw[1] * 100f);
            }
            callback.onSuccess(result);
        });

        vehicle.getSeat().getCurrentSeatBeltBuckledState(DRIVER_SEAT,
                Kt.f1(v -> join.done(() -> result.driverSeatBeltBuckled = v)),
                Kt.f1(e -> join.failed("seatBelt", e)));
        vehicle.getFuel().getFuelPercent(
                Kt.f1(v -> join.done(() -> result.fuelPercent = v)),
                Kt.f1(e -> join.failed("fuel", e)));
        vehicle.getEvBattery().getEvBatteryLevel(
                Kt.f1(v -> join.done(() -> evRaw[0] = v)),
                Kt.f1(e -> join.failed("evBattery", e)));
        vehicle.getEvBattery().getEvBatteryCapacity(
                Kt.f1(v -> join.done(() -> evRaw[1] = v)),
                Kt.f1(e -> join.failed("evCapacity", e)));
        vehicle.getOdometer().getRangeRemainingDistance(
                // VHAL RANGE_REMAINING 단위는 미터
                Kt.f1(v -> join.done(() -> result.rangeKm = v == null ? null : v / 1000f)),
                Kt.f1(e -> join.failed("range", e)));
        for (DoorValue door : DOORS) {
            vehicle.getDoor().getCurrentDoorPosition(new DoorArea(door),
                    Kt.f1(pos -> join.done(() -> {
                        if (pos != null) {
                            knownDoors.incrementAndGet();
                            if (pos > 0) {
                                openDoors.incrementAndGet();
                            }
                        }
                    })),
                    // 2도어 차량 등 없는 문은 실패로 온다 → 알 수 없음으로 처리
                    Kt.f1(e -> join.failed("door " + door, e)));
        }
    }

    /** 여러 비동기 읽기가 모두 끝나면(성공/실패 무관) 한 번만 완료 콜백을 부른다. */
    private static final class Join {
        private final AtomicInteger remaining;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final Runnable onAllDone;

        Join(int count, Runnable onAllDone) {
            this.remaining = new AtomicInteger(count);
            this.onAllDone = onAllDone;
        }

        void done(Runnable apply) {
            synchronized (this) {
                apply.run();
            }
            countDown();
        }

        void failed(String what, Exception e) {
            Log.w(TAG, "read " + what + " failed: " + e.getMessage());
            countDown();
        }

        private void countDown() {
            if (remaining.decrementAndGet() == 0 && finished.compareAndSet(false, true)) {
                MainThread.post(onAllDone);
            }
        }
    }
}
