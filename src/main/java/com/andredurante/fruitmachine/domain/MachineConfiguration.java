package com.andredurante.fruitmachine.domain;

import java.util.HashSet;
import java.util.List;

/** Immutable per-machine reel configuration. M is the number of colour IDs. */
public record MachineConfiguration(int slotCount, List<Colour> colours, int smallPrizeWindow) {
    public static final MachineConfiguration DEFAULT = new MachineConfiguration(
            4, List.of(Colour.BLACK, Colour.WHITE, Colour.GREEN, Colour.YELLOW), 2);

    public MachineConfiguration {
        colours = List.copyOf(colours);
        if (slotCount < 1) {
            throw new IllegalArgumentException("slot count must be positive");
        }
        if (colours.isEmpty() || new HashSet<>(colours).size() != colours.size()) {
            throw new IllegalArgumentException("colours must be non-empty with unique IDs");
        }
        if (smallPrizeWindow < 1 || smallPrizeWindow > slotCount) {
            throw new IllegalArgumentException("small-prize window must be between 1 and slot count");
        }
    }
}
