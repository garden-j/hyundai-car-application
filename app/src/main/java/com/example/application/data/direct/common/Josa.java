package com.example.application.data.direct.common;

/** 한국어 조사 선택(받침 유무). */
public final class Josa {
    private Josa() {
    }

    public static boolean hasBatchim(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char c = word.charAt(word.length() - 1);
        if (c < 0xAC00 || c > 0xD7A3) {
            return "013678".indexOf(c) >= 0;
        }
        return (c - 0xAC00) % 28 != 0;
    }

    public static String ieyo(String word) {
        return word + (hasBatchim(word) ? "이에요" : "예요");
    }

    public static String eseo(String place) {
        return place + "에서";
    }
}
