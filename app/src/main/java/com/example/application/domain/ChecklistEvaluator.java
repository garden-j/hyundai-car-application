package com.example.application.domain;

import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.domain.ChecklistItem.Id;
import com.example.application.domain.ChecklistItem.Status;
import com.example.application.model.PracticeSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 출발 전 체크리스트 평가. 차량 데이터로 확인 가능한 항목은 자동 판정하고,
 * 확인할 수 없는 항목(데이터 없음 포함)은 사용자가 직접 확인하도록 한다.
 */
public final class ChecklistEvaluator {
    public static final int LOW_FUEL_PERCENT = 15;

    private ChecklistEvaluator() {
    }

    /**
     * @param readiness      차량 상태(차량 권한 미동의·읽기 실패면 null)
     * @param userChecked    사용자가 직접 확인 표시한 항목
     * @param allPermissions 위치·마이크 권한에 모두 동의했는지
     */
    public static List<ChecklistItem> evaluate(VehicleReadiness readiness, Set<Id> userChecked,
                                               PracticeSetup setup, boolean allPermissions) {
        List<ChecklistItem> items = new ArrayList<>();
        Boolean belt = readiness == null ? null : readiness.driverSeatBeltBuckled;
        items.add(auto(Id.SEAT_BELT, belt, userChecked, "안전벨트를 매 주세요."));

        Boolean doors = readiness == null ? null : readiness.allDoorsClosed;
        items.add(auto(Id.DOORS, doors, userChecked, "열린 문이 있어요. 모든 문을 닫아 주세요."));

        items.add(manual(Id.SIDE_MIRROR, userChecked, "양쪽 사이드미러에 차 옆면이 살짝 보이게 맞춰 주세요."));
        items.add(fuel(readiness, userChecked));
        items.add(manual(Id.SEAT_POSITION, userChecked, "브레이크를 끝까지 밟았을 때 무릎이 살짝 굽혀지게 맞춰 주세요."));
        items.add(manual(Id.GUIDE_VOLUME, userChecked, "'안내 들어보기'로 음성이 잘 들리는지 확인해 주세요."));
        items.add(manual(Id.MUSIC_OFF, userChecked, "연습 중에는 음악을 끄면 안내를 더 잘 들을 수 있어요."));
        items.add(manual(Id.PHONE_MOUNT, userChecked, "휴대폰은 거치대에 두고 주행 중에는 만지지 않아요."));

        boolean routeSet = setup != null && setup.selectedRoute != null;
        items.add(new ChecklistItem(Id.ROUTE, routeSet ? Status.DONE : Status.ATTENTION, true,
                routeSet ? null : "연습 경로를 먼저 골라 주세요."));

        items.add(new ChecklistItem(Id.PERMISSIONS,
                allPermissions || userChecked.contains(Id.PERMISSIONS) ? Status.DONE : Status.NEEDS_USER,
                allPermissions, allPermissions ? null
                : "일부 권한에 동의하지 않아 음성 답변이나 위치 기반 안내가 제한돼요. 설정에서 켤 수 있어요."));
        return items;
    }

    private static ChecklistItem auto(Id id, Boolean ok, Set<Id> userChecked, String guide) {
        if (ok == null) {
            // 차량에서 확인할 수 없으면 사용자 확인으로 대체
            return manual(id, userChecked, guide);
        }
        return new ChecklistItem(id, ok ? Status.DONE : Status.ATTENTION, true, ok ? null : guide);
    }

    private static ChecklistItem manual(Id id, Set<Id> userChecked, String guide) {
        boolean done = userChecked.contains(id);
        return new ChecklistItem(id, done ? Status.DONE : Status.NEEDS_USER, false, done ? null : guide);
    }

    private static ChecklistItem fuel(VehicleReadiness r, Set<Id> userChecked) {
        Integer percent = null;
        if (r != null) {
            if (r.fuelPercent != null) {
                percent = r.fuelPercent;
            } else if (r.evBatteryLevel != null && r.evBatteryLevel <= 100f) {
                percent = Math.round(r.evBatteryLevel);
            }
        }
        if (percent == null) {
            return manual(Id.FUEL, userChecked, "계기판에서 연료(배터리) 잔량을 확인해 주세요.");
        }
        if (percent >= LOW_FUEL_PERCENT || userChecked.contains(Id.FUEL)) {
            return new ChecklistItem(Id.FUEL, Status.DONE, true, null);
        }
        return new ChecklistItem(Id.FUEL, Status.ATTENTION, true,
                "연료가 " + percent + "% 남았어요. 연습 전에 주유(충전)하는 것을 권해요.");
    }

    /** 출발을 막는 항목(필수인데 완료되지 않음). */
    public static List<ChecklistItem> blocking(List<ChecklistItem> items) {
        List<ChecklistItem> out = new ArrayList<>();
        for (ChecklistItem i : items) {
            if (i.id.required && !i.isDone()) {
                out.add(i);
            }
        }
        return out;
    }

    public static List<ChecklistItem> incomplete(List<ChecklistItem> items) {
        List<ChecklistItem> out = new ArrayList<>();
        for (ChecklistItem i : items) {
            if (!i.isDone()) {
                out.add(i);
            }
        }
        return out;
    }
}
