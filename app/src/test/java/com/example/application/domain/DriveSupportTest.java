package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.application.core.pleos.vehicle.VehicleStatus;
import com.example.application.model.DrivingSkill;
import com.example.application.model.GuidanceLevel;
import com.example.application.model.PracticeSetup;

import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** 주행 중·주행 후 지원 기능(Relax, 주유, 환경 변화, 격려, 차량 도움, 운전 지식) 규칙. */
public class DriveSupportTest {

    @Test
    public void relaxIsShortWhileDrivingAndBreathingWhenParked() {
        assertEquals(1, RelaxGuide.calm(GuidanceLevel.MINIMAL, false).size());
        List<String> parked = RelaxGuide.calm(GuidanceLevel.STANDARD, true);
        assertTrue(parked.size() > 3);
        assertTrue(String.join(" ", parked).contains("내쉬"));
        assertNull(RelaxGuide.afterHardSection(GuidanceLevel.MINIMAL));
    }

    @Test
    public void fuelAlertByPercentOrRange() {
        assertTrue(FuelGuide.needsEnergy(10, null));
        assertTrue(FuelGuide.needsEnergy(null, 30f));
        assertFalse(FuelGuide.needsEnergy(60, 300f));
        assertTrue(FuelGuide.alert(true, 10, 30f).contains("충전소"));
        assertEquals("주유소", FuelGuide.searchKeyword(false));
        assertTrue(FuelGuide.procedure(false).get(2).contains("연료"));
    }

    @Test
    public void environmentChangeOnlyOnTurnOn() {
        EnvironmentWatch w = new EnvironmentWatch();
        assertNull(w.update(false, false));
        assertEquals(EnvironmentWatch.Change.RAIN, w.update(true, false));
        assertNull(w.update(true, false));
        assertEquals(EnvironmentWatch.Change.DARK, w.update(true, true));
        EnvironmentWatch alreadyOn = new EnvironmentWatch();
        assertNull(alreadyOn.update(true, true)); // 처음부터 켜져 있으면 기준값
    }

    @Test
    public void encouragementCountsAchievedSkills() {
        PracticeSetup s = new PracticeSetup();
        s.skills.addAll(EnumSet.of(DrivingSkill.U_TURN, DrivingSkill.MERGE));
        DriveStats st = new DriveStats();
        st.startedAtMillis = System.currentTimeMillis() - 20 * 60_000;
        st.countManeuver(DriveGuide.Maneuver.U_TURN);
        st.countManeuver(DriveGuide.Maneuver.MERGE);
        Encouragement e = Encouragement.build(s, st, GuidanceLevel.STANDARD);
        assertEquals("오늘 목표를 해냈어요", e.title);
        assertEquals(2, e.achieved.size());

        DriveStats partial = new DriveStats();
        partial.countManeuver(DriveGuide.Maneuver.U_TURN);
        Encouragement p = Encouragement.build(s, partial, GuidanceLevel.ACTIVE);
        assertEquals("한 걸음 더 나아갔어요", p.title);
        assertTrue(p.next.contains("합류"));
        assertTrue(p.speech.contains("다음에"));
    }

    @Test
    public void encouragementForGeneralDriveArrival() {
        PracticeSetup s = new PracticeSetup();
        s.generalDrive = true;
        DriveStats st = new DriveStats();
        st.arrived = true;
        assertEquals("안전하게 도착했어요", Encouragement.build(s, st, GuidanceLevel.MINIMAL).title);
    }

    @Test
    public void faqSearchFindsByKeyword() {
        assertEquals("와이퍼 켜기", VehicleFaq.search("와이퍼 어떻게 켜?").get(0).title);
        assertEquals("앞유리 김서림 제거", VehicleFaq.search("앞유리가 뿌옇게 됐어").get(0).title);
        assertTrue(VehicleFaq.search("노래 틀어 줘").isEmpty());
    }

    @Test
    public void diagnosisSeparatesConfirmedAndManualChecks() {
        VehicleStatus st = new VehicleStatus();
        st.gear = VehicleStatus.Gear.DRIVE;
        st.allDoorsClosed = false;
        st.brakeFluid = "LOW";
        st.warnings.add("PARKING_BRAKE");
        List<StartDiagnosis.Finding> f = StartDiagnosis.diagnose(st);
        long confirmed = f.stream().filter(x -> x.confirmed).count();
        assertEquals(4, confirmed);
        assertTrue(f.get(f.size() - 1).title.contains("핸들"));
        List<StartDiagnosis.Finding> unknown = StartDiagnosis.diagnose(null);
        assertTrue(unknown.stream().noneMatch(x -> x.confirmed));
    }

    @Test
    public void knowledgeRecommendsForGoals() {
        List<KnowledgeCatalog.Item> rec = KnowledgeCatalog.recommended(EnumSet.of(DrivingSkill.U_TURN), Collections.emptySet());
        assertEquals("유턴하는 방법", rec.get(0).title);
        assertFalse(KnowledgeCatalog.recommended(Collections.emptySet(), null).isEmpty());
        assertFalse(KnowledgeCatalog.of(KnowledgeCatalog.Category.PARKING).isEmpty());
    }
}
