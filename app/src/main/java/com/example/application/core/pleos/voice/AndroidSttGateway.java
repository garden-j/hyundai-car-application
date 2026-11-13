package com.example.application.core.pleos.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/** Pleos 서비스가 없는 환경용 대체 음성 인식(안드로이드 SpeechRecognizer). 없는 기기면 오류를 돌려준다. */
public class AndroidSttGateway implements SttGateway {
    private final Context context;
    private SpeechRecognizer recognizer;

    public AndroidSttGateway(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void connect() {
    }

    @Override
    public void release() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public void listen(Listener listener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("이 기기에서는 음성 인식을 쓸 수 없어요. 화면을 눌러 선택해 주세요.");
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                listener.onListeningChanged(true);
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                listener.onListeningChanged(false);
                listener.onError("잘 듣지 못했어요. 다시 말씀해 주세요.");
            }

            @Override
            public void onResults(Bundle results) {
                listener.onListeningChanged(false);
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list == null || list.isEmpty()) {
                    listener.onError("잘 듣지 못했어요. 다시 말씀해 주세요.");
                } else {
                    listener.onResult(list.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partial) {
                ArrayList<String> list = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) {
                    listener.onPartial(list.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(intent);
    }

    @Override
    public void stop() {
        if (recognizer != null) {
            recognizer.stopListening();
        }
    }
}
