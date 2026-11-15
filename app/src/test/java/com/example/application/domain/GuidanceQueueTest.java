package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.application.domain.GuidanceQueue.Action;
import com.example.application.domain.GuidanceQueue.Priority;

import org.junit.Test;

public class GuidanceQueueTest {

    @Test
    public void speaksImmediatelyWhenIdleOtherwiseQueues() {
        GuidanceQueue q = new GuidanceQueue();
        assertEquals(Action.SPEAK, q.offer("회전", Priority.TURN, 0));
        assertEquals(Action.QUEUED, q.offer("비", Priority.ALERT, 1));
        assertEquals("비", q.onSpeakingFinished(2).text);
        assertNull(q.onSpeakingFinished(3));
    }

    @Test
    public void higherPriorityGoesFirst() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("말하는 중", Priority.ALERT, 0);
        q.offer("릴랙스", Priority.RELAX, 1);
        q.offer("회전", Priority.TURN, 2);
        assertEquals("회전", q.onSpeakingFinished(3).text);
        assertEquals("릴랙스", q.onSpeakingFinished(4).text);
    }

    @Test
    public void safetyInterruptsAndResumesValidTurn() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("곧 좌회전", Priority.TURN, 0);
        assertEquals(Action.INTERRUPT, q.offer("어린이보호구역", Priority.SAFETY, 1));
        assertEquals("어린이보호구역", q.current().text);
        assertEquals("곧 좌회전", q.onSpeakingFinished(2).text); // 끊긴 회전 안내를 다시 말함
    }

    @Test
    public void safetyDoesNotInterruptSafety() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("과속방지턱", Priority.SAFETY, 0);
        assertEquals(Action.QUEUED, q.offer("어린이보호구역", Priority.SAFETY, 1));
    }

    @Test
    public void pendingIsLimitedAndLowestDropped() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("말하는 중", Priority.SAFETY, 0);
        assertEquals(Action.QUEUED, q.offer("격려", Priority.ENCOURAGE, 1));
        assertEquals(Action.QUEUED, q.offer("비", Priority.ALERT, 2));
        assertEquals(Action.QUEUED, q.offer("회전", Priority.TURN, 3)); // 격려가 밀려남
        assertEquals(GuidanceQueue.MAX_PENDING, q.pendingCount());
        assertEquals(Action.DROPPED, q.offer("릴랙스", Priority.RELAX, 4)); // 가장 낮아 버려짐
        assertEquals("회전", q.onSpeakingFinished(5).text);
        assertEquals("비", q.onSpeakingFinished(6).text);
        assertNull(q.onSpeakingFinished(7));
    }

    @Test
    public void newerTurnReplacesOlderTurn() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("말하는 중", Priority.ALERT, 0);
        q.offer("곧 좌회전", Priority.TURN, 1);
        q.offer("곧 합류", Priority.TURN, 2);
        assertEquals(1, q.pendingCount());
        assertEquals("곧 합류", q.onSpeakingFinished(3).text);
    }

    @Test
    public void expiredItemsAreSkipped() {
        GuidanceQueue q = new GuidanceQueue();
        q.offer("말하는 중", Priority.ALERT, 0);
        q.offer("곧 좌회전", Priority.TURN, 1_000);
        q.offer("릴랙스", Priority.RELAX, 1_000);
        // 20초 뒤: 회전(15초)은 만료, Relax(30초)는 유효
        assertEquals("릴랙스", q.onSpeakingFinished(21_000).text);
    }
}
