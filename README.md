# 차차 — 초보 운전 연습 코치 (Pleos Connect 차량용 앱)

초보 운전자가 **연습 목표·긴장도에 맞는 연습 경로**를 고르고, 출발 전에 **복잡 구간 브리핑·준비 체크리스트·연습 모드 차량 환경**까지 갖춘 뒤 출발하도록 돕는 Pleos Connect 앱입니다.

> 현대자동차그룹 차량용 앱 개발 공모전 출품작 · Android(Java) · Pleos Connect SDK

## 현재 구현 범위 (2단계: 출발 전 흐름)

| 흐름 | 화면 | 사용 SDK |
|---|---|---|
| 온보딩 | 권한 동의(목적별) → 비동의 시 기능 제한 안내 → 운전 경력 → 어려운 점 → 안내량 | – |
| 홈 | 오늘의 추천 연습, 차량 상태, 주행 가능 거리, 실내 온도 | Vehicle |
| 연습 설정 | 목표 유형(복수) → 선택한 유형의 세부 설정(기술 / 거리·시간 / 목적지 / 주행 환경) → 난이도·지역 익숙함 → 긴장도·이유 | STT(말로 답하기) |
| 경로 추천 | 후보 3개(넓은 길 / 최단 / 목표 연습), 추천 이유·난이도 점수 근거, 선택 경로를 내비에 전달 | FusedLocation, NaviHelper |
| 복잡 구간 브리핑 | 주행 순서대로 구간 목록 + 경로 미리보기 + 음성 브리핑, 다시 듣기 | TTS |
| 준비 체크리스트 | 안전벨트·문·연료/배터리 자동 확인, 나머지는 직접 확인, 필수 미완료 시 출발 차단 | Vehicle |
| 연습 모드 프리셋 | 목표·날씨·시간 기반 4종 추천 → 항목 수정 → 적용 전 설정 저장 → 일괄 적용 → 실패 항목 안내 | Vehicle(HVAC·Window·SteeringWheel) |
| 출발 | 설정 요약(주행 중 화면은 다음 단계) | – |

긴장도 4 이상이면 **난이도를 한 단계 낮춰 경로를 추천**하고 **안내 수준을 한 단계 올립니다**(유저플로우 반영).

### 기본 버전 / 상세 버전

설정 › 앱 버전에서 고를 수 있습니다(기본값: 상세 버전, 카카오 키가 없는 빌드는 기본 버전). 별도 서버는 필요 없습니다.

| | 기본 버전 | 상세 버전 |
|---|---|---|
| 사용 | Pleos SDK + 기기 내 로직 | 기본 버전 + 카카오모빌리티 길찾기·카카오 로컬·기상청 API(앱이 직접 호출) |
| 외부 통신 | 없음 | 있음(인터넷 필요) |
| 경로 추천·브리핑 | 기기 내 추정 | 실제 도로의 회전 안내 지점으로 좌회전·유턴·합류·복잡 교차로·정체를 분석해 난이도 점수와 브리핑 생성 |
| 장소 검색 | 예시 장소 | 카카오 장소 검색 |
| 날씨 | – | 기상청 초단기실황(악천후 프리셋 추천) |

상세 버전에서 외부 API가 응답하지 않으면 자동으로 기기 내 로직으로 동작합니다. 음성 인식은 공모전 규정에 따라 **Pleos SpeechToText만** 사용합니다(외부 음성 인식으로 대체하지 않음).

## 실행 방법

1. Android Studio(Gradle 8+ / AGP 9.5) · JDK 17 이상
2. Pleos Connect Emulator **v2.1.0** (Automotive, API 34, RAM 4GB, Cold boot) 설치
   - SDK Update Site: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml` (Mac)
3. 프로젝트 열기 → `app` 실행 (Pleos 에뮬레이터 또는 일반 에뮬레이터)
4. `local.properties`에 키를 넣습니다(저장소에는 올라가지 않음).
   ```properties
   # Pleos Playground 내 프로젝트의 SDK 클라이언트 정보 → TTS/STT HYBRID 모드
   pleos.clientId=...
   pleos.clientSecret=...
   # 상세 버전: 카카오 REST API 키, 기상청 키(API허브 authKey 또는 공공데이터포털 Decoding 키)
   kakao.restApiKey=...
   kma.serviceKey=...
   ```
   Pleos 키를 비워 두면 ON_DEVICE 모드로, 카카오 키를 비워 두면 기본 버전만 동작합니다.

### 테스트 환경 · 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator **v2.1.0** (Android 14 / API 34, arm64) |
| 화면 사양 | 전체 **2560 × 1440**, 160dpi(1dp = 1px) |
| 앱 표시 영역 | 멀티 윈도 오른쪽 **1682 × 1210** (화면 좌표 853,90 – 2535,1300) |
| 패키지명 | `com.chacha.app` (앱 이름 "차차") |
| minSdk / compileSdk / Java | 28 / 37 / 17 |

- **위치 설정 필수**: Pleos 에뮬레이터의 FusedLocation은 기본 GPS 위치(미국 마운틴뷰)를 돌려주고, 내비 엔진은 서울 근처를 씁니다. 에뮬레이터 Extended Controls › Location에서 서울 좌표를 지정하거나 아래 명령을 실행하세요.
  ```bash
  adb emu geo fix 127.0947 37.4127   # 내비 엔진의 기본 차량 위치(성남) 근처
  ```
  위치가 목적지에서 100km 넘게 떨어져 있으면 앱이 출발지를 직접 고르도록 안내합니다.
- **안전벨트**: Pleos 에뮬레이터는 운전석 안전벨트가 미착용으로 고정돼 있고 Virtual Console(v2.1.0)에도 바꾸는 기능이 없습니다(경고등만 조작 가능). 그래서 에뮬레이터에서는 체크리스트의 안전벨트를 **직접 확인** 항목으로 보여 줍니다. 실제 차량에서는 센서 값으로 자동 확인하고, 미착용이면 출발을 막습니다.
- **음성**: 에뮬레이터에는 온디바이스 TTS 엔진이 없어 ON_DEVICE 모드에서는 음성이 나오지 않습니다(화면 안내는 정상). HYBRID 모드(위 4번)로 확인하세요.
- **인터넷(상세 버전)**: 에뮬레이터를 켠 뒤 PC의 네트워크(Wi-Fi 등)가 바뀌면 에뮬레이터 DNS가 예전 값으로 남아 외부 API에 접속하지 못합니다. 이때는 에뮬레이터를 Cold Boot 하세요.
- Pleos 시스템 서비스가 없는 기기에서는 가상 차량·내비·위치(시연 장소 좌표)로 자동 전환되며, 오른쪽 위에 "시뮬레이션 모드"가 표시됩니다.

### 테스트

```bash
./gradlew :app:testDebugUnitTest            # 단위 테스트 54개(도메인 로직 + 상세 버전 경로 분석)
./gradlew :app:connectedDebugAndroidTest    # Pleos 에뮬레이터 연동·UI 스모크 테스트 3개
```

## 백엔드 (선택)

상세 버전의 분석 로직은 앱 안(`data/direct/`)에서 실행되므로 **서버 없이 동작**합니다.
`backend/` 폴더에는 같은 로직을 Spring Boot 서버로 제공하는 버전이 있습니다. 키를 서버에만 두고 싶을 때 `local.properties`에서 `kakao.restApiKey`를 비우고 `api.baseUrl`에 서버 주소를 넣으면 앱이 서버를 사용합니다. 자세한 내용은 [`backend/README.md`](backend/README.md)를 보세요.

## 구조

```
app/src/main/java/com/example/application/
├── App.java · MainActivity.java       # 앱 진입점(SDK 초기화), 단일 Activity
├── di/ServiceLocator.java             # 의존성 구성: Pleos 서비스 유무에 따라 실제/가상 구현 선택
├── core/
│   ├── pleos/                         # Pleos SDK 래퍼(인터페이스 + Pleos 구현 + Fake 구현)
│   │   ├── vehicle/  navi/  location/  voice/
│   ├── vehiclecontrol/                # 프리셋 적용(적용 전 저장 → 일괄 적용 → 결과 보고)
│   └── Callback · MainThread
├── domain/                            # Android 의존 없는 규칙(단위 테스트 대상)
│   ├── SetupFlow                      # 단계 순서·검증
│   ├── RouteCandidateGenerator · RouteDifficultyScorer · BriefingGenerator
│   ├── PresetRecommender · PresetTargets · ChecklistEvaluator
│   └── VoiceCommandParser · VoiceVocabulary · TodayRecommendation
├── data/                              # 로컬 저장(프로필·동의 이력·세션), 경로/장소/날씨 저장소
│   └── direct/                        # 상세 버전: 카카오·기상청 직접 호출 + 경로 분석(백엔드와 같은 로직)
├── model/                             # 도메인 모델
└── ui/                                # 화면(Fragment): onboarding · home · setup · depart · settings
```

- 경로 추천·장소 검색·날씨는 상세 버전일 때 `data/direct/DirectApiClient`가 카카오·기상청을 직접 호출하고, 실패하면 기기 내 로직으로 자동 전환합니다.
- 디자인은 Figma '연습주행_가로세로'(가로 1700 / 세로 840 기준)를 따릅니다. 색·글자 크기·간격은 `res/values`의 pleos 토큰, 글꼴은 Pleos 시스템 글꼴 Asta Sans(시스템 이름 `pretendard`)를 씁니다. 앱 영역 폭이 1100dp보다 좁으면 세로(P) 배치로 바뀝니다.

## Pleos SDK 사용 시 확인한 점 (문서와 다른 부분)

- `TextToSpeech`/`SpeechToText`는 `pleos.car.permission.TTS_SERVICE` / `STT_SERVICE` 선언이 필요합니다(없으면 `initialize()`에서 SecurityException).
- `pleos.car.permission.FUSED_LOCATION`은 **dangerous** 보호 수준이라 런타임 권한 요청이 필요합니다.
- `NaviHelper.requestNavigationScreen()`(예: 경로 전체 보기 `OverviewRoute`)은 문서에 없는 `pleos.car.permission.NAVI_CUSTOM_MAP` 권한이 필요합니다. 또 경로가 설정돼 있어야 전체 보기가 열려서, 경로를 먼저 보낸 뒤 요청합니다.
- NaviHelper는 출발 전에 경로의 도로 좌표·회전 목록을 앱에 돌려주지 않습니다(`requestTrip`은 거리·시간·정체 비율만). 그래서 기본 버전은 앱 화면에 실제 경로를 그리지 않고 내비 화면으로 보여 줍니다.
- 공조 온도·풍량·전원은 운전석 단독 영역(0x1)이 아니라 차량별 공조 구역을 써야 합니다 → `check…Capabilities`로 운전석을 포함하는 영역을 찾아 사용합니다.
- 공조 전원이 꺼져 있으면 온도를 읽을 수 없습니다(`PropertyNotAvailableException`). 프리셋은 공조 전원을 먼저 켭니다.
- `EV_BATTERY_LEVEL`은 % 가 아니라 Wh 단위입니다 → `getEvBatteryCapacity()`로 나눠 %로 표시합니다. `RANGE_REMAINING`은 미터 단위입니다.
- 에뮬레이터 차량은 전기차라 `FUEL_PERCENT`가 지원되지 않습니다.
- Pleos 런처가 앱을 여러 번 실행할 수 있어 `MainActivity`는 `singleTask`입니다.
