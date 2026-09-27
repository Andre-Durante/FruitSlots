package com.andredurante.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Optional;

public class FruitMachine {
    public static final int SLOT_COUNT = 4;
    private static final Colour[] COLOURS = Colour.values();

    public static final long PLAY_COST_CENTS = 100L;
    public static final long STARTING_FLOAT_CENTS = 100_000L;

    private final Random random;
    private long floatCents;

    public FruitMachine(Random random) {
        this(random, STARTING_FLOAT_CENTS);
    }

    /** Allows an existing balance to be restored, including balances below the play cost. */
    public FruitMachine(Random random, long floatCents) {
        this.random = Objects.requireNonNull(random, "random must not be null");
        if (floatCents < 0) {
            throw new IllegalArgumentException("float must not be negative");
        }
        this.floatCents = floatCents;
    }

    public long floatCents() {
        return floatCents;
    }

    public PlayOutcome play() {
        if (floatCents < PLAY_COST_CENTS) {
            return new PlayOutcome(PrizeTier.INSUFFICIENT_FLOAT, Optional.empty(),
                    0, 0, 0, floatCents);
        }

        floatCents -= PLAY_COST_CENTS;
        SpinOutcome spin = spin();
        if (spin.isJackpot()) {
            return pay(PrizeTier.JACKPOT, spin, floatCents);
        }
        if (spin.isFullHouse()) {
            return pay(PrizeTier.FULL_HOUSE, spin, floatCents / 2);
        }
        if (spin.hasAdjacentMatch()) {
            return pay(PrizeTier.SMALL_PRIZE, spin, 5 * PLAY_COST_CENTS);
        }
        return pay(PrizeTier.NO_PRIZE, spin, 0);
    }

    private PlayOutcome pay(PrizeTier tier, SpinOutcome spin, long prizeCents) {
        long paidCents = Math.min(prizeCents, floatCents);
        long shortfall = prizeCents - paidCents;
        long freePlays = shortfall / PLAY_COST_CENTS
                + (shortfall % PLAY_COST_CENTS == 0 ? 0 : 1);
        floatCents -= paidCents;
        return new PlayOutcome(tier, Optional.of(spin), prizeCents,
                paidCents, freePlays, floatCents);
    }

    /** Generates slots only; use play() to charge and settle a paid play. */
    public SpinOutcome spin() {
        List<Slot> slots = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add(new Slot(COLOURS[random.nextInt(COLOURS.length)]));
        }
        return new SpinOutcome(slots);
    }
}
