package com.example.application.domain;

/** 출발 전 체크리스트 한 항목. */
public class ChecklistItem {
    public enum Id {
        SEAT_BELT("안전벨트 매기", true),
        DOORS("문 닫힘 확인", true),
        SIDE_MIRROR("사이드미러 확인", false),
        FUEL("연료 확인", false),
        SEAT_POSITION("시트 위치 확인", false),
        GUIDE_VOLUME("안내 음량 확인", false),
        MUSIC_OFF("음악 끄기", false),
        PHONE_MOUNT("휴대폰 거치", false),
        ROUTE("목적지·연습 설정 확인", true),
        PERMISSIONS("위치·마이크 사용 확인", false);

        public final String label;
        /** 확인하지 않으면 출발할 수 없는 필수 항목. */
        public final boolean required;

        Id(String label, boolean required) {
            this.label = label;
            this.required = required;
        }
    }

    public enum Status {
        /** 확인 완료(차량 데이터 또는 사용자 확인). */
        DONE,
        /** 사용자가 직접 확인해야 함. */
        NEEDS_USER,
        /** 차량 데이터로 문제가 확인됨(예: 안전벨트 미착용, 연료 부족). */
        ATTENTION
    }

    public final Id id;
    public final Status status;
    /** true면 차량·앱 데이터로 자동 확인한 결과. */
    public final boolean autoChecked;
    /** 미완료 시 조치 안내. */
    public final String guide;

    public ChecklistItem(Id id, Status status, boolean autoChecked, String guide) {
        this.id = id;
        this.status = status;
        this.autoChecked = autoChecked;
        this.guide = guide;
    }

    public boolean isDone() {
        return status == Status.DONE;
    }
}
