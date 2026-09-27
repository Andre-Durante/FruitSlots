package com.andredurante.fruitmachine.domain;

import java.util.Optional;

/**
 * Settlement of one play. All monetary values are cents.
 * freePlays is the credit awarded for this play's unpaid prize shortfall.
 * A rejected play has no spin; floatCents is the balance after settlement.
 */
public record PlayOutcome(
        PrizeTier tier,
        Optional<SpinOutcome> spin,
        long prizeCents,
        long paidCents,
        long freePlays,
        long floatCents) {
}
