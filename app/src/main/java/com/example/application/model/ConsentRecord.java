package com.example.application.model;

/** 권한별 동의 상태 한 건. 동의·철회할 때마다 이력에 추가한다. */
public class ConsentRecord {
    /** 동의서 문구가 바뀌면 올린다. 재동의 판단에 사용. */
    public static final int CURRENT_VERSION = 1;

    public PermissionType type;
    public boolean granted;
    public long timestampMillis;
    public int version;

    public ConsentRecord() {
    }

    public ConsentRecord(PermissionType type, boolean granted, long timestampMillis) {
        this.type = type;
        this.granted = granted;
        this.timestampMillis = timestampMillis;
        this.version = CURRENT_VERSION;
    }
}
