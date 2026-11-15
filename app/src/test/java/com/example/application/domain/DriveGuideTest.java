package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.application.domain.DriveGuide.Caution;
import com.example.application.domain.DriveGuide.CautionPoint;
import com.example.application.domain.DriveGuide.Maneuver;
import com.example.application.domain.DriveGuide.Upcoming;
import com.example.application.model.GuidanceLevel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DriveGuideTest {

    private static java.util.List<Upcoming> next(Maneuver m, double meters) {
        return Collections.singletonList(new Upcoming(m, meters, "성남시청 방면"));
    }

    @Test
    public void minimalOnlyCoversHardManeuvers() {
        DriveGuide g = new DriveGuide(GuidanceLevel.MINIMAL);
        assertNull(g.onUpcoming(next(Maneuver.LEFT, 200)));
        assertNull(g.onUpcoming(next(Maneuver.RIGHT, 200)));
        assertNotNull(g.onUpcoming(next(Maneuver.U_TURN, 200)));
    }

    @Test
    public void standardAddsLeftTurnButNotRight() {
        DriveGuide g = new DriveGuide(GuidanceLevel.STANDARD);
        assertNotNull(g.onUpcoming(next(Maneuver.LEFT, 250)));
        assertNull(g.onUpcoming(next(Maneuver.RIGHT, 250)));
    }

    @Test
    public void activeSpeaksEarlierAndMoreDetail() {
        DriveGuide active = new DriveGuide(GuidanceLevel.ACTIVE);
        DriveGuide standard = new DriveGuide(GuidanceLevel.STANDARD);
        assertNull(standard.onUpcoming(next(Maneuver.LEFT, 450)));
        String a = active.onUpcoming(next(Maneuver.LEFT, 450));
        assertNotNull(a);
        assertTrue(a.contains("방향지시등"));
        assertNotNull(active.onUpcoming(next(Maneuver.RIGHT, 400)));
    }

    @Test
    public void sameTurnIsSpokenOnce() {
        DriveGuide g = new DriveGuide(GuidanceLevel.STANDARD);
        assertNotNull(g.onUpcoming(next(Maneuver.MERGE, 280)));
        assertNull(g.onUpcoming(next(Maneuver.MERGE, 150)));
    }

    @Test
    public void tooFarOrAlreadyAtTurnIsSilent() {
        DriveGuide g = new DriveGuide(GuidanceLevel.ACTIVE);
        assertNull(g.onUpcoming(next(Maneuver.U_TURN, 900)));
        assertNull(g.onUpcoming(next(Maneuver.U_TURN, 10)));
    }

    @Test
    public void cautionsFollowLevelAndPickNearest() {
        DriveGuide minimal = new DriveGuide(GuidanceLevel.MINIMAL);
        assertNull(minimal.onCautions(Collections.singletonList(new CautionPoint("a", Caution.SPEED_BUMP, 100))));
        DriveGuide g = new DriveGuide(GuidanceLevel.STANDARD);
        String msg = g.onCautions(Arrays.asList(
                new CautionPoint("far", Caution.SCHOOL_ZONE, 280),
                new CautionPoint("near", Caution.SPEED_BUMP, 120)));
        assertEquals("과속방지턱이 있어요. 미리 속도를 줄여요.", msg);
        assertNotNull(g.onCautions(Collections.singletonList(new CautionPoint("far", Caution.SCHOOL_ZONE, 200))));
        assertNull(g.onCautions(Collections.singletonList(new CautionPoint("near", Caution.SPEED_BUMP, 60))));
    }

    @Test
    public void arrivalOnce() {
        DriveGuide g = new DriveGuide(GuidanceLevel.STANDARD);
        assertNotNull(g.onArrived());
        assertNull(g.onArrived());
    }
}
