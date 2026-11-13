package com.example.application.core;

/**
 * 비동기 작업 결과 콜백. 앱 전역에서 공통으로 사용한다.
 * 모든 구현체는 메인 스레드에서 콜백을 호출해야 한다.
 */
public interface Callback<T> {
    void onSuccess(T value);

    void onError(Exception e);

    /** 실패하지 않는 작업(내부에서 대체 결과를 주는 경우)용: 성공만 처리한다. */
    static <T> Callback<T> success(java.util.function.Consumer<T> onSuccess) {
        return new Callback<T>() {
            @Override
            public void onSuccess(T value) {
                onSuccess.accept(value);
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.w("Callback", "unexpected error", e);
            }
        };
    }
}
