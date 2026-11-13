package com.example.backend.external.kma;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 기상청 단기예보 조회서비스(공공데이터포털) 설정. 일반 인증키(Decoding)를 환경변수 KMA_SERVICE_KEY로 주입한다.
 */
@ConfigurationProperties(prefix = "app.kma")
public record KmaProperties(String serviceKey, String nowcastUrl) {

    public KmaProperties {
        if (nowcastUrl == null || nowcastUrl.isBlank()) {
            // 기본 주소(https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0) + 초단기실황 오퍼레이션
            nowcastUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
        }
    }

    public boolean configured() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
