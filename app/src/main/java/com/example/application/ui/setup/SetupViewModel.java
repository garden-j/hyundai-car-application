package com.example.application.ui.setup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.core.Callback;
import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.core.pleos.vehicle.VehicleReadiness;
import com.example.application.core.vehiclecontrol.ApplyReport;
import com.example.application.data.PracticeSession;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.ChecklistEvaluator;
import com.example.application.domain.ChecklistItem;
import com.example.application.domain.PresetRecommender;
import com.example.application.domain.PresetTargets;
import com.example.application.domain.TodayRecommendation;
import com.example.application.model.BriefingPoint;
import com.example.application.model.GeoPoint;
import com.example.application.model.PermissionType;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;
import com.example.application.ui.common.Resource;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 출발 전 설정 흐름(연습 시작 ~ 출발) 전체가 공유하는 상태. Activity 범위로 유지된다.
 */
public class SetupViewModel extends ViewModel {
    private final ServiceLocator sl = ServiceLocator.get();

    private PracticeSetup setup = new PracticeSetup();
    private GeoPoint origin;

    private final MutableLiveData<Resource<List<RouteCandidate>>> routes = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<BriefingPoint>>> briefing = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> naviRequest = new MutableLiveData<>();
    private final MutableLiveData<List<ChecklistItem>> checklist = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApplyReport>> applyResult = new MutableLiveData<>();

    /** 연습 목적지로 현실적인 최대 직선 거리(km). 넘으면 위치가 잘못됐을 가능성이 크다. */
    static final double MAX_PRACTICE_KM = 100;

    private VehicleReadiness readiness;
    private final Set<ChecklistItem.Id> userChecked = EnumSet.noneOf(ChecklistItem.Id.class);
    private String presetReason;
    /** 경로·브리핑을 계산한 기준 설정(설정이 바뀌면 다시 계산). */
    private int routesForHash;
    private RouteCandidate briefingFor;

    /** 프리셋 추천에 쓰는 현재 날씨(서버가 없거나 실패하면 null). */
    private com.example.application.data.WeatherRepository.Weather weather;

    public PracticeSetup setup() {
        return setup;
    }

    /** 이미 알고 있는 출발 위치(장소 검색 정렬용). 모르면 null. */
    public GeoPoint knownOrigin() {
        if (setup.destination != null && setup.destination.origin != null) {
            return setup.destination.origin.point;
        }
        if (setup.manualOrigin != null) {
            return setup.manualOrigin.point;
        }
        return origin;
    }

    /** 새 연습을 시작한다. 오늘의 추천에서 왔으면 미리 채운다. */
    public void start(TodayRecommendation prefill) {
        setup = new PracticeSetup();
        if (prefill != null) {
            prefill.prefill(setup);
        }
        routes.setValue(null);
        briefing.setValue(null);
        naviRequest.setValue(null);
        applyResult.setValue(null);
        checklist.setValue(null);
        readiness = null;
        weather = null;
        userChecked.clear();
        routesForHash = 0;
        briefingFor = null;
    }

    /** 일반 주행을 시작한다(연습 목표·긴장도 없이 목적지까지). 안내 수준은 설정값 그대로 쓴다. */
    public void startGeneralDrive() {
        start(null);
        setup.generalDrive = true;
    }

    /** 일반 주행: 고른 목적지로 바로 가는 경로를 만든다(경유지 없이 내비가 경로를 계산). */
    public void prepareGeneralDriveRoute() {
        com.example.application.model.DestinationGoal d = setup.destination;
        if (d == null || d.destination == null) {
            return;
        }
        RouteCandidate r = new RouteCandidate();
        r.id = "drive";
        r.kind = RouteCandidate.Kind.WIDE_EASY;
        r.title = d.destination.name;
        r.reason = d.roundTrip ? "다녀오기" : "목적지까지";
        r.origin = knownOrigin();
        r.source = "DIRECT";
        if (d.roundTrip && d.origin != null) {
            // 왕복: 목적지를 경유지로, 출발지를 최종 목적지로
            r.waypoints.add(d.destination);
            r.destination = d.origin;
        } else {
            r.destination = d.destination;
        }
        if (r.origin != null && r.destination != null) {
            double km = r.origin.distanceKmTo(d.destination.point);
            r.distanceKm = Math.round(km * (d.roundTrip ? 2 : 1) * 1.3 * 10) / 10.0;
            r.minutes = (int) Math.max(1, Math.round(r.distanceKm / 25.0 * 60));
        }
        setup.selectedRoute = r;
    }

    // ---- 위치 ----

    public void withOrigin(Callback<GeoPoint> callback) {
        if (setup.destination != null && setup.destination.origin != null) {
            callback.onSuccess(setup.destination.origin.point);
            return;
        }
        if (setup.manualOrigin != null) {
            callback.onSuccess(setup.manualOrigin.point);
            return;
        }
        if (origin != null) {
            callback.onSuccess(origin);
            return;
        }
        if (!sl.consents.isGranted(PermissionType.LOCATION)) {
            callback.onError(new SecurityException("위치 사용에 동의하지 않아 출발지를 직접 골라야 해요."));
            return;
        }
        sl.location.lastKnownLocation(new Callback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint value) {
                origin = value;
                callback.onSuccess(value);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // ---- 경로 추천 ----

    public LiveData<Resource<List<RouteCandidate>>> routes() {
        return routes;
    }

    public void loadRoutes(boolean force) {
        int hash = setupHash();
        Resource<List<RouteCandidate>> current = routes.getValue();
        if (!force && current != null && current.state != Resource.State.ERROR && hash == routesForHash) {
            return;
        }
        routesForHash = hash;
        setup.selectedRoute = null;
        routes.setValue(Resource.loading());
        withOrigin(new Callback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint from) {
                if (setup.hasDestination()) {
                    double km = from.distanceKmTo(setup.destination.destination.point);
                    if (km > MAX_PRACTICE_KM) {
                        routes.setValue(Resource.error(String.format(java.util.Locale.KOREA,
                                "목적지가 현재 위치에서 약 %,.0fkm 떨어져 있어요. 출발지를 확인해 주세요.", km)));
                        return;
                    }
                }
                sl.routes.recommend(from, setup, sl.profiles.get(), new Callback<List<RouteCandidate>>() {
                    @Override
                    public void onSuccess(List<RouteCandidate> value) {
                        routes.setValue(Resource.success(value));
                    }

                    @Override
                    public void onError(Exception e) {
                        routes.setValue(Resource.error("경로를 찾지 못했어요. 다시 시도해 주세요."));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                routes.setValue(Resource.error(e.getMessage() != null ? e.getMessage() : "현재 위치를 알 수 없어요."));
            }
        });
    }

    private int setupHash() {
        return java.util.Objects.hash(setup.goalTypes, setup.skills,
                setup.amount == null ? null : setup.amount.display(),
                setup.destination == null || setup.destination.destination == null ? null
                        : setup.destination.destination.name + setup.destination.roundTrip
                        + (setup.destination.origin == null ? "" : setup.destination.origin.name),
                setup.environments, setup.difficulty, setup.familiarity, setup.tension, setup.tensionReasons,
                setup.manualOrigin == null ? null : setup.manualOrigin.name);
    }

    /** 선택한 경로를 Pleos 내비게이션에 전달한다. */
    public LiveData<Resource<Void>> naviRequest() {
        return naviRequest;
    }

    public void sendRouteToNavi() {
        naviRequest.setValue(Resource.loading());
        sl.navi.startRoute(setup.selectedRoute, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                naviRequest.setValue(Resource.success(null));
            }

            @Override
            public void onError(Exception e) {
                naviRequest.setValue(Resource.error(e.getMessage()));
            }
        });
    }

    public void clearNaviRequest() {
        naviRequest.setValue(null);
    }

    // ---- 브리핑 ----

    public LiveData<Resource<List<BriefingPoint>>> briefing() {
        return briefing;
    }

    public void loadBriefing() {
        RouteCandidate route = setup.selectedRoute;
        if (route == null || (route == briefingFor && briefing.getValue() != null
                && briefing.getValue().state == Resource.State.SUCCESS)) {
            return;
        }
        briefingFor = route;
        briefing.setValue(Resource.loading());
        sl.routes.briefing(route, new Callback<List<BriefingPoint>>() {
            @Override
            public void onSuccess(List<BriefingPoint> value) {
                briefing.setValue(Resource.success(value));
            }

            @Override
            public void onError(Exception e) {
                briefing.setValue(Resource.error("브리핑을 만들지 못했어요."));
            }
        });
    }

    // ---- 체크리스트 ----

    public LiveData<List<ChecklistItem>> checklist() {
        return checklist;
    }

    /** 프리셋 화면 전에 날씨를 미리 받아 둔다(체크리스트 화면에서 호출). */
    private void prefetchWeather() {
        GeoPoint at = knownOrigin();
        if (at == null || weather != null) {
            return;
        }
        sl.weather.current(at, new Callback<com.example.application.data.WeatherRepository.Weather>() {
            @Override
            public void onSuccess(com.example.application.data.WeatherRepository.Weather value) {
                weather = value;
            }

            @Override
            public void onError(Exception e) {
                // 날씨 없이 목표·시간만으로 추천
            }
        });
    }

    public void refreshChecklist() {
        prefetchWeather();
        if (!sl.consents.isGranted(PermissionType.VEHICLE)) {
            readiness = null;
            evaluateChecklist();
            return;
        }
        sl.vehicle.readReadiness(new Callback<VehicleReadiness>() {
            @Override
            public void onSuccess(VehicleReadiness value) {
                readiness = value;
                evaluateChecklist();
            }

            @Override
            public void onError(Exception e) {
                readiness = null;
                evaluateChecklist();
            }
        });
    }

    public void toggleUserCheck(ChecklistItem.Id id) {
        if (!userChecked.remove(id)) {
            userChecked.add(id);
        }
        evaluateChecklist();
    }

    private void evaluateChecklist() {
        boolean allPermissions = sl.consents.isGranted(PermissionType.LOCATION)
                && sl.consents.isGranted(PermissionType.MICROPHONE);
        checklist.setValue(ChecklistEvaluator.evaluate(readiness, userChecked, setup, allPermissions));
    }

    // ---- 연습 모드 프리셋 ----

    public void recommendPreset() {
        if (setup.preset == null) {
            Float temp = weather == null ? null : weather.temperatureC;
            Boolean raining = weather == null ? null : (weather.raining || weather.snowing);
            PresetRecommender.Recommendation r = PresetRecommender.recommend(setup, sl.profiles.get(), temp, raining);
            setup.preset = r.preset;
            presetReason = r.reason;
        }
    }

    public String presetReason() {
        return presetReason;
    }

    public Map<SettingKey, Object> presetTargets() {
        return PresetTargets.resolve(setup.preset, setup.presetOverrides, setup.presetConsentGiven);
    }

    public LiveData<Resource<ApplyReport>> applyResult() {
        return applyResult;
    }

    public boolean vehicleControlAllowed() {
        return sl.consents.isGranted(PermissionType.VEHICLE);
    }

    /** 현재 설정을 저장하고 프리셋을 적용한다. 결과와 무관하게 세션을 기록한다. */
    public void applyPreset() {
        applyResult.setValue(Resource.loading());
        sl.presets.apply(presetTargets(), new Callback<ApplyReport>() {
            @Override
            public void onSuccess(ApplyReport report) {
                saveSession(report);
                applyResult.setValue(Resource.success(report));
            }

            @Override
            public void onError(Exception e) {
                saveSession(null);
                applyResult.setValue(Resource.error("차량 설정을 읽지 못했어요. 설정은 직접 맞춰 주세요."));
            }
        });
    }

    /** 차량 제어 없이 출발(차량 권한 미동의 등). */
    public void skipPreset() {
        saveSession(null);
    }

    private void saveSession(ApplyReport report) {
        PracticeSession s = new PracticeSession();
        s.startedAtMillis = System.currentTimeMillis();
        if (setup.selectedRoute != null) {
            s.routeTitle = setup.selectedRoute.title;
            s.routeDistanceKm = setup.selectedRoute.distanceKm;
            s.routeMinutes = setup.selectedRoute.minutes;
        }
        s.preset = setup.preset;
        if (report != null) {
            s.settingsBefore = report.before;
            s.settingsApplied = report.applied;
        }
        sl.sessions.save(s);
    }
}
