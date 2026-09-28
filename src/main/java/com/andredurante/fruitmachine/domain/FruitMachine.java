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
    private long freePlays;
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

    public long freePlays() { return freePlays; }

    public MachineConfiguration configuration() { return configuration; }

    public long startingFloatCents() { return startingFloatCents; }

    public long playCostCents() { return playCostCents; }

    public PlayOutcome play() {
        boolean useFreePlay = freePlays > 0;
        if (!useFreePlay && floatCents < playCostCents) {
            return new PlayOutcome(PrizeTier.INSUFFICIENT_FLOAT, Optional.empty(),
                    0, 0, 0, floatCents);
        }

        // Generate and classify completely before changing cash or consuming a credit.
        SpinOutcome spin = spin();
        PrizeTier tier = classify(spin);
        if (useFreePlay) {
            freePlays--;
        } else {
            floatCents -= playCostCents;
        }
        long prize = switch (tier) {
            case JACKPOT -> floatCents;
            case FULL_HOUSE -> floatCents / 2;
            case SMALL_PRIZE -> 5 * playCostCents;
            default -> 0;
        };
        return pay(tier, spin, prize);
    }

    private PrizeTier classify(SpinOutcome spin) {
        if (spin.isJackpot()) return PrizeTier.JACKPOT;
        if (spin.isFullHouse()) return PrizeTier.FULL_HOUSE;
        if (spin.hasMatchingRun(configuration.smallPrizeWindow())) return PrizeTier.SMALL_PRIZE;
        return PrizeTier.NO_PRIZE;
    }

    private PlayOutcome pay(PrizeTier tier, SpinOutcome spin, long prizeCents) {
        long paidCents = Math.min(prizeCents, floatCents);
        long shortfall = prizeCents - paidCents;
        long freePlays = shortfall == 0 ? 0 : shortfall / playCostCents
                + (shortfall % playCostCents == 0 ? 0 : 1);
        floatCents -= paidCents;
        this.freePlays += freePlays;
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
