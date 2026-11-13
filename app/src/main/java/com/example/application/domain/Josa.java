package com.example.application.domain;

/** 한국어 조사 선택 도우미(받침 유무에 따라 이에요/예요, 을/를 등). */
public final class Josa {
    private Josa() {
    }

    public static boolean hasBatchim(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char c = word.charAt(word.length() - 1);
        if (c < 0xAC00 || c > 0xD7A3) {
            // 숫자·영문 등: 숫자는 읽는 소리 기준
            return "013678".indexOf(c) >= 0;
        }
        return (c - 0xAC00) % 28 != 0;
    }

    /** "교차로" → "교차로예요", "구간" → "구간이에요" */
    public static String ieyo(String word) {
        return word + (hasBatchim(word) ? "이에요" : "예요");
    }

    /** "기술" → "기술을", "주차" → "주차를" */
    public static String eulReul(String word) {
        return word + (hasBatchim(word) ? "을" : "를");
    }
}
