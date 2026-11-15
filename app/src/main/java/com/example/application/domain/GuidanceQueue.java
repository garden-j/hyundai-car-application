package com.example.application.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 주행 중 음성 안내 우선순위 대기열. 안내가 몰려도 밀리지 않도록
 * <ul>
 *   <li>우선순위가 높은 안내를 먼저 말한다(안전 > 회전 > 상황 알림 > Relax > 격려)</li>
 *   <li>안전 안내만 말하던 낮은 순위 안내를 끊고 끼어든다. 끊긴 회전 안내는 아직 유효하면 다시 말한다</li>
 *   <li>기다리는 안내는 최대 {@link #MAX_PENDING}개. 넘치면 가장 낮은 순위(같으면 오래된 것)부터 버린다</li>
 *   <li>회전 안내는 새것이 오면 기다리던 옛 회전 안내를 대체한다(지나간 회전은 의미가 없음)</li>
 *   <li>기다리다 유효 시간이 지난 안내는 버린다</li>
 * </ul>
 * Android에 의존하지 않으며, 말하기가 끝나면 호출자가 {@link #onSpeakingFinished(long)}로 알려 준다.
 */
public final class GuidanceQueue {
    public static final int MAX_PENDING = 2;

    /** 우선순위(위가 높음)와 대기 유효 시간. */
    public enum Priority {
        /** 주의 지점·도착 등 안전 안내. 말하던 낮은 순위를 끊고 끼어든다. */
        SAFETY(10_000),
        /** 다음 회전 요령. */
        TURN(15_000),
        /** 비·야간, 잔량 부족, 경유지 도착 절차. */
        ALERT(60_000),
        /** 긴장 완화 한마디. */
        RELAX(30_000),
        /** 격려. */
        ENCOURAGE(30_000);

        public final long ttlMs;

        Priority(long ttlMs) {
            this.ttlMs = ttlMs;
        }

        boolean higherThan(Priority other) {
            return ordinal() < other.ordinal();
        }
    }

    public static final class Item {
        public final String text;
        public final Priority priority;
        final long createdAt;

        Item(String text, Priority priority, long createdAt) {
            this.text = text;
            this.priority = priority;
            this.createdAt = createdAt;
        }

        boolean expired(long now) {
            return now - createdAt > priority.ttlMs;
        }
    }

    /** {@link #offer} 결과: 지금 무엇을 해야 하는지. */
    public enum Action {
        /** 말하던 것을 끊고 {@link #current()}를 바로 말한다. */
        INTERRUPT,
        /** 아무것도 말하고 있지 않으니 {@link #current()}를 바로 말한다. */
        SPEAK,
        /** 대기열에 넣었다(앞의 안내가 끝나면 말함). */
        QUEUED,
        /** 우선순위가 낮아 버렸다. */
        DROPPED
    }

    private final List<Item> pending = new ArrayList<>();
    private Item current;

    public Item current() {
        return current;
    }

    public int pendingCount() {
        return pending.size();
    }

    public Action offer(String text, Priority priority, long now) {
        if (text == null || text.isEmpty()) {
            return Action.DROPPED;
        }
        Item item = new Item(text, priority, now);
        if (current == null) {
            current = item;
            return Action.SPEAK;
        }
        if (priority == Priority.SAFETY && priority.higherThan(current.priority)) {
            // 끊긴 회전 안내는 아직 유효하면 다시 말한다
            if (current.priority == Priority.TURN && !current.expired(now)) {
                add(current);
            }
            current = item;
            return Action.INTERRUPT;
        }
        if (priority == Priority.TURN) {
            pending.removeIf(i -> i.priority == Priority.TURN);
        }
        return add(item) ? Action.QUEUED : Action.DROPPED;
    }

    /** 대기열에 넣는다. 꽉 차면 가장 낮은 순위를 버린다. 넣은 안내가 버려지면 false. */
    private boolean add(Item item) {
        pending.add(item);
        boolean kept = true;
        while (pending.size() > MAX_PENDING) {
            Item lowest = null;
            for (Item i : pending) {
                if (lowest == null || lowest.priority.higherThan(i.priority)
                        || (lowest.priority == i.priority && i.createdAt < lowest.createdAt)) {
                    lowest = i;
                }
            }
            pending.remove(lowest);
            if (lowest == item) {
                kept = false;
            }
        }
        return kept;
    }

    /** 지금 말하던 안내가 끝났다. 다음에 말할 안내(없으면 null)를 돌려준다. */
    public Item onSpeakingFinished(long now) {
        current = null;
        pending.removeIf(i -> i.expired(now));
        Item next = null;
        for (Item i : pending) {
            if (next == null || i.priority.higherThan(next.priority)
                    || (i.priority == next.priority && i.createdAt < next.createdAt)) {
                next = i;
            }
        }
        if (next != null) {
            pending.remove(next);
            current = next;
        }
        return next;
    }

    /** 주행을 마칠 때 비운다. */
    public void clear() {
        pending.clear();
        current = null;
    }
}
