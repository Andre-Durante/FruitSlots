package com.andredurante.fruitmachine.domain;

import java.util.List;

/** Immutable snapshot of the four slots produced by a spin. */
public record SpinOutcome(List<Slot> slots) {
    public SpinOutcome {
        slots = List.copyOf(slots);
        if (slots.size() != FruitMachine.SLOT_COUNT) {
            throw new IllegalArgumentException("A spin outcome must contain exactly four slots");
        }
    }

    public boolean isFullHouse() {
        return slots.stream().map(Slot::colour).distinct().count() == slots.size();
    }

    public boolean hasAdjacentMatch() {
        for (int i = 1; i < slots.size(); i++) {
            if (slots.get(i - 1).colour() == slots.get(i).colour()) {
                return true;
            }
        }
        return false;
    }

    public boolean isJackpot() {
        Colour first = slots.getFirst().colour();
        return slots.stream().allMatch(slot -> slot.colour() == first);
    }
}
