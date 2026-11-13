package com.example.application.core.pleos.voice;

import android.content.Context;
import android.util.Log;

import com.example.application.BuildConfig;

import java.util.Locale;

import ai.pleos.playground.tts.TextToSpeech;
import ai.pleos.playground.tts.constant.ErrorType;
import ai.pleos.playground.tts.constant.Mode;
import ai.pleos.playground.tts.listener.EventListener;
import ai.pleos.playground.tts.listener.OnInitializeListener;
import ai.pleos.playground.tts.listener.OnServerConnectionListener;

/**
 * Pleos TextToSpeech. 내 프로젝트의 Client ID/Secret(local.properties의 pleos.clientId/clientSecret)가 있으면 HYBRID,
 * 없으면 등록이 필요 없는 ON_DEVICE 모드로 동작한다.
 */
public class PleosTtsGateway extends QueuedTts {
    private static final String TAG = "PleosTts";
    private final TextToSpeech tts;
    private final boolean useServer;
    /** Pleos 엔진을 쓸 수 없을 때(에뮬레이터 온디바이스 엔진 없음 등) 대신 쓰는 안드로이드 TTS. */
    private final AndroidTtsGateway fallback;
    private boolean usingFallback;
    private String current;

    private final EventListener events = new EventListener() {
        @Override
        public void onStart() {
        }

        @Override
        public void onReady() {
        }

        @Override
        public void onDone() {
            onUtteranceFinished();
        }

        @Override
        public void onError(ErrorType errorType) {
            Log.w(TAG, "tts error " + errorType);
            if (!usingFallback && isEngineUnavailable(errorType)) {
                // 이후 문장은 모두 대체 엔진으로. 실패한 문장은 다시 읽는다
                Log.w(TAG, "switching to Android TTS fallback");
                usingFallback = true;
                if (current != null) {
                    fallback.speakNow(current);
                    return;
                }
            }
            onUtteranceFinished();
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onUpdatedRms(double rms) {
        }
    };

    public PleosTtsGateway(Context context) {
        useServer = !BuildConfig.PLEOS_CLIENT_ID.isEmpty();
        tts = new TextToSpeech(context.getApplicationContext(), useServer ? Mode.HYBRID : Mode.ON_DEVICE);
        fallback = new AndroidTtsGateway(context) {
            @Override
            protected void onUtteranceFinished() {
                PleosTtsGateway.this.onUtteranceFinished();
            }
        };
    }

    private static boolean isEngineUnavailable(ErrorType t) {
        return t == ErrorType.ON_DEVICE_ENGINE_ERROR || t == ErrorType.NOT_SUPPORTED_API
                || t == ErrorType.UNAUTHENTICATED;
    }

    @Override
    public void connect() {
        fallback.connect();
        tts.initialize(new OnInitializeListener() {
            @Override
            public void onSuccess() {
                tts.addEventListener(events);
                tts.setLocale(Locale.KOREAN);
                if (useServer) {
                    tts.registerApp(BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET,
                            new OnServerConnectionListener() {
                                @Override
                                public void onConnected() {
                                    onReady();
                                }

                                @Override
                                public void onFailed(ErrorType errorType) {
                                    // HYBRID는 서버 연결 실패 시에도 온디바이스로 동작한다
                                    Log.w(TAG, "server registration failed: " + errorType);
                                    onReady();
                                }
                            });
                } else {
                    onReady();
                }
            }

            @Override
            public void onFailure(ErrorType errorType) {
                Log.e(TAG, "tts init failed: " + errorType + ", using Android TTS fallback");
                usingFallback = true;
                onReady();
            }
        });
    }

    @Override
    public void release() {
        tts.removeEventListener(events);
        tts.release();
        fallback.release();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    protected void speakNow(String text) {
        current = text;
        if (usingFallback) {
            fallback.speakNow(text);
        } else {
            tts.speak(text, true); // isFinal=true: 한 문장을 완결된 요청으로 처리
        }
    }

    @Override
    protected void stopNow() {
        if (usingFallback) {
            fallback.stopNow();
        } else {
            tts.stop();
        }
    }
}
