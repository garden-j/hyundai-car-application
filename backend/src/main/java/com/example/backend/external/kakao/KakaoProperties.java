package com.example.backend.external.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 API 설정. REST API 키는 환경변수 KAKAO_REST_API_KEY로 주입한다(저장소에 넣지 않음).
 * 길찾기(카카오모빌리티)와 장소 검색(카카오 로컬)은 같은 REST API 키를 쓴다.
 */
@ConfigurationProperties(prefix = "app.kakao")
public record KakaoProperties(String restApiKey, String directionsUrl, String localKeywordUrl) {

    public KakaoProperties {
        if (directionsUrl == null || directionsUrl.isBlank()) {
            directionsUrl = "https://apis-navi.kakaomobility.com/v1/directions";
        }
        if (localKeywordUrl == null || localKeywordUrl.isBlank()) {
            localKeywordUrl = "https://dapi.kakao.com/v2/local/search/keyword.json";
        }
    }

    public boolean configured() {
        return restApiKey != null && !restApiKey.isBlank();
    }

    public String authorization() {
        return "KakaoAK " + restApiKey;
    }
}
