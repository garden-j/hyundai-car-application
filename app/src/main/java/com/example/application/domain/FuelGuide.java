package com.example.application.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 주유·충전 필요 판단과 절차 안내. 전기차는 충전, 내연기관은 주유로 안내한다.
 */
public final class FuelGuide {
    /** 잔량(%)이 이보다 낮거나 주행 가능 거리가 아래보다 짧으면 알린다. */
    public static final int LOW_PERCENT = 15;
    public static final int LOW_RANGE_KM = 50;

    private FuelGuide() {
    }

    public static boolean needsEnergy(Integer percent, Float rangeKm) {
        // 주행 가능 거리 0은 차량이 값을 주지 않는 경우(에뮬레이터 등)라 무시한다
        return (percent != null && percent < LOW_PERCENT) || (rangeKm != null && rangeKm > 0 && rangeKm < LOW_RANGE_KM);
    }

    public static String alert(boolean ev, Integer percent, Float rangeKm) {
        String what = ev ? "배터리" : "연료";
        String amount = rangeKm != null ? "주행 가능 거리가 " + Math.round(rangeKm) + "킬로미터 남았어요."
                : percent != null ? what + "가 " + percent + "퍼센트 남았어요." : what + "가 부족해요.";
        return amount + (ev ? " 가까운 충전소를 찾아 드릴까요?" : " 가까운 주유소를 찾아 드릴까요?");
    }

    /** 장소 검색어. */
    public static String searchKeyword(boolean ev) {
        return ev ? "전기차 충전소" : "주유소";
    }

    /** 도착 후 단계별 절차(정차 → 주유구/충전구 → 선택 → 결제 → 마무리). */
    public static List<String> procedure(boolean ev) {
        if (ev) {
            return Arrays.asList(
                    "충전기 앞 주차 칸에 천천히 세우고 기어를 P에 둬요.",
                    "차 시동을 끄고 충전구 덮개를 열어요.",
                    "충전기 화면에서 커넥터 종류를 고르고, 카드나 앱으로 인증해요.",
                    "커넥터를 딸깍 소리가 날 때까지 꽂고 충전 시작을 눌러요.",
                    "끝나면 충전기에서 종료를 누르고 커넥터를 빼서 제자리에 걸어요.",
                    "충전구 덮개를 닫았는지 확인하고 출발해요.");
        }
        return Arrays.asList(
                "주유기 옆에 주유구 방향을 맞춰 세우고 기어를 P에 둬요.",
                "시동을 끄고, 주유구 열림 버튼을 눌러 주유구를 열어요.",
                "차에 맞는 연료를 꼭 확인해요. 휘발유인지 경유인지 주유구 안쪽에 적혀 있어요.",
                "셀프 주유기라면 금액이나 가득을 고르고 결제해요.",
                "주유 손잡이를 끝까지 넣고 당겨 주유해요. 다 되면 손잡이를 제자리에 걸어요.",
                "주유구 마개를 딸깍 소리가 날 때까지 돌려 닫고 출발해요.");
    }
}
