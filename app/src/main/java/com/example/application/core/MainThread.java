package com.example.application.core;

import android.os.Handler;
import android.os.Looper;

/** Pleos SDK 콜백은 바인더 스레드에서 올 수 있어 UI 갱신 전에 메인 스레드로 넘긴다. */
public final class MainThread {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private MainThread() {
    }

    public static void post(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            HANDLER.post(r);
        }
    }

    public static void postDelayed(Runnable r, long delayMs) {
        HANDLER.postDelayed(r, delayMs);
    }

    public static void cancel(Runnable r) {
        HANDLER.removeCallbacks(r);
    }
}
