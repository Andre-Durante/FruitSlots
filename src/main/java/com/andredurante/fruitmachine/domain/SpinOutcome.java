package com.andredurante.fruitmachine.domain;

import java.util.List;

/** Immutable snapshot of the slots produced by a spin. */
public record SpinOutcome(List<Slot> slots) {
    public SpinOutcome {
        slots = List.copyOf(slots);
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("A spin outcome must contain at least one slot");
        }
    }

    public boolean isFullHouse() {
        return slots.stream().map(Slot::colour).distinct().count() == slots.size();
    }

    /** One pass, O(n) time and O(1) extra space; row ends are never joined. */
    public boolean hasMatchingRun(int k) {
        if (k < 1 || k > slots.size()) {
            throw new IllegalArgumentException("window must be between 1 and slot count");
        }
        int streak = 1;
        if (streak >= k) {
            return true;
        }
        for (int i = 1; i < slots.size(); i++) {
            streak = slots.get(i - 1).colour().equals(slots.get(i).colour())
                    ? streak + 1 : 1;
            if (streak >= k) {
                return true;
            }
        }
        return false;
    }

    public boolean isJackpot() {
        Colour first = slots.getFirst().colour();
        return slots.stream().allMatch(slot -> slot.colour().equals(first));
    }
}
