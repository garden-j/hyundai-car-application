package com.example.application.domain;

import com.example.application.core.pleos.vehicle.VehicleStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 조작 실패 원인 확인(규칙 기반). 차량에서 읽은 상태로 "확인된 원인"을 먼저, 차량이 알려 주지 않는
 * 흔한 원인은 "직접 확인할 것"으로 구분해 안내한다. 원인을 단정하지 않고, 안전 이상은 정비를 권한다.
 */
public final class StartDiagnosis {

    public static final class Finding {
        public final boolean confirmed;
        public final String title;
        public final String action;

        Finding(boolean confirmed, String title, String action) {
            this.confirmed = confirmed;
            this.title = title;
            this.action = action;
        }
    }

    private StartDiagnosis() {
    }

    public static List<Finding> diagnose(VehicleStatus st) {
        List<Finding> out = new ArrayList<>();
        if (st != null) {
            if (st.gear != null && st.gear != VehicleStatus.Gear.PARK && st.gear != VehicleStatus.Gear.UNKNOWN) {
                out.add(new Finding(true, "기어가 P가 아니에요", "브레이크를 밟고 기어를 P로 옮긴 뒤 다시 시동을 걸어요."));
            }
            if (Boolean.FALSE.equals(st.allDoorsClosed)) {
                out.add(new Finding(true, "열린 문이 있어요", "모든 문과 트렁크가 닫혔는지 확인해요."));
            }
            if (st.warnings.contains("PARKING_BRAKE") || st.warnings.contains("ELECTRONIC_PARKING_BRAKE")) {
                out.add(new Finding(true, "주차 브레이크가 걸려 있어요", "브레이크를 밟은 채 주차 브레이크 스위치를 눌러 풀어요."));
            }
            if (st.warnings.contains("SEAT_BELT")) {
                out.add(new Finding(true, "안전벨트 경고가 켜져 있어요", "운전석 안전벨트를 딸깍 소리가 나게 매요."));
            }
            if (isLow(st.brakeFluid)) {
                out.add(new Finding(true, "브레이크액이 부족해요", "안전과 관련된 문제예요. 운전하지 말고 정비소나 긴급출동 서비스에 연락해요."));
            }
            if (isLow(st.engineOil)) {
                out.add(new Finding(true, "엔진오일이 부족해요", "가까운 정비소에서 엔진오일을 점검해요."));
            }
            for (String w : st.warnings) {
                if (w.equals("ABS") || w.equals("AIRBAG") || w.equals("EV_SYSTEM") || w.equals("ENGINE")
                        || w.equals("MASTER_WARNING") || w.equals("ENGINE_OIL_PRESSURE")) {
                    out.add(new Finding(true, "경고등이 켜져 있어요(" + w + ")", "계기판 경고 내용을 확인하고, 계속 켜져 있으면 정비 서비스를 이용해요."));
                    break;
                }
            }
        }
        // 차량이 알려 주지 않는 흔한 원인
        out.add(new Finding(false, "브레이크를 끝까지 밟았나요?", "시동은 브레이크를 끝까지 밟은 채 시동 버튼을 눌러야 걸려요."));
        out.add(new Finding(false, "스마트키가 차 안에 있나요?", "키 배터리가 약하면 키를 시동 버튼에 직접 대고 눌러 보세요."));
        out.add(new Finding(false, "핸들이 잠겨 있나요?", "핸들을 좌우로 살짝 움직이면서 시동 버튼을 눌러요."));
        return out;
    }

    static boolean isLow(String level) {
        return "LOW".equals(level) || "CRITICALLY_LOW".equals(level);
    }
}
