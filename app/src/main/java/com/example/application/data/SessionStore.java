package com.example.application.data;

/** 진행 중인 연습 세션 하나만 저장한다(중복 세션 방지의 기준). */
public class SessionStore {
    private static final String KEY = "active_session";
    private final JsonStore store;

    public SessionStore(JsonStore store) {
        this.store = store;
    }

    public PracticeSession active() {
        return store.read(KEY, PracticeSession.class);
    }

    public void save(PracticeSession session) {
        store.write(KEY, session);
    }

    public void clear() {
        store.remove(KEY);
    }
}
