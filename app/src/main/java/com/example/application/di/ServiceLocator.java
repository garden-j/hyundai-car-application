package com.example.application.di;

import android.content.Context;
import android.util.Log;

import com.example.application.core.pleos.PleosAvailability;
import com.example.application.core.pleos.location.FakeLocationGateway;
import com.example.application.core.pleos.location.LocationGateway;
import com.example.application.core.pleos.location.PleosLocationGateway;
import com.example.application.core.pleos.navi.FakeNaviGateway;
import com.example.application.core.pleos.navi.NaviGateway;
import com.example.application.core.pleos.navi.PleosNaviGateway;
import com.example.application.core.pleos.vehicle.FakeVehicleGateway;
import com.example.application.core.pleos.vehicle.PleosVehicleGateway;
import com.example.application.core.pleos.vehicle.VehicleGateway;
import com.example.application.core.pleos.voice.AndroidSttGateway;
import com.example.application.core.pleos.voice.AndroidTtsGateway;
import com.example.application.core.pleos.voice.PleosSttGateway;
import com.example.application.core.pleos.voice.PleosTtsGateway;
import com.example.application.core.pleos.voice.SttGateway;
import com.example.application.core.pleos.voice.TtsGateway;
import com.example.application.core.pleos.voice.UnavailableSttGateway;
import com.example.application.core.vehiclecontrol.PresetController;
import com.example.application.data.ConsentRepository;
import com.example.application.data.JsonStore;
import com.example.application.data.LocalRouteRepository;
import com.example.application.data.PlaceRepository;
import com.example.application.data.ProfileRepository;
import com.example.application.data.RouteRepository;
import com.example.application.data.SessionStore;
import com.example.application.data.WeatherRepository;
import com.example.application.data.direct.DirectApiClient;
import com.example.application.data.remote.ApiClient;
import com.example.application.data.remote.RemoteRouteRepository;
import com.example.application.BuildConfig;

/**
 * 수동 의존성 주입. Pleos 시스템 서비스가 있으면 실제 SDK, 없으면 Fake/안드로이드 기본 구현을 연결한다.
 */
public final class ServiceLocator {
    private static final String TAG = "ServiceLocator";
    private static ServiceLocator instance;

    public VehicleGateway vehicle;
    public NaviGateway navi;
    public LocationGateway location;
    public TtsGateway tts;
    public SttGateway stt;

    public final ProfileRepository profiles;
    public final ConsentRepository consents;
    public final SessionStore sessions;
    // 서버 사용 모드에 따라 바뀌므로 매번 services().routes 처럼 꺼내 쓴다.
    public RouteRepository routes;
    public PlaceRepository places;
    public WeatherRepository weather;
    public PresetController presets;

    /** 서버 주소가 설정돼 있을 때만 존재한다. */
    private final ApiClient api;
    private final JsonStore store;
    private static final String KEY_DETAILED_MODE = "detailed_mode";

    private ServiceLocator(Context context) {
        boolean pleosVehicle = PleosAvailability.has(context, PleosAvailability.VEHICLE_SERVICE);
        boolean pleosNavi = PleosAvailability.has(context, PleosAvailability.NAVI_SERVICE);
        boolean pleosLocation = PleosAvailability.has(context, PleosAvailability.LOCATION_SERVICE);
        Log.i(TAG, "pleos services: vehicle=" + pleosVehicle + " navi=" + pleosNavi + " location=" + pleosLocation);

        vehicle = pleosVehicle ? new PleosVehicleGateway(context) : new FakeVehicleGateway();
        navi = pleosNavi ? new PleosNaviGateway(context) : new FakeNaviGateway();
        location = pleosLocation ? new PleosLocationGateway(context) : new FakeLocationGateway();
        // 음성 서비스는 별도 패키지 확인 수단이 없어 Pleos 차량 서비스 유무로 Pleos 환경을 판단한다
        tts = pleosVehicle ? new PleosTtsGateway(context) : new AndroidTtsGateway(context);
        stt = pleosVehicle ? new PleosSttGateway(context) : new AndroidSttGateway(context);

        store = new JsonStore(context, "app_data");
        profiles = new ProfileRepository(store);
        consents = new ConsentRepository(store);
        sessions = new SessionStore(store);
        // 상세 버전 데이터 출처: 카카오 키가 있으면 앱이 직접 호출(서버 불필요),
        // 없고 서버 주소가 있으면 백엔드 서버, 둘 다 없으면 기본 버전만 쓸 수 있다.
        if (DirectApiClient.canRun(BuildConfig.KAKAO_REST_API_KEY)) {
            api = new DirectApiClient(BuildConfig.KAKAO_REST_API_KEY, BuildConfig.KMA_SERVICE_KEY);
            Log.i(TAG, "detailed data source: direct (kakao/kma from device)");
        } else if (!BuildConfig.API_BASE_URL.isEmpty()) {
            api = new ApiClient(BuildConfig.API_BASE_URL);
            Log.i(TAG, "detailed data source: server " + BuildConfig.API_BASE_URL);
        } else {
            api = null;
            Log.i(TAG, "detailed data source: none (basic only)");
        }
        applyServerMode();
        presets = new PresetController(vehicle);
    }

    /**
     * 기본 버전: Pleos SDK와 기기 내 로직만 사용(외부 통신 없음).
     * 상세 버전: 카카오 길찾기·장소 검색, 기상청 날씨로 실제 도로를 분석한다.
     * 외부 API가 응답하지 않으면 상세 버전에서도 각 저장소가 기기 내 로직으로 대체한다.
     */
    private void applyServerMode() {
        ApiClient active = isDetailedMode() ? api : null;
        routes = active == null ? new LocalRouteRepository() : new RemoteRouteRepository(active, new LocalRouteRepository());
        places = new PlaceRepository(active);
        weather = new WeatherRepository(active);
        Log.i(TAG, "server mode: " + (active == null ? "basic" : "detailed"));
    }

    /** 이 빌드에 카카오 키(또는 서버 주소)가 설정돼 있어 상세 버전을 고를 수 있는지. */
    public boolean serverConfigured() {
        return api != null;
    }

    /** 상세 버전(카카오·기상청 사용) 여부. 사용자가 고르지 않았으면 상세 버전이 기본값. */
    public boolean isDetailedMode() {
        Boolean on = store.read(KEY_DETAILED_MODE, Boolean.class);
        return api != null && !Boolean.FALSE.equals(on);
    }

    public void setDetailedMode(boolean on) {
        store.write(KEY_DETAILED_MODE, on);
        applyServerMode();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ServiceLocator(context.getApplicationContext());
            instance.connectAll(context.getApplicationContext());
        }
    }

    /**
     * SDK별로 연결한다. 권한 누락·서비스 오류로 하나가 실패해도 앱이 죽지 않도록
     * 실패한 SDK는 대체 구현으로 바꾼다.
     */
    private void connectAll(Context context) {
        try {
            vehicle.connect();
        } catch (RuntimeException e) {
            Log.e(TAG, "Vehicle SDK connect failed, using simulation", e);
            vehicle = new FakeVehicleGateway();
        }
        try {
            navi.connect();
        } catch (RuntimeException e) {
            Log.e(TAG, "NaviHelper connect failed, using simulation", e);
            navi = new FakeNaviGateway();
        }
        try {
            location.connect();
        } catch (RuntimeException e) {
            Log.e(TAG, "FusedLocation connect failed, using simulation", e);
            location = new FakeLocationGateway();
        }
        try {
            tts.connect();
        } catch (RuntimeException e) {
            Log.e(TAG, "Pleos TTS connect failed, using Android TTS", e);
            tts = new AndroidTtsGateway(context);
            tts.connect();
        }
        try {
            stt.connect();
        } catch (RuntimeException e) {
            // 공모전 규정: 차량 마이크 음성을 외부 음성 인식 서비스로 보낼 수 없다.
            // Pleos 환경에서는 안드로이드 기본 인식(구글 서버)으로 대체하지 않고 음성 입력만 끈다.
            Log.e(TAG, "Pleos STT connect failed, voice input disabled", e);
            stt = stt instanceof PleosSttGateway ? new UnavailableSttGateway() : new AndroidSttGateway(context);
        }
        presets = new PresetController(vehicle);
    }

    public static ServiceLocator get() {
        if (instance == null) {
            throw new IllegalStateException("ServiceLocator.init() not called");
        }
        return instance;
    }

    /** Pleos 서비스 없이 시뮬레이션 중인 항목이 하나라도 있으면 true(화면에 표시용). */
    public boolean anySimulated() {
        return vehicle.isSimulated() || navi.isSimulated() || location.isSimulated();
    }
}
