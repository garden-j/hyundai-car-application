package com.example.application;

import android.app.Application;

import com.example.application.di.ServiceLocator;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Pleos 가이드: SDK initialize()는 애플리케이션 생성 시 호출
        ServiceLocator.init(this);
    }
}
