package com.example.application.core.pleos.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/** Pleos 서비스가 없는 환경용 대체 TTS(안드로이드 기본 엔진). */
public class AndroidTtsGateway extends QueuedTts {
    private static final String TAG = "AndroidTts";
    private final Context context;
    private TextToSpeech tts;
    private int seq;

    public AndroidTtsGateway(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void connect() {
        tts = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "android tts unavailable");
                return;
            }
            tts.setLanguage(Locale.KOREAN);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                }

                @Override
                public void onDone(String id) {
                    onUtteranceFinished();
                }

                @Override
                public void onError(String id) {
                    onUtteranceFinished();
                }
            });
            onReady();
        });
    }

    @Override
    public void release() {
        if (tts != null) {
            tts.shutdown();
        }
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    protected void speakNow(String text) {
        Log.i(TAG, "speak: " + text);
        if (tts == null || !ready) {
            onUtteranceFinished(); // 엔진이 없으면 건너뛴다(화면 안내는 그대로 표시됨)
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u" + (seq++));
    }

    @Override
    protected void stopNow() {
        if (tts != null) {
            tts.stop();
        }
    }
}
