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
import com.example.application.core.vehiclecontrol.PresetController;
import com.example.application.data.ConsentRepository;
import com.example.application.data.JsonStore;
import com.example.application.data.LocalRouteRepository;
import com.example.application.data.PlaceRepository;
import com.example.application.data.ProfileRepository;
import com.example.application.data.RouteRepository;
import com.example.application.data.SessionStore;
import com.example.application.data.WeatherRepository;
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
    public final RouteRepository routes;
    public final PlaceRepository places;
    public final WeatherRepository weather;
    public PresetController presets;

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

        JsonStore store = new JsonStore(context, "app_data");
        profiles = new ProfileRepository(store);
        consents = new ConsentRepository(store);
        sessions = new SessionStore(store);
        // 서버 주소(local.properties의 api.baseUrl)가 있으면 서버 경로 추천·장소 검색·날씨를 쓰고,
        // 서버가 응답하지 않으면 각 저장소가 기기 내 로직으로 대체한다.
        ApiClient api = BuildConfig.API_BASE_URL.isEmpty() ? null : new ApiClient(BuildConfig.API_BASE_URL);
        Log.i(TAG, "api server: " + (api == null ? "none (on-device only)" : BuildConfig.API_BASE_URL));
        routes = api == null ? new LocalRouteRepository() : new RemoteRouteRepository(api, new LocalRouteRepository());
        places = new PlaceRepository(api);
        weather = new WeatherRepository(api);
        presets = new PresetController(vehicle);
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
            Log.e(TAG, "Pleos STT connect failed, using Android STT", e);
            stt = new AndroidSttGateway(context);
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
