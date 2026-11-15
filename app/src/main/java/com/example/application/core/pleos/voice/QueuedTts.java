package com.example.application.core.pleos.voice;

import com.example.application.core.MainThread;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** 엔진과 무관한 문장 큐 처리. 하위 클래스는 한 문장을 말하고 끝나면 {@link #onUtteranceFinished()}를 부른다. */
abstract class QueuedTts implements TtsGateway {
    private final Deque<String> queue = new ArrayDeque<>();
    private boolean speaking;
    protected boolean ready;
    private final java.util.List<Listener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    protected abstract void speakNow(String text);

    protected abstract void stopNow();

    @Override
    public void speak(String text) {
        speakAll(Collections.singletonList(text));
    }

    @Override
    public void speakAll(List<String> lines) {
        MainThread.post(() -> {
            queue.clear();
            if (speaking) {
                stopNow();
                speaking = false;
            }
            queue.addAll(lines);
            pump();
        });
    }

    @Override
    public void enqueue(String text) {
        MainThread.post(() -> {
            queue.add(text);
            pump();
        });
    }

    @Override
    public void stop() {
        MainThread.post(() -> {
            queue.clear();
            stopNow();
            setSpeaking(false);
        });
    }

    @Override
    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** 엔진 초기화 완료 시 호출. 그동안 쌓인 문장을 읽기 시작한다. */
    protected void onReady() {
        MainThread.post(() -> {
            ready = true;
            pump();
        });
    }

    protected void onUtteranceFinished() {
        MainThread.post(() -> {
            speaking = false;
            pump();
        });
    }

    private void pump() {
        if (!ready || speaking) {
            return;
        }
        String next = queue.poll();
        if (next == null) {
            setSpeaking(false);
            return;
        }
        speaking = true;
        setSpeaking(true);
        speakNow(next);
    }

    private boolean reportedSpeaking;

    private void setSpeaking(boolean value) {
        if (reportedSpeaking != value) {
            reportedSpeaking = value;
            for (Listener l : listeners) {
                l.onSpeakingChanged(value);
            }
        }
    }
}
