package com.example.application.domain;

import com.example.application.model.DifficultSituation;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Labeled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 주행 전 기본 지식(운전 지식 탭). 카테고리별 항목과 짧은 음성 안내 문장.
 * 기획 확정 전 초안 문구이며, 이 파일만 바꾸면 내용이 갱신된다.
 */
public final class KnowledgeCatalog {

    public enum Category implements Labeled {
        BASICS("기본 조작"), SIGNALS("신호·교차로"), LANES("차로 변경·합류"), PARKING("주차"), DANGER("위험 상황");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    public static final class Item {
        public final Category category;
        public final String title;
        public final String summary;
        public final String speech;
        /** 관련 연습 기술(추천용). */
        public final DrivingSkill skill;

        Item(Category category, String title, String summary, String speech, DrivingSkill skill) {
            this.category = category;
            this.title = title;
            this.summary = summary;
            this.speech = speech;
            this.skill = skill;
        }
    }

    private static final List<Item> ITEMS = Collections.unmodifiableList(Arrays.asList(
            new Item(Category.BASICS, "출발 전 자세 맞추기", "브레이크를 끝까지 밟아도 무릎이 살짝 굽혀지게",
                    "시트는 브레이크를 끝까지 밟았을 때 무릎이 살짝 굽혀지게, 등받이는 손목이 핸들 위에 닿게 맞춰요. 사이드미러는 차 옆면이 살짝 보이게 맞춰요.", null),
            new Item(Category.BASICS, "부드럽게 출발하고 멈추기", "가속은 천천히, 브레이크는 미리 나눠서",
                    "출발할 때는 가속 페달을 천천히 밟고, 멈출 때는 미리 브레이크를 나눠 밟아요. 앞차 뒷바퀴가 보일 만큼 거리를 두고 서요.", null),
            new Item(Category.BASICS, "방향지시등 켜는 때", "바꾸기 3초 전, 30미터 앞에서",
                    "차로를 바꾸거나 돌기 전에는 최소 3초 전, 시내에서는 30미터 앞에서 방향지시등을 켜요.", DrivingSkill.LANE_CHANGE),
            new Item(Category.SIGNALS, "비보호 좌회전", "녹색 신호에서 맞은편 차가 끊길 때",
                    "비보호 좌회전은 녹색 신호일 때 맞은편 차와 보행자가 없을 때만 해요. 서두르지 말고 다음 신호를 기다려도 괜찮아요.", DrivingSkill.LEFT_TURN),
            new Item(Category.SIGNALS, "우회전할 때 멈추기", "횡단보도 보행자 확인 후 서행",
                    "우회전할 때는 횡단보도에 보행자가 있으면 반드시 멈춰요. 우회전 신호등이 있으면 신호를 따르고, 빨간불에는 일단 멈췄다가 가요.", DrivingSkill.RIGHT_TURN),
            new Item(Category.SIGNALS, "유턴하는 방법", "유턴 표지 있는 1차로에서",
                    "유턴은 유턴 표지가 있는 곳의 1차로에서 해요. 표지에 적힌 신호 조건을 확인하고, 맞은편 차가 끊길 때 핸들을 끝까지 돌려 크게 돌아요.", DrivingSkill.U_TURN),
            new Item(Category.SIGNALS, "복잡한 교차로 지나기", "바닥 화살표로 차로 미리 맞추기",
                    "복잡한 교차로는 미리 바닥 화살표를 보고 가야 할 방향의 차로로 옮겨 둬요. 교차로 안에서는 차로를 바꾸지 않아요.", DrivingSkill.INTERSECTION),
            new Item(Category.LANES, "차로 바꾸는 순서", "지시등 → 미러 → 어깨 너머 → 천천히",
                    "차로를 바꿀 때는 방향지시등을 켜고, 사이드미러와 어깨 너머로 사각지대를 확인한 뒤 천천히 옮겨요.", DrivingSkill.LANE_CHANGE),
            new Item(Category.LANES, "합류 구간", "본선 속도에 맞춰 들어가기",
                    "합류할 때는 가속 차로에서 본선 차와 속도를 맞추고, 들어갈 틈이 보이면 방향지시등을 켜고 들어가요. 합류 차로 끝에서 멈추지 않도록 미리 살펴요.", DrivingSkill.MERGE),
            new Item(Category.PARKING, "후진 주차 기본", "옆 차 뒤 범퍼와 나란할 때 핸들",
                    "후진 주차는 내 차 뒤 범퍼가 주차 칸 옆 차의 앞쪽과 나란해질 때 핸들을 돌리고, 사이드미러로 주차선과 평행해지면 핸들을 풀어요.", DrivingSkill.PARKING),
            new Item(Category.PARKING, "평행 주차 기본", "앞차와 나란히 선 뒤 천천히 후진",
                    "평행 주차는 앞차와 나란히 선 뒤 천천히 후진하다가, 내 차 뒷바퀴가 앞차 뒤 범퍼를 지나면 핸들을 돌려 들어가요.", DrivingSkill.PARKING),
            new Item(Category.DANGER, "비 오는 날", "거리 두 배, 급브레이크 금지",
                    "비 오는 날은 앞차와 거리를 평소의 두 배로 두고, 급하게 핸들을 돌리거나 브레이크를 밟지 않아요. 전조등을 켜면 내 차가 잘 보여요.", null),
            new Item(Category.DANGER, "밤에 운전할 때", "전조등 켜고 보행자 더 천천히",
                    "밤에는 전조등을 켜고, 보행자와 자전거를 더 천천히 살펴요. 맞은편 차가 오면 상향등을 꺼요.", null),
            new Item(Category.DANGER, "당황했을 때", "비상등 켜고 안전한 곳에 정차",
                    "길을 잘못 들었거나 당황했다면 무리하게 차로를 바꾸지 말고 그대로 가요. 내비가 다시 길을 찾아 줘요. 쉬고 싶으면 비상등을 켜고 안전한 곳에 세워요.", null)));

    private KnowledgeCatalog() {
    }

    public static List<Item> all() {
        return ITEMS;
    }

    public static List<Item> of(Category c) {
        List<Item> out = new ArrayList<>();
        for (Item i : ITEMS) {
            if (i.category == c) {
                out.add(i);
            }
        }
        return out;
    }

    /** 연습 목표·어려워한 상황과 관련된 항목(최대 3개). */
    public static List<Item> recommended(Set<DrivingSkill> skills, Set<DifficultSituation> difficulties) {
        Set<DrivingSkill> wanted = new LinkedHashSet<>(skills == null ? Collections.emptySet() : skills);
        if (difficulties != null) {
            for (DifficultSituation d : difficulties) {
                if (d.relatedSkill != null) {
                    wanted.add(d.relatedSkill);
                }
            }
        }
        List<Item> out = new ArrayList<>();
        for (Item i : ITEMS) {
            if (i.skill != null && wanted.contains(i.skill) && out.size() < 3) {
                out.add(i);
            }
        }
        if (out.isEmpty()) {
            out.add(ITEMS.get(0));
            out.add(ITEMS.get(2));
        }
        return out;
    }
}
