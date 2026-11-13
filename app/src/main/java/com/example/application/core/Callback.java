package com.example.application.core;

/**
 * 비동기 작업 결과 콜백. 앱 전역에서 공통으로 사용한다.
 * 모든 구현체는 메인 스레드에서 콜백을 호출해야 한다.
 */
public interface Callback<T> {
    void onSuccess(T value);

    void onError(Exception e);
}
