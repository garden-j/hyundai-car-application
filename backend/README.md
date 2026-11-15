# 백엔드 (Spring Boot)

연습 경로 추천·복잡 구간 브리핑·장소 검색·날씨를 제공합니다. 외부 API 키가 없거나 장애가 나면 503을 돌려주고, 앱은 기기 내 로직으로 자동 전환합니다.

## API

| Method | Path | 설명 | 외부 API |
|---|---|---|---|
| POST | `/api/v1/routes/recommend` | 연습 경로 후보 최대 3개(넓은 길 / 최단 / 목표 연습) + 각 경로의 실제 회전 지점 기반 브리핑 | 카카오모빌리티 길찾기 |
| GET | `/api/v1/places/search?q=&lat=&lng=` | 장소 검색(위치가 있으면 가까운 순) | 카카오 로컬 키워드 검색 |
| GET | `/api/v1/weather/current?lat=&lng=` | 현재 기온·강수(프리셋 추천의 악천후 판단) | 기상청 초단기실황 |
| GET | `/actuator/health` | 상태 확인 | – |

오류 응답은 `{"code": "...", "message": "..."}` 형식입니다.

| code | HTTP | 의미 | 앱 동작 |
|---|---|---|---|
| `ROUTING_UNAVAILABLE`, `ROUTING_NO_RESULT`, `PLACES_UNAVAILABLE`, `WEATHER_UNAVAILABLE` | 503 | 키 미설정·외부 장애·결과 없음 | 기기 내 로직으로 대체 |
| `BAD_REQUEST` | 400 | 목적지가 출발지에서 100km 초과 등 | 안내 문구 표시 |
| `INVALID_REQUEST` | 400 | 요청 형식 오류 | – |

### 경로 추천 방식

- **목적지 있음**: 추천(대안 경로 포함)·최단거리·유턴 회피(유턴 연습이 목표면 최단시간) 조건으로 경로를 받아 비교합니다.
- **목적지 없음**: 출발지에서 방향이 다른 삼각 코스 3개(경유지 2개)를 받아 비교합니다.
- 경로마다 카카오 안내 타입(`guides[].type`)으로 좌회전·유턴·합류(고속도로 진입·분기 후 합류)·차로 맞추기(방향 유지·출구)·복잡한 교차로(로터리·회전교차로·n시 방향)를 세고, 정체 구간 비율을 더해 난이도 점수를 냅니다. 가중치는 앱과 같습니다.
- 그중 난이도 최저(넓은 길) / 최단 거리 / 선택한 기술을 가장 많이 경험하는 경로(목표 연습)를 고릅니다. 긴장도 4 이상이면 난이도를 한 단계 낮춰 판단합니다.

## 실행

```bash
cp .env.example .env   # 키 입력
export $(grep -v '^#' .env | xargs)
./gradlew bootRun      # http://localhost:8080
```

앱에서 쓰려면 앱 프로젝트의 `local.properties`에 서버 주소를 넣습니다(에뮬레이터에서 PC는 `10.0.2.2`).

```properties
api.baseUrl=http://10.0.2.2:8080
```

Docker:

```bash
docker build -t practice-drive-backend .
docker run -p 8080:8080 --env-file .env practice-drive-backend
```

### 키 발급

- **카카오 REST API 키**: [developers.kakao.com](https://developers.kakao.com) › 내 애플리케이션 › 앱 키 › REST API 키. 같은 키로 카카오모빌리티 길찾기(`apis-navi.kakaomobility.com`)와 로컬 검색을 호출합니다. 카카오모빌리티 API는 콘솔에서 사용 설정이 필요할 수 있습니다.
- **기상청 인증키**: 두 발급처 중 하나. 키 형태(길이)로 자동 판별합니다.
  - [기상청 API허브](https://apihub.kma.go.kr) authKey(22자): 회원가입 후 '단기예보 조회서비스(VilageFcstInfoService_2.0)'의 초단기실황조회 **활용신청**이 필요합니다.
  - [공공데이터포털](https://www.data.go.kr/data/15084084/openapi.do) '기상청_단기예보 조회서비스' 활용 신청 → 일반 인증키(Decoding).

## 테스트

```bash
./gradlew test   # 23개: 카카오 응답 파싱, 경로 분석·점수·브리핑, 후보 선택, 기상청 격자·파싱, API(MockMvc)
```

## 구조

```
src/main/java/com/example/backend/
├── api/            # 컨트롤러, DTO, 오류 처리
├── routing/        # 경로 분석(안내 타입 분류·난이도)·후보 선택·브리핑·추천 서비스
├── external/kakao/ # 카카오모빌리티 길찾기, 카카오 로컬 검색 클라이언트
├── external/kma/   # 기상청 초단기실황, 위경도→격자 변환
├── place/ weather/ # 서비스
└── config/         # HTTP 타임아웃(연결 3초, 응답 5초)
```

## 확인이 더 필요한 부분

- 실제 카카오·기상청 키로는 아직 호출해 보지 않았습니다. 응답 형식은 공식 문서(카카오모빌리티 `guide/navi-api`) 예시로 검증했습니다.
- 기상청 초단기실황 오퍼레이션 경로(`getUltraSrtNcst`), 발표 시각(매시 40분 이후), 격자 변환 상수는 널리 쓰이는 값을 사용했습니다. 공공데이터포털 활용가이드로 재확인이 필요합니다.
- 공모전 규정상 외부 서버 사용이 허용되는지 주최 측 확인이 필요합니다(앱은 서버 없이도 동작).
