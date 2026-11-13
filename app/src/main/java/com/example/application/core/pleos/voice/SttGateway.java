package com.example.application.core.pleos.voice;

/** 음성 인식. 한 번 듣고 최종 결과를 돌려준다. */
public interface SttGateway {
    interface Listener {
        void onListeningChanged(boolean listening);

        /** 인식 중간 결과(화면에 실시간 표시용). */
        void onPartial(String text);

        void onResult(String text);

        void onError(String message);
    }

    void connect();

    void release();

    boolean isSimulated();

    void listen(Listener listener);

    void stop();
}
