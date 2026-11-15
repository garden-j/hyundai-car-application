package com.example.application;

import android.app.Application;

import com.example.application.di.ServiceLocator;

public class App extends Application {
    private static App instance;

    /** 화면이 없을 때(백그라운드 타이머 등) 쓰는 애플리케이션 Context. */
    public static android.content.Context context() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Pleos 가이드: SDK initialize()는 애플리케이션 생성 시 호출
        ServiceLocator.init(this);
    }
}
