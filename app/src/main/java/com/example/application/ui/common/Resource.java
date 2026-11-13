package com.example.application.ui.common;

/** 비동기 결과 상태(로딩/성공/실패)를 LiveData로 전달하기 위한 래퍼. */
public final class Resource<T> {
    public enum State { LOADING, SUCCESS, ERROR }

    public final State state;
    public final T data;
    public final String error;

    private Resource(State state, T data, String error) {
        this.state = state;
        this.data = data;
        this.error = error;
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(State.LOADING, null, null);
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(State.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String message) {
        return new Resource<>(State.ERROR, null, message);
    }
}
