package com.andredurante.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Optional;

public class FruitMachine {

    public static final long PLAY_COST_CENTS = 100L;
    public static final long STARTING_FLOAT_CENTS = 100_000L;

    private final Random random;
    private final MachineConfiguration configuration;
    private long floatCents;
    private final long playCostCents;
    private final long startingFloatCents;

    public FruitMachine(Random random) {
        this(random, STARTING_FLOAT_CENTS);
    }

    /** Allows an existing balance to be restored, including balances below the play cost. */
    public FruitMachine(Random random, long floatCents) {
        this(random, floatCents, MachineConfiguration.DEFAULT);
    }

    public FruitMachine(Random random, MachineConfiguration configuration) {
        this(random, STARTING_FLOAT_CENTS, configuration);
    }

    public FruitMachine(Random random, long floatCents, MachineConfiguration configuration) {
        this(random, floatCents, configuration, PLAY_COST_CENTS);
    }

    public FruitMachine(Random random, long floatCents, MachineConfiguration configuration, long playCostCents) {
        this.playCostCents = playCostCents;
        this.startingFloatCents = floatCents;
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        if (floatCents < 0) {
            throw new IllegalArgumentException("float must not be negative");
        }
        this.floatCents = floatCents;
    }

    public long floatCents() {
        return floatCents;
    }

    public MachineConfiguration configuration() { return configuration; }

    public long startingFloatCents() { return startingFloatCents; }

    public long playCostCents() { return playCostCents; }

    public PlayOutcome play() {
        if (floatCents < playCostCents) {
            return new PlayOutcome(PrizeTier.INSUFFICIENT_FLOAT, Optional.empty(),
                    0, 0, 0, floatCents);
        }

        floatCents -= playCostCents;
        SpinOutcome spin = spin();
        if (spin.isJackpot()) {
            return pay(PrizeTier.JACKPOT, spin, floatCents);
        }
        if (spin.isFullHouse()) {
            return pay(PrizeTier.FULL_HOUSE, spin, floatCents / 2);
        }
        if (spin.hasMatchingRun(configuration.smallPrizeWindow())) {
            return pay(PrizeTier.SMALL_PRIZE, spin, 5 * playCostCents);
        }
        return pay(PrizeTier.NO_PRIZE, spin, 0);
    }

    private PlayOutcome pay(PrizeTier tier, SpinOutcome spin, long prizeCents) {
        long paidCents = Math.min(prizeCents, floatCents);
        long shortfall = prizeCents - paidCents;
        long freePlays = shortfall == 0 ? 0 : shortfall / playCostCents
                + (shortfall % playCostCents == 0 ? 0 : 1);
        floatCents -= paidCents;
        return new PlayOutcome(tier, Optional.of(spin), prizeCents,
                paidCents, freePlays, floatCents);
    }

    /** Generates slots only; use play() to charge and settle a paid play. */
    public SpinOutcome spin() {
        List<Slot> slots = new ArrayList<>(configuration.slotCount());
        for (int i = 0; i < configuration.slotCount(); i++) {
            slots.add(new Slot(configuration.colours().get(random.nextInt(configuration.colours().size()))));
        }
        return new SpinOutcome(slots);
    }
}
