package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.PracticeGoalType;

import org.junit.Test;

import java.util.Set;

public class VoiceCommandParserTest {

    @Test
    public void numbers() {
        assertEquals(Integer.valueOf(25), VoiceCommandParser.parseNumber("25분"));
        assertEquals(Integer.valueOf(3), VoiceCommandParser.parseNumber("3 정도야"));
        assertEquals(Integer.valueOf(25), VoiceCommandParser.parseNumber("이십오분"));
        assertEquals(Integer.valueOf(3), VoiceCommandParser.parseNumber("삼 정도"));
        assertEquals(Integer.valueOf(5), VoiceCommandParser.parseNumber("다섯"));
        assertEquals(Integer.valueOf(2), VoiceCommandParser.parseNumber("두 번째 길"));
        assertEquals(Integer.valueOf(10), VoiceCommandParser.parseNumber("십 킬로"));
    }

    @Test
    public void wordsThatLookLikeNumbersAreIgnored() {
        assertNull(VoiceCommandParser.parseNumber("이대로 해 줘"));
        assertNull(VoiceCommandParser.parseNumber("네 좋아요"));
    }

    @Test
    public void goalTypesFromSentence() {
        Set<PracticeGoalType> s = VoiceCommandParser.match("주행 기술과 목적지", VoiceVocabulary.GOAL_TYPES);
        assertTrue(s.contains(PracticeGoalType.SKILL));
        assertTrue(s.contains(PracticeGoalType.DESTINATION));
        assertEquals(2, s.size());
    }

    @Test
    public void skillsWithSynonyms() {
        Set<DrivingSkill> s = VoiceCommandParser.match("U턴이랑 차선 변경", VoiceVocabulary.SKILLS);
        assertTrue(s.contains(DrivingSkill.U_TURN));
        assertTrue(s.contains(DrivingSkill.LANE_CHANGE));
    }

    @Test
    public void yesNo() {
        assertTrue(VoiceCommandParser.isYes("이대로 해 줘"));
        assertTrue(VoiceCommandParser.isYes("네"));
        assertFalse(VoiceCommandParser.isYes("아니요"));
        assertTrue(VoiceCommandParser.isNo("아니 괜찮아"));
    }

    @Test
    public void difficultyAndTension() {
        assertEquals(Difficulty.EASY, VoiceCommandParser.matchFirst("쉬움으로 할래", VoiceVocabulary.DIFFICULTIES));
        assertEquals(Integer.valueOf(3), VoiceVocabulary.tensionLevel("3 정도야"));
        assertEquals(Integer.valueOf(5), VoiceVocabulary.tensionLevel("많이 긴장돼"));
        assertEquals(Integer.valueOf(1), VoiceVocabulary.tensionLevel("편안해"));
    }

    @Test
    public void josa() {
        assertEquals("교차로예요", Josa.ieyo("교차로"));
        assertEquals("구간이에요", Josa.ieyo("구간"));
        assertEquals("조작 연습 모드를", Josa.eulReul("조작 연습 모드"));
    }
}
