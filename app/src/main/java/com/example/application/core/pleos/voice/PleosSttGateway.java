package com.example.application.core.pleos.voice;

import android.content.Context;
import android.util.Log;

import com.example.application.BuildConfig;
import com.example.application.core.MainThread;

import java.util.Locale;

import ai.pleos.playground.stt.SpeechToText;
import ai.pleos.playground.stt.constant.ErrorType;
import ai.pleos.playground.stt.constant.Mode;
import ai.pleos.playground.stt.listener.OnInitializeListener;
import ai.pleos.playground.stt.listener.OnServerConnectionListener;
import ai.pleos.playground.stt.listener.ResultListener;

/** Pleos SpeechToText. 텍스트만 돌려주며 명령 해석은 앱({@code VoiceCommandParser})이 한다. */
public class PleosSttGateway implements SttGateway {
    private static final String TAG = "PleosStt";
    private final SpeechToText stt;
    private final boolean useServer;
    private Listener current;
    private String lastPartial = "";

    private final ResultListener results = new ResultListener() {
        @Override
        public void onUpdated(String text, boolean isFinal) {
            MainThread.post(() -> {
                if (current == null) {
                    return;
                }
                if (isFinal) {
                    Listener l = current;
                    current = null;
                    l.onListeningChanged(false);
                    l.onResult(text);
                } else {
                    lastPartial = text;
                    current.onPartial(text);
                }
            });
        }

        @Override
        public void onUpdatedEpdData(long epdOn, long epdOff) {
        }

        @Override
        public void onStartedRecognition() {
            MainThread.post(() -> {
                if (current != null) {
                    current.onListeningChanged(true);
                }
            });
        }

        @Override
        public void onEndedRecognition() {
            MainThread.post(() -> {
                // 최종 결과 없이 끝나면 마지막 중간 결과를 최종으로 사용
                if (current != null) {
                    Listener l = current;
                    current = null;
                    l.onListeningChanged(false);
                    if (lastPartial.isEmpty()) {
                        l.onError("잘 듣지 못했어요. 다시 말씀해 주세요.");
                    } else {
                        l.onResult(lastPartial);
                    }
                }
            });
        }

        @Override
        public void onError(ErrorType errorType) {
            Log.w(TAG, "stt error " + errorType);
            MainThread.post(() -> {
                if (current != null) {
                    Listener l = current;
                    current = null;
                    l.onListeningChanged(false);
                    l.onError(errorMessage(errorType));
                }
            });
        }

        @Override
        public void onReady() {
        }

        @Override
        public void onUpdatedRms(double rms) {
        }
    };

    public PleosSttGateway(Context context) {
        useServer = !BuildConfig.PLEOS_CLIENT_ID.isEmpty();
        stt = new SpeechToText(context.getApplicationContext(), useServer ? Mode.HYBRID : Mode.ON_DEVICE);
    }

    @Override
    public void connect() {
        stt.initialize(new OnInitializeListener() {
            @Override
            public void onSuccess() {
                stt.addListener(results);
                stt.setLocale(Locale.KOREAN);
                if (useServer) {
                    stt.registerApp(BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET,
                            new OnServerConnectionListener() {
                                @Override
                                public void onConnected() {
                                }

                                @Override
                                public void onFailed(ErrorType errorType) {
                                    Log.w(TAG, "server registration failed: " + errorType);
                                }
                            });
                }
            }

            @Override
            public void onFailure(ErrorType errorType) {
                Log.e(TAG, "stt init failed: " + errorType);
            }
        });
    }

    @Override
    public void release() {
        stt.removeListener(results);
        stt.release();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    public void listen(Listener listener) {
        current = listener;
        lastPartial = "";
        try {
            stt.request();
        } catch (RuntimeException e) {
            current = null;
            listener.onError("음성 인식을 시작하지 못했어요.");
        }
    }

    @Override
    public void stop() {
        stt.stop();
    }

    private static String errorMessage(ErrorType t) {
        switch (t) {
            case MIC_ERROR:
            case PERMISSION_DENIED:
                return "마이크를 사용할 수 없어요. 마이크 권한을 확인해 주세요.";
            case NETWORK_ERROR:
                return "네트워크가 불안정해요. 화면을 눌러 선택해 주세요.";
            case TIMEOUT:
                return "말씀이 들리지 않았어요. 다시 눌러 말씀해 주세요.";
            default:
                return "음성 인식에 실패했어요. 화면을 눌러 선택해 주세요.";
        }
    }
}
