package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.model.DriverProfile;
import com.example.application.model.DrivingSkill;
import com.example.application.model.GoalAmount;
import com.example.application.model.LicensePeriod;
import com.example.application.model.PracticePreset;
import com.example.application.model.PracticeSetup;
import com.example.application.model.PresetItem;
import com.example.application.model.RoadEnvironment;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class PresetTest {

    @Test
    public void weatherBeatsEverything() {
        PracticeSetup s = new PracticeSetup();
        s.skills.add(DrivingSkill.PARKING);
        s.environments.add(RoadEnvironment.RAIN);
        assertEquals(PracticePreset.WEATHER, PresetRecommender.recommend(s, null, null, null).preset);
        PracticeSetup cold = new PracticeSetup();
        assertEquals(PracticePreset.WEATHER, PresetRecommender.recommend(cold, null, -3f, null).preset);
    }

    @Test
    public void skillsMeanManeuverMode() {
        PracticeSetup s = new PracticeSetup();
        s.skills.add(DrivingSkill.U_TURN);
        PresetRecommender.Recommendation r = PresetRecommender.recommend(s, null, 15f, false);
        assertEquals(PracticePreset.MANEUVER, r.preset);
        assertEquals("조작 연습 모드를 추천해요.", r.reason);
    }

    @Test
    public void longDriveAndFirstDrive() {
        PracticeSetup s = new PracticeSetup();
        s.amount = new GoalAmount(GoalAmount.Unit.TIME_MIN, 40);
        assertEquals(PracticePreset.LONG_DRIVE, PresetRecommender.recommend(s, null, null, null).preset);

        DriverProfile p = new DriverProfile();
        p.licensePeriod = LicensePeriod.UNDER_1_MONTH;
        PresetRecommender.Recommendation r = PresetRecommender.recommend(new PracticeSetup(), p, null, null);
        assertEquals(PracticePreset.FOCUS, r.preset);
        assertTrue(r.reason.startsWith("첫 운전이니"));
    }

    @Test
    public void consentRequiredItemsAreExcludedWithoutConsent() {
        Map<SettingKey, Object> without = PresetTargets.resolve(PracticePreset.WEATHER, null, false);
        assertFalse(without.containsKey(SettingKey.STEERING_WHEEL_HEAT));
        Map<SettingKey, Object> with = PresetTargets.resolve(PracticePreset.WEATHER, null, true);
        assertEquals(1, with.get(SettingKey.STEERING_WHEEL_HEAT));
    }

    @Test
    public void overridesReplaceOrKeep() {
        Map<SettingKey, Object> o = new EnumMap<>(SettingKey.class);
        o.put(SettingKey.CABIN_TEMPERATURE, 24.0f);
        o.put(SettingKey.DRIVER_WINDOW, PresetTargets.KEEP);
        Map<SettingKey, Object> t = PresetTargets.resolve(PracticePreset.FOCUS, o, false);
        assertEquals(24.0f, t.get(SettingKey.CABIN_TEMPERATURE));
        assertFalse(t.containsKey(SettingKey.DRIVER_WINDOW));
        assertEquals(PresetItem.FAN_LOW, t.get(SettingKey.FAN_SPEED));
    }

    @Test
    public void lowFanIsLowerQuarterOfRange() {
        assertEquals(3, PresetTargets.lowFan(1, 8));
        assertEquals(0, PresetTargets.lowFan(0, 1));
        assertEquals(5, PresetTargets.lowFan(5, 5));
    }

    @Test
    public void longDriveKeepsMusicAndWindow() {
        Map<SettingKey, Object> t = PresetTargets.resolve(PracticePreset.LONG_DRIVE, null, true);
        assertFalse(t.containsKey(SettingKey.DRIVER_WINDOW));
        assertFalse(t.containsKey(SettingKey.STEERING_WHEEL_HEAT));
    }
}
