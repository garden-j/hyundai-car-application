package com.example.application.core.drive;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.application.domain.DriveGuide;
import com.example.application.domain.DriveStats;
import com.example.application.domain.EnvironmentWatch;
import com.example.application.model.GuidanceLevel;

import java.util.List;

/**
 * 진행 중인 주행 상태(앱 전체 공유). 주행 안내 서비스가 내비 이벤트·차량 상태로 갱신하고,
 * 주행 중 화면이 관찰해 표시한다. 메인 스레드에서만 갱신한다.
 */
public final class DriveSession {
    private static final DriveSession INSTANCE = new DriveSession();

    public static DriveSession get() {
        return INSTANCE;
    }

    /** 경유지를 넣은 이유(도착 시 안내를 고르기 위해). */
    public enum WaypointPurpose { NONE, REST, ENERGY }

    /** 화면 표시용 상태(불변 스냅샷). */
    public static final class State {
        public boolean active;
        public String modeLabel = "연습 모드";
        public DriveGuide.Maneuver maneuver;
        public double distanceM = -1;
        public String headline;
        public String tip;
        public String lastSpoken;
        public EnvironmentWatch.Change envChange;
        public String energyAlert;
        public boolean arrived;
        public List<String> procedure;

        State copy() {
            State s = new State();
            s.active = active;
            s.modeLabel = modeLabel;
            s.maneuver = maneuver;
            s.distanceM = distanceM;
            s.headline = headline;
            s.tip = tip;
            s.lastSpoken = lastSpoken;
            s.envChange = envChange;
            s.energyAlert = energyAlert;
            s.arrived = arrived;
            s.procedure = procedure;
            return s;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(new State());
    private DriveStats stats = new DriveStats();
    private GuidanceLevel guidance = GuidanceLevel.STANDARD;
    private GuidanceLevel relax = GuidanceLevel.STANDARD;
    private GuidanceLevel encourage = GuidanceLevel.STANDARD;
    private boolean ev;
    private WaypointPurpose waypointPurpose = WaypointPurpose.NONE;

    private DriveSession() {
    }

    public LiveData<State> state() {
        return state;
    }

    public State current() {
        return state.getValue();
    }

    public void begin(String modeLabel, GuidanceLevel guidance, GuidanceLevel relax, GuidanceLevel encourage) {
        this.guidance = guidance;
        this.relax = relax;
        this.encourage = encourage;
        stats = new DriveStats();
        stats.startedAtMillis = System.currentTimeMillis();
        waypointPurpose = WaypointPurpose.NONE;
        State s = new State();
        s.active = true;
        s.modeLabel = modeLabel;
        s.headline = "안내를 시작했어요";
        s.tip = "다음 회전이 가까워지면 알려 드릴게요";
        state.setValue(s);
    }

    public void end() {
        stats.endedAtMillis = System.currentTimeMillis();
        State s = current().copy();
        s.active = false;
        state.setValue(s);
    }

    public void update(java.util.function.Consumer<State> change) {
        State s = current().copy();
        change.accept(s);
        state.setValue(s);
    }

    public DriveStats stats() {
        return stats;
    }

    public GuidanceLevel guidance() {
        return guidance;
    }

    public GuidanceLevel relaxLevel() {
        return relax;
    }

    public GuidanceLevel encourageLevel() {
        return encourage;
    }

    public boolean isEv() {
        return ev;
    }

    public void setEv(boolean ev) {
        this.ev = ev;
    }

    public WaypointPurpose waypointPurpose() {
        return waypointPurpose;
    }

    public void setWaypointPurpose(WaypointPurpose p) {
        waypointPurpose = p;
    }
}
