package com.andredurante.fruitmachine.domain;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FruitMachinePayoutTests {
    private Random randomFor(int a, int b, int c, int d) {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(a, b, c, d);
        return random;
    }

    @Test
    void startsWithOneThousandDollarsAndCostsOneDollar() {
        FruitMachine machine = new FruitMachine(randomFor(0, 1, 0, 1));
        assertThat(machine.floatCents()).isEqualTo(100_000L);
        PlayOutcome outcome = machine.play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.NO_PRIZE);
        assertThat(outcome.paidCents()).isZero();
        assertThat(outcome.freePlays()).isZero();
        assertThat(machine.floatCents()).isEqualTo(99_900L);
    }

    @Test
    void matchingRowEndsDoNotWin() {
        PlayOutcome outcome = new FruitMachine(randomFor(0, 1, 2, 0)).play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.NO_PRIZE);
        assertThat(outcome.prizeCents()).isZero();
        assertThat(outcome.paidCents()).isZero();
        assertThat(outcome.floatCents()).isEqualTo(99_900L);
    }

    @Test
    void jackpotNeverAlsoAwardsSmallPrizeOrShortfallCredits() {
        PlayOutcome outcome = new FruitMachine(randomFor(0, 0, 0, 0), 300).play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.JACKPOT);
        assertThat(outcome.prizeCents()).isEqualTo(200);
        assertThat(outcome.paidCents()).isEqualTo(200);
        assertThat(outcome.freePlays()).isZero();
        assertThat(outcome.floatCents()).isZero();
    }

    @Test
    void jackpotPaysEntireFloatAfterChargingCost() {
        FruitMachine machine = new FruitMachine(randomFor(1, 1, 1, 1));
        PlayOutcome outcome = machine.play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.JACKPOT);
        assertThat(outcome.prizeCents()).isEqualTo(99_900L);
        assertThat(outcome.paidCents()).isEqualTo(99_900L);
        assertThat(machine.floatCents()).isZero();
    }

    @Test
    void fullHousePaysHalfPostChargeFloatRoundedDownToWholeCents() {
        FruitMachine machine = new FruitMachine(randomFor(0, 1, 2, 3), 1001);
        PlayOutcome outcome = machine.play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.FULL_HOUSE);
        assertThat(outcome.prizeCents()).isEqualTo(450);
        assertThat(outcome.paidCents()).isEqualTo(450);
        assertThat(outcome.freePlays()).isZero();
        assertThat(machine.floatCents()).isEqualTo(451);
    }

    @Test
    void fractionalShortfallAwardsRoundedUpFreePlays() {
        FruitMachine machine = new FruitMachine(randomFor(0, 0, 1, 2), 349);
        PlayOutcome outcome = machine.play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.SMALL_PRIZE);
        assertThat(outcome.prizeCents()).isEqualTo(500);
        assertThat(outcome.paidCents()).isEqualTo(249);
        assertThat(outcome.freePlays()).isEqualTo(3); // 251 cents / 100, rounded up
        assertThat(machine.floatCents()).isZero();
    }

    @Test
    void exactShortfallDoesNotAwardAnExtraFreePlay() {
        PlayOutcome outcome = new FruitMachine(randomFor(0, 0, 1, 2), 400).play();
        assertThat(outcome.paidCents()).isEqualTo(300);
        assertThat(outcome.freePlays()).isEqualTo(2);
        assertThat(outcome.floatCents()).isZero();
    }

    @Test
    void twoSeparateRunsPayOnlyOneSmallPrize() {
        PlayOutcome outcome = new FruitMachine(randomFor(0, 0, 1, 1)).play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.SMALL_PRIZE);
        assertThat(outcome.prizeCents()).isEqualTo(500);
        assertThat(outcome.paidCents()).isEqualTo(500);
        assertThat(outcome.freePlays()).isZero();
        assertThat(outcome.floatCents()).isEqualTo(99_400L);
    }

    @Test
    void threeMatchingSlotsStillPayOneSmallPrize() {
        PlayOutcome outcome = new FruitMachine(randomFor(0, 0, 0, 1)).play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.SMALL_PRIZE);
        assertThat(outcome.paidCents()).isEqualTo(500);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 99})
    void insufficientFloatDoesNotSpinChargeOrMutate(long balance) {
        Random random = mock(Random.class);
        FruitMachine machine = new FruitMachine(random, balance);
        PlayOutcome outcome = machine.play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.INSUFFICIENT_FLOAT);
        assertThat(outcome.spin()).isEmpty();
        assertThat(outcome.prizeCents()).isZero();
        assertThat(outcome.paidCents()).isZero();
        assertThat(outcome.freePlays()).isZero();
        assertThat(outcome.floatCents()).isEqualTo(balance);
        assertThat(machine.floatCents()).isEqualTo(balance);
        verifyNoInteractions(random);
    }

    @Test
    void exactlyTheCostAllowsPlayAndCreditsEntireSmallPrize() {
        Random random = randomFor(0, 0, 1, 2);
        PlayOutcome outcome = new FruitMachine(random, 100).play();
        assertThat(outcome.tier()).isEqualTo(PrizeTier.SMALL_PRIZE);
        assertThat(outcome.spin()).isPresent();
        assertThat(outcome.paidCents()).isZero();
        assertThat(outcome.freePlays()).isEqualTo(5);
        assertThat(outcome.floatCents()).isZero();
        verify(random, times(4)).nextInt(4);
    }

    @Test
    void depletedMachineRejectsNextPlayWithoutAnotherSpin() {
        Random random = randomFor(0, 0, 0, 0);
        FruitMachine machine = new FruitMachine(random);
        machine.play();
        assertThat(machine.play().tier()).isEqualTo(PrizeTier.INSUFFICIENT_FLOAT);
        verify(random, times(4)).nextInt(4);
    }

    @Test
    void rejectsNegativeStartingFloat() {
        assertThatThrownBy(() -> new FruitMachine(new Random(1), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
