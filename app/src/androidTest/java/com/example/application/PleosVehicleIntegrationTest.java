package com.example.application;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.application.core.Callback;
import com.example.application.core.pleos.PleosAvailability;
import com.example.application.core.pleos.vehicle.PleosVehicleGateway;
import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.core.vehiclecontrol.ApplyReport;
import com.example.application.core.vehiclecontrol.PresetController;
import com.example.application.domain.PresetTargets;
import com.example.application.model.PracticePreset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pleos Connect 에뮬레이터(또는 실차)에서 Vehicle SDK 연동을 검증한다.
 * Pleos 차량 서비스가 없는 기기에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4.class)
public class PleosVehicleIntegrationTest {
    private static final String TAG = "PleosIT";
    private PleosVehicleGateway vehicle;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue("Pleos vehicle service required", PleosAvailability.has(ctx, PleosAvailability.VEHICLE_SERVICE));
        vehicle = new PleosVehicleGateway(ctx);
        vehicle.connect();
    }

    @After
    public void tearDown() {
        if (vehicle != null) {
            vehicle.release();
        }
    }

    private static <T> T await(java.util.function.Consumer<Callback<T>> call) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        call.accept(new Callback<T>() {
            @Override
            public void onSuccess(T v) {
                value.set(v);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        assertTrue("timeout", latch.await(20, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        return value.get();
    }

    @Test
    public void readsReadiness() throws Exception {
        VehicleReadiness r = await(vehicle::readReadiness);
        Log.i(TAG, "belt=" + r.driverSeatBeltBuckled + " doors=" + r.allDoorsClosed + " fuel=" + r.fuelPercent
                + " ev=" + r.evBatteryLevel + " range=" + r.rangeKm);
        // 안전벨트: 에뮬레이터에서는 미착용 고정이라 null(직접 확인)로 바꿔 돌려준다

        assertNotNull("doors", r.allDoorsClosed);
        assertTrue("fuel or ev level", r.fuelPercent != null || r.evBatteryLevel != null);
    }

    /** 집중 주행 프리셋을 적용하고, 적용 전 설정으로 되돌린다. */
    @Test
    public void appliesFocusPresetAndRestores() throws Exception {
        PresetController controller = new PresetController(vehicle);
        Map<SettingKey, Object> targets = PresetTargets.resolve(PracticePreset.FOCUS, null, true);
        ApplyReport report = await(cb -> controller.apply(targets, cb));
        for (ApplyReport.Result res : report.results) {
            Log.i(TAG, res.key + " requested=" + res.requested + " ok=" + res.success + " " + res.message);
        }
        VehicleSettings after = await(vehicle::readSettings);
        Log.i(TAG, "after: power=" + after.hvacPower + " temp=" + after.cabinTemperature + " fan=" + after.fanSpeed
                + " window=" + after.driverWindow + " wheel=" + after.steeringWheelHeat);
        // 모든 설정 요청이 차량에 수락돼야 한다. (Pleos 에뮬레이터 2.1.0의 시뮬레이션 차량은
        // HVAC_POWER_ON 설정을 수락하지만 값을 바꾸지 않아, 읽어 온 값은 로그로만 확인한다)
        assertTrue("all settings accepted: " + report.failures().size() + " failed", report.failures().isEmpty());

        // 복원(연습 종료 흐름의 핵심 동작): 앱이 바꾼 항목만, 공조 전원은 다른 공조 항목 뒤에 되돌린다
        VehicleSettings before = report.before;
        for (SettingKey k : new SettingKey[]{SettingKey.CABIN_TEMPERATURE, SettingKey.FAN_SPEED, SettingKey.HVAC_POWER}) {
            Object original = before.get(k);
            if (original != null && !VehicleSettings.sameValue(k, original, report.applied.get(k))) {
                await(cb -> vehicle.applySetting(k, original, cb));
            }
        }
        VehicleSettings restored = await(vehicle::readSettings);
        Log.i(TAG, "restored: power=" + restored.hvacPower + " fan=" + restored.fanSpeed);
        assertTrue(VehicleSettings.sameValue(SettingKey.FAN_SPEED, before.fanSpeed, restored.fanSpeed));
    }
}
