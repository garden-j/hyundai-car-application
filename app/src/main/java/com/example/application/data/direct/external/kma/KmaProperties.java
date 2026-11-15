package com.example.application.data.direct.external.kma;


/**
 * 기상청 단기예보 조회서비스 설정. 인증키는 local.properties의 kma.serviceKey로 주입한다.
 * 두 발급처를 모두 지원한다: 기상청 API허브(apihub.kma.go.kr, 22자 authKey)와 공공데이터포털(일반 인증키 Decoding).
 * 주소를 따로 지정하지 않으면 키 형태로 발급처를 고른다.
 */
public record KmaProperties(String serviceKey, String nowcastUrl) {

    public KmaProperties {
        if (nowcastUrl == null || nowcastUrl.trim().isEmpty()) {
            nowcastUrl = looksLikeApiHubKey(serviceKey) ? API_HUB_URL : DATA_GO_KR_URL;
        }
    }

    static final String API_HUB_URL =
            "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst";
    static final String DATA_GO_KR_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

    /** API허브 키는 22자 내외의 짧은 문자열, 공공데이터포털 키는 수십 자 이상이다. */
    static boolean looksLikeApiHubKey(String key) {
        return key != null && !key.trim().isEmpty() && key.trim().length() <= 32;
    }

    /** 인증키 쿼리 파라미터 이름: API허브는 authKey, 공공데이터포털은 serviceKey. */
    public String keyParam() {
        return nowcastUrl.contains("apihub.kma.go.kr") ? "authKey" : "serviceKey";
    }

    public boolean configured() {
        return serviceKey != null && !serviceKey.trim().isEmpty();
    }
}
