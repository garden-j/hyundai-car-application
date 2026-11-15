package com.example.application.data.direct.external.kakao;


/**
 * 카카오 API 설정. REST API 키는 local.properties의 kakao.restApiKey로 주입한다(저장소에 넣지 않음).
 * 길찾기(카카오모빌리티)와 장소 검색(카카오 로컬)은 같은 REST API 키를 쓴다.
 */
public record KakaoProperties(String restApiKey, String directionsUrl, String localKeywordUrl) {

    public KakaoProperties {
        if (directionsUrl == null || directionsUrl.trim().isEmpty()) {
            directionsUrl = "https://apis-navi.kakaomobility.com/v1/directions";
        }
        if (localKeywordUrl == null || localKeywordUrl.trim().isEmpty()) {
            localKeywordUrl = "https://dapi.kakao.com/v2/local/search/keyword.json";
        }
    }

    public boolean configured() {
        return restApiKey != null && !restApiKey.trim().isEmpty();
    }

    public String authorization() {
        return "KakaoAK " + restApiKey;
    }
}
