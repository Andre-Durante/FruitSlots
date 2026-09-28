package com.andredurante.fruitmachine.domain;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreePlayTests {
    @Test
    void storesCreditsConsumesThemAtZeroFloatThenRejectsPaidPlay() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 0, 1, 2, 0, 1, 0, 1);
        FruitMachine machine = new FruitMachine(random, 349);
        assertThat(machine.play().freePlays()).isEqualTo(3);
        assertThat(machine.freePlays()).isEqualTo(3);
        for (int remaining = 2; remaining >= 0; remaining--) {
            assertThat(machine.play().tier()).isNotEqualTo(PrizeTier.INSUFFICIENT_FLOAT);
            assertThat(machine.freePlays()).isEqualTo(remaining);
            assertThat(machine.floatCents()).isZero();
        }
        assertThat(machine.play().tier()).isEqualTo(PrizeTier.INSUFFICIENT_FLOAT);
        verify(random, times(16)).nextInt(4);
    }

    @Test
    void freePlayCanEarnNewShortfallCredits() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 0, 1, 2, 0, 0, 1, 2);
        FruitMachine machine = new FruitMachine(random, 349);
        machine.play();
        assertThat(machine.play().freePlays()).isEqualTo(5);
        assertThat(machine.freePlays()).isEqualTo(7); // 3 - 1 + 5
        assertThat(machine.floatCents()).isZero();
    }

    @Test
    void generationFailureDoesNotChargePaidPlay() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 1).thenThrow(new IllegalStateException("random failed"));
        FruitMachine machine = new FruitMachine(random);
        assertThatThrownBy(machine::play).isInstanceOf(IllegalStateException.class);
        assertThat(machine.floatCents()).isEqualTo(100_000);
        assertThat(machine.freePlays()).isZero();
    }

    @Test
    void generationFailureDoesNotConsumeFreePlay() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 0, 1, 2).thenThrow(new IllegalStateException("random failed"));
        FruitMachine machine = new FruitMachine(random, 349);
        machine.play();
        assertThatThrownBy(machine::play).isInstanceOf(IllegalStateException.class);
        assertThat(machine.freePlays()).isEqualTo(3);
        assertThat(machine.floatCents()).isZero();
    }
}
