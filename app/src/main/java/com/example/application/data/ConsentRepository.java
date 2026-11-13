package com.example.application.data;

import com.example.application.model.ConsentRecord;
import com.example.application.model.PermissionType;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * 권한별 동의 상태와 이력. 동의·철회 시점, 항목, 동의서 버전을 기록한다(기능 명세 "권한 동의 관리").
 */
public class ConsentRepository {
    private static final String KEY = "consent_history";
    private final JsonStore store;
    private List<ConsentRecord> history;

    public ConsentRepository(JsonStore store) {
        this.store = store;
    }

    private List<ConsentRecord> history() {
        if (history == null) {
            history = store.read(KEY, new TypeToken<List<ConsentRecord>>() {
            }.getType());
            if (history == null) {
                history = new ArrayList<>();
            }
        }
        return history;
    }

    public void record(PermissionType type, boolean granted) {
        history().add(new ConsentRecord(type, granted, System.currentTimeMillis()));
        store.write(KEY, history());
    }

    /** 가장 최근 기록. 한 번도 묻지 않았으면 null. */
    public ConsentRecord latest(PermissionType type) {
        List<ConsentRecord> h = history();
        for (int i = h.size() - 1; i >= 0; i--) {
            if (h.get(i).type == type) {
                return h.get(i);
            }
        }
        return null;
    }

    public boolean isGranted(PermissionType type) {
        ConsentRecord r = latest(type);
        return r != null && r.granted && r.version >= ConsentRecord.CURRENT_VERSION;
    }

    public boolean allGranted() {
        for (PermissionType t : PermissionType.values()) {
            if (!isGranted(t)) {
                return false;
            }
        }
        return true;
    }

    public List<ConsentRecord> allHistory() {
        return new ArrayList<>(history());
    }
}
