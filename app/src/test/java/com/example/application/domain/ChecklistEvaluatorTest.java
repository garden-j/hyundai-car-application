package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.domain.ChecklistItem.Id;
import com.example.application.domain.ChecklistItem.Status;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;

import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ChecklistEvaluatorTest {

    private static PracticeSetup withRoute() {
        PracticeSetup s = new PracticeSetup();
        s.selectedRoute = new RouteCandidate();
        return s;
    }

    private static ChecklistItem find(List<ChecklistItem> items, Id id) {
        for (ChecklistItem i : items) {
            if (i.id == id) {
                return i;
            }
        }
        throw new AssertionError(id);
    }

    @Test
    public void unbuckledSeatBeltBlocksDeparture() {
        VehicleReadiness r = new VehicleReadiness();
        r.driverSeatBeltBuckled = false;
        r.allDoorsClosed = true;
        r.fuelPercent = 50;
        List<ChecklistItem> items = ChecklistEvaluator.evaluate(r, EnumSet.noneOf(Id.class), withRoute(), true);
        assertEquals(Status.ATTENTION, find(items, Id.SEAT_BELT).status);
        List<ChecklistItem> blocking = ChecklistEvaluator.blocking(items);
        assertEquals(1, blocking.size());
        assertEquals(Id.SEAT_BELT, blocking.get(0).id);
    }

    @Test
    public void unknownVehicleDataFallsBackToUserCheck() {
        Set<Id> checked = EnumSet.of(Id.SEAT_BELT, Id.DOORS);
        List<ChecklistItem> items = ChecklistEvaluator.evaluate(null, checked, withRoute(), true);
        assertEquals(Status.DONE, find(items, Id.SEAT_BELT).status);
        assertEquals(false, find(items, Id.SEAT_BELT).autoChecked);
        assertTrue(ChecklistEvaluator.blocking(items).isEmpty());
        assertEquals(Status.NEEDS_USER, find(items, Id.FUEL).status);
    }

    @Test
    public void lowFuelNeedsAttentionButDoesNotBlock() {
        VehicleReadiness r = new VehicleReadiness();
        r.driverSeatBeltBuckled = true;
        r.allDoorsClosed = true;
        r.fuelPercent = 8;
        List<ChecklistItem> items = ChecklistEvaluator.evaluate(r, EnumSet.noneOf(Id.class), withRoute(), true);
        ChecklistItem fuel = find(items, Id.FUEL);
        assertEquals(Status.ATTENTION, fuel.status);
        assertTrue(fuel.guide.contains("8%"));
        assertTrue(ChecklistEvaluator.blocking(items).isEmpty());
    }

    @Test
    public void missingRouteBlocks() {
        List<ChecklistItem> items = ChecklistEvaluator.evaluate(null, EnumSet.allOf(Id.class),
                new PracticeSetup(), true);
        assertEquals(Id.ROUTE, ChecklistEvaluator.blocking(items).get(0).id);
    }
}
