package com.example.application.data;

import com.example.application.model.DriverProfile;

import java.util.EnumSet;

import com.example.application.model.DifficultSituation;

/** 운전 경험 프로필 저장소(기기 내). */
public class ProfileRepository {
    private static final String KEY = "driver_profile";
    private final JsonStore store;
    private DriverProfile cached;

    public ProfileRepository(JsonStore store) {
        this.store = store;
    }

    public DriverProfile get() {
        if (cached == null) {
            cached = store.read(KEY, DriverProfile.class);
            if (cached == null) {
                cached = new DriverProfile();
            }
            if (cached.difficulties == null) {
                cached.difficulties = EnumSet.noneOf(DifficultSituation.class);
            }
        }
        return cached;
    }

    public void save(DriverProfile profile) {
        cached = profile;
        store.write(KEY, profile);
    }
}
