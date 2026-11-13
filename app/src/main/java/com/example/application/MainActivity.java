package com.example.application;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.application.databinding.ActivityMainBinding;
import com.example.application.di.ServiceLocator;
import com.example.application.ui.Navigator;
import com.example.application.ui.home.HomeFragment;
import com.example.application.ui.onboarding.PermissionFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** 단일 Activity. 첫 실행이면 온보딩(권한 동의 → 운전 경험 → 안내량), 아니면 홈으로 시작한다. */
public class MainActivity extends AppCompatActivity {
    private Navigator navigator;
    /** Pleos FusedLocation 권한은 문서와 달리 dangerous 수준이라 런타임 요청이 필요하다. */
    public static final String PERMISSION_FUSED_LOCATION = "pleos.car.permission.FUSED_LOCATION";

    private Consumer<Map<String, Boolean>> pendingCallback;

    private final ActivityResultLauncher<String[]> runtimePermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Consumer<Map<String, Boolean>> cb = pendingCallback;
                pendingCallback = null;
                if (cb != null) {
                    cb.accept(result);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        navigator = new Navigator(getSupportFragmentManager(), R.id.fragment_container);

        ServiceLocator services = ServiceLocator.get();
        binding.simulatedBanner.setVisibility(services.anySimulated() ? View.VISIBLE : View.GONE);

        if (savedInstanceState == null) {
            if (services.profiles.get().onboardingCompleted) {
                navigator.setRoot(new HomeFragment());
            } else {
                navigator.setRoot(new PermissionFragment());
            }
        }
    }

    public Navigator navigator() {
        return navigator;
    }

    /** 안드로이드 마이크 런타임 권한 확인(없으면 요청). */
    public void ensureMicPermission(Consumer<Boolean> callback) {
        ensurePermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                r -> callback.accept(Boolean.TRUE.equals(r.get(Manifest.permission.RECORD_AUDIO))));
    }

    public void ensureLocationPermission(Consumer<Boolean> callback) {
        ensurePermissions(new String[]{PERMISSION_FUSED_LOCATION},
                r -> callback.accept(Boolean.TRUE.equals(r.get(PERMISSION_FUSED_LOCATION))));
    }

    /** 여러 런타임 권한을 한 번에 확인·요청한다. 결과는 권한별 허용 여부. */
    public void ensurePermissions(String[] permissions, Consumer<Map<String, Boolean>> callback) {
        Map<String, Boolean> result = new HashMap<>();
        List<String> missing = new ArrayList<>();
        for (String p : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
            result.put(p, granted);
            if (!granted) {
                missing.add(p);
            }
        }
        if (missing.isEmpty()) {
            callback.accept(result);
            return;
        }
        pendingCallback = r -> {
            result.putAll(r);
            callback.accept(result);
        };
        runtimePermissions.launch(missing.toArray(new String[0]));
    }
}
