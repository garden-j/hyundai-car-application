package com.example.application.core.pleos;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Pleos Connect 시스템 서비스가 설치된 기기(Pleos 에뮬레이터·실차)인지 확인한다.
 * 서비스가 없으면 Fake 구현으로 대체해 일반 에뮬레이터에서도 화면 흐름을 확인할 수 있게 한다.
 * (패키지 이름은 각 SDK가 바인딩하는 서비스 패키지와 같다. AndroidManifest의 queries에도 선언되어 있어야 한다.)
 */
public final class PleosAvailability {
    public static final String VEHICLE_SERVICE = "ai.pleos.playground.service.vehicle";
    public static final String NAVI_SERVICE = "ai.pleos.playground.service.navi";
    public static final String LOCATION_SERVICE = "ai.pleos.playground.service.location.fused";

    private PleosAvailability() {
    }

    public static boolean has(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
