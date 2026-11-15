package com.example.application.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 차량 기능 조작 안내(차량 도움 탭). 자주 묻는 기능을 정리한 안내와 키워드 검색.
 * Pleos LLM SDK가 없어 정해진 문장으로 답한다. 차종마다 위치가 다를 수 있어 일반적인 위치로 안내한다.
 */
public final class VehicleFaq {

    public static final class Entry {
        public final String title;
        public final String answer;
        final List<String> keywords;

        Entry(String title, String answer, String... keywords) {
            this.title = title;
            this.answer = answer;
            this.keywords = Arrays.asList(keywords);
        }
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry("와이퍼 켜기", "핸들 오른쪽 뒤 레버를 아래로 내리면 와이퍼가 켜져요. 한 칸씩 내릴수록 빨라지고, 위로 올리면 꺼져요.", "와이퍼", "비", "앞유리 닦"),
            new Entry("워셔액 뿌리기", "와이퍼 레버를 몸 쪽으로 당기면 워셔액이 나오고 와이퍼가 함께 움직여요.", "워셔", "앞유리 더러"),
            new Entry("전조등 켜기", "핸들 왼쪽 뒤 레버 끝을 돌려 전조등 표시에 맞춰요. AUTO에 두면 어두울 때 자동으로 켜져요.", "전조등", "라이트", "헤드라이트", "불 켜"),
            new Entry("상향등 끄고 켜기", "왼쪽 레버를 앞으로 밀면 상향등이 켜지고 당기면 꺼져요. 맞은편 차가 오면 꼭 꺼 주세요.", "상향등", "하이빔"),
            new Entry("방향지시등", "왼쪽 레버를 위로 올리면 오른쪽, 아래로 내리면 왼쪽 방향지시등이 켜져요.", "깜빡이", "방향지시등", "지시등"),
            new Entry("비상등", "센터페시아 가운데 빨간 삼각형 버튼을 누르면 비상등이 켜져요. 다시 누르면 꺼져요.", "비상등", "삼각형"),
            new Entry("앞유리 김서림 제거", "공조 패널의 앞유리 김서림 버튼을 누르면 바람이 앞유리로 나와요. 에어컨을 함께 켜면 더 빨리 없어져요.", "김서림", "성에", "뿌옇", "서리"),
            new Entry("뒷유리 열선", "공조 패널의 뒷유리 열선 버튼을 누르면 뒷유리 김서림이 사라져요.", "뒷유리", "열선 뒤"),
            new Entry("에어컨·히터", "공조 화면이나 버튼에서 온도를 맞추고 AUTO를 누르면 알아서 바람 세기를 조절해요.", "에어컨", "히터", "온도", "춥", "덥"),
            new Entry("핸들 열선", "공조 화면이나 핸들 근처의 핸들 열선 버튼을 누르면 켜져요.", "핸들 열선", "핸들 따뜻"),
            new Entry("사이드미러 조절", "운전석 문의 미러 조절 스위치로 왼쪽·오른쪽을 고르고 방향 버튼으로 맞춰요. 차 옆면이 살짝 보이게 맞춰요.", "사이드미러", "미러", "거울"),
            new Entry("주차 브레이크", "기어 근처나 왼쪽 아래의 P 표시 스위치를 당기면 걸리고, 브레이크를 밟은 채 누르면 풀려요.", "주차 브레이크", "사이드 브레이크", "파킹"),
            new Entry("주유구·충전구 열기", "운전석 아래 주유구 버튼을 누르거나, 전기차는 충전구 덮개를 눌러서 열어요.", "주유구", "충전구", "기름 넣"),
            new Entry("트렁크 열기", "운전석 문 쪽의 트렁크 버튼을 누르거나, 스마트키의 트렁크 버튼을 길게 눌러요.", "트렁크"),
            new Entry("시동 걸기", "브레이크를 끝까지 밟은 채 시동 버튼을 눌러요. 계기판에 READY나 시동 표시가 뜨면 준비된 거예요.", "시동", "출발 안", "안 켜")));

    private VehicleFaq() {
    }

    public static List<Entry> all() {
        return ENTRIES;
    }

    /** 질문(음성 인식 결과 등)에서 키워드가 가장 많이 맞는 항목들. 없으면 빈 목록. */
    public static List<Entry> search(String question) {
        List<Entry> out = new ArrayList<>();
        if (question == null) {
            return out;
        }
        String q = question.replace(" ", "");
        int best = 0;
        for (Entry e : ENTRIES) {
            int score = 0;
            for (String k : e.keywords) {
                if (q.contains(k.replace(" ", ""))) {
                    score++;
                }
            }
            if (score > best) {
                best = score;
                out.clear();
                out.add(e);
            } else if (score > 0 && score == best) {
                out.add(e);
            }
        }
        return out;
    }
}
