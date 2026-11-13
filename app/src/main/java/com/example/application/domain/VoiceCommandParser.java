package com.example.application.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STT 결과 문장에서 화면 선택지를 찾는 간단한 규칙 기반 파서.
 * 출발 전 설정 화면의 "말로 답해도 돼요" 기능에 사용한다(복잡한 질의는 서버 LLM이 담당).
 */
public final class VoiceCommandParser {
    private VoiceCommandParser() {
    }

    /** 공백·문장부호를 없애고 소문자로 바꾼다. "U 턴!" → "u턴" */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}·,.!?~]", "");
    }

    private static boolean containsAny(String norm, String... words) {
        for (String w : words) {
            if (norm.contains(normalize(w))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isYes(String text) {
        String n = normalize(text);
        if (isNo(text)) {
            return false;
        }
        return containsAny(n, "네", "예", "응", "그래", "좋아", "이대로", "맞아", "해줘", "할게", "적용");
    }

    public static boolean isNo(String text) {
        String n = normalize(text);
        return containsAny(n, "아니", "싫어", "안할래", "괜찮아요안", "취소", "그만");
    }

    public static boolean isNext(String text) {
        return containsAny(normalize(text), "다음", "넘어가", "계속");
    }

    public static boolean isBack(String text) {
        return containsAny(normalize(text), "이전", "뒤로", "돌아가");
    }

    /**
     * 사전(키워드 → 값)에서 문장에 들어 있는 값을 모두 찾는다. 사전 순서를 유지한다.
     * 키워드는 쉼표로 여러 개 줄 수 있다("유턴,u턴").
     */
    public static <T> Set<T> match(String text, Map<String, T> vocabulary) {
        String n = normalize(text);
        Set<T> out = new LinkedHashSet<>();
        for (Map.Entry<String, T> e : vocabulary.entrySet()) {
            for (String kw : e.getKey().split(",")) {
                if (!kw.isEmpty() && n.contains(normalize(kw))) {
                    out.add(e.getValue());
                    break;
                }
            }
        }
        return out;
    }

    public static <T> T matchFirst(String text, Map<String, T> vocabulary) {
        Set<T> s = match(text, vocabulary);
        return s.isEmpty() ? null : s.iterator().next();
    }

    private static final Pattern DIGITS = Pattern.compile("(\\d+)");
    private static final Pattern SINO = Pattern.compile(
            "((?:[이삼사오육칠팔구]?십)?[일이삼사오육칠팔구]?)(분|킬로|키로|km|단계|번|점|정도)");
    private static final Map<String, Integer> NATIVE = new LinkedHashMap<>();

    static {
        // 긴 단어부터 검사해야 "스물다섯"이 "다섯"보다 먼저 잡힌다
        String[][] tens = {{"서른", "30"}, {"스물", "20"}, {"열", "10"}};
        String[][] ones = {{"아홉", "9"}, {"여덟", "8"}, {"일곱", "7"}, {"여섯", "6"}, {"다섯", "5"},
                {"넷", "4"}, {"네", "4"}, {"셋", "3"}, {"세", "3"}, {"둘", "2"}, {"두", "2"}, {"하나", "1"}, {"한", "1"}};
        for (String[] t : tens) {
            for (String[] o : ones) {
                NATIVE.put(t[0] + o[0], Integer.parseInt(t[1]) + Integer.parseInt(o[1]));
            }
        }
        for (String[] t : tens) {
            NATIVE.put(t[0], Integer.parseInt(t[1]));
        }
        for (String[] o : ones) {
            NATIVE.put(o[0], Integer.parseInt(o[1]));
        }
    }

    /**
     * 문장에서 첫 번째 숫자를 찾는다. "25분", "3 정도야", "이십오분", "세 번째", "다섯" 지원. 없으면 null.
     * 한자어 수("삼")는 단위가 붙은 경우만 인정한다("이대로" 같은 오인식 방지).
     */
    public static Integer parseNumber(String text) {
        String n = normalize(text);
        Matcher d = DIGITS.matcher(n);
        if (d.find()) {
            try {
                return Integer.parseInt(d.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        Matcher s = SINO.matcher(n);
        while (s.find()) {
            Integer v = sinoValue(s.group(1));
            if (v != null) {
                return v;
            }
        }
        for (Map.Entry<String, Integer> e : NATIVE.entrySet()) {
            String w = e.getKey();
            int idx = n.indexOf(w);
            if (idx < 0) {
                continue;
            }
            // "네"는 대답(네, 그래요)과 겹치므로 뒤에 단위가 올 때만 숫자로 본다
            if ((w.equals("네") || w.equals("한") || w.equals("두") || w.equals("세"))
                    && !followedByUnit(n, idx + w.length())) {
                continue;
            }
            return e.getValue();
        }
        return null;
    }

    private static boolean followedByUnit(String n, int pos) {
        String rest = n.substring(Math.min(pos, n.length()));
        return rest.startsWith("번") || rest.startsWith("시간") || rest.startsWith("정도")
                || rest.startsWith("단계") || rest.startsWith("개") || rest.startsWith("곳") || rest.startsWith("점");
    }

    private static Integer sinoValue(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String digits = "일이삼사오육칠팔구";
        int value = 0;
        int idx = s.indexOf('십');
        if (idx >= 0) {
            int tens = idx == 0 ? 1 : digits.indexOf(s.charAt(idx - 1)) + 1;
            value += tens * 10;
            s = s.substring(idx + 1);
        }
        if (!s.isEmpty()) {
            int o = digits.indexOf(s.charAt(s.length() - 1));
            if (o < 0) {
                return null;
            }
            value += o + 1;
        }
        return value == 0 ? null : value;
    }
}
