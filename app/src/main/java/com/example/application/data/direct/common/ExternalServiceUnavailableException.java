package com.example.application.data.direct.common;

/**
 * 외부 API(카카오·기상청)를 쓸 수 없을 때(키 미설정·장애·결과 없음).
 * 앱은 이 경우 기기 내 대체 로직으로 전환한다.
 */
public class ExternalServiceUnavailableException extends RuntimeException {
    private final String code;

    public ExternalServiceUnavailableException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ExternalServiceUnavailableException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
