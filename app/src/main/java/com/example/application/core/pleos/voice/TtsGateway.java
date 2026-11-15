package com.example.application.core.pleos.voice;

import java.util.List;

/** 음성 안내 출력. 문장을 큐에 쌓아 한 문장씩 순서대로 읽는다. */
public interface TtsGateway {
    interface Listener {
        void onSpeakingChanged(boolean speaking);
    }

    void connect();

    void release();

    boolean isSimulated();

    /** 지금 읽던 것을 멈추고 새 문장(들)을 읽는다. */
    void speak(String text);

    void speakAll(List<String> lines);

    /** 큐 뒤에 이어서 읽는다. */
    void enqueue(String text);

    void stop();

    /** 말하기 시작·끝 알림을 받는다(여러 곳에서 동시에 받을 수 있음). */
    void addListener(Listener listener);

    void removeListener(Listener listener);
}
