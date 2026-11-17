package edu.sjsu.cmpe172.tutoring.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// one staged write (book or cancel) per user, and counts chat turns per user.
@Component
public class PendingActionStore {

    public record Pending(String type, Long targetId, String summary, int turn) {}

    private final Map<Long, Integer> turns = new ConcurrentHashMap<>();
    private final Map<Long, Pending> pending = new ConcurrentHashMap<>();

    /** Called once for every user message. */
    public int nextTurn(Long userId) {
        return turns.merge(userId, 1, Integer::sum);
    }

    public int currentTurn(Long userId) {
        return turns.getOrDefault(userId, 0);
    }

    public void stage(Long userId, String type, Long targetId, String summary) {
        pending.put(userId, new Pending(type, targetId, summary, currentTurn(userId)));
    }

    public Pending get(Long userId) {
        return pending.get(userId);
    }

    public void clear(Long userId) {
        pending.remove(userId);
    }
}