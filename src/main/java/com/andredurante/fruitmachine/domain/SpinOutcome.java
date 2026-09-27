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

    public boolean isJackpot() {
        Colour first = slots.getFirst().colour();
        return slots.stream().allMatch(slot -> slot.colour() == first);
    }
}
