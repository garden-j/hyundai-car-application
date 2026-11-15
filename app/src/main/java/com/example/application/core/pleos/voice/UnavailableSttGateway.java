package com.example.application.core.pleos.voice;

/** Pleos 음성 인식을 쓸 수 없을 때. 외부 인식 서비스로 대체하지 않고 화면 입력으로 안내한다. */
public class UnavailableSttGateway implements SttGateway {
    @Override
    public void connect() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public void listen(Listener listener) {
        listener.onListeningChanged(false);
        listener.onError("지금은 음성 인식을 쓸 수 없어요. 화면에서 선택해 주세요.");
    }

    @Override
    public void stop() {
    }
}
