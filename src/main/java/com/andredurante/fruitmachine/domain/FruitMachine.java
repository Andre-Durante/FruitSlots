package com.andredurante.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class FruitMachine {
    public static final int SLOT_COUNT = 4;
    private static final Colour[] COLOURS = Colour.values();

    private final Random random;

    public FruitMachine(Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public SpinOutcome spin() {
        List<Slot> slots = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add(new Slot(COLOURS[random.nextInt(COLOURS.length)]));
        }
        return new SpinOutcome(slots);
    }
}
