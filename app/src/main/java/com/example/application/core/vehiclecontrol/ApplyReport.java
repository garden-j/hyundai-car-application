package com.example.application.core.vehiclecontrol;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleSettings;

import java.util.ArrayList;
import java.util.List;

/** 차량 환경 일괄 적용 결과. */
public class ApplyReport {
    public static class Result {
        public final SettingKey key;
        public final Object requested;
        public final boolean success;
        public final String message;

        public Result(SettingKey key, Object requested, boolean success, String message) {
            this.key = key;
            this.requested = requested;
            this.success = success;
            this.message = message;
        }
    }

    public VehicleSettings before;
    public VehicleSettings applied;
    public final List<Result> results = new ArrayList<>();

    public List<Result> failures() {
        List<Result> out = new ArrayList<>();
        for (Result r : results) {
            if (!r.success) {
                out.add(r);
            }
        }
        return out;
    }
}
