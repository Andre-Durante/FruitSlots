package com.andredurante.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FruitMachineTests {
    @ParameterizedTest
    @EnumSource(Colour.class)
    void detectsJackpotForEveryColour(Colour colour) {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(colour.ordinal());

        SpinOutcome outcome = new FruitMachine(random).spin();

        assertThat(outcome.slots()).containsExactly(
                new Slot(colour), new Slot(colour), new Slot(colour), new Slot(colour));
        assertThat(outcome.isJackpot()).isTrue();
        verify(random, times(4)).nextInt(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void rejectsJackpotWhenAnySlotDiffers(int differingSlot) {
        Random random = mock(Random.class);
        Integer[] picks = {0, 0, 0, 0};
        picks[differingSlot] = 1;
        when(random.nextInt(4)).thenReturn(picks[0], picks[1], picks[2], picks[3]);

        SpinOutcome outcome = new FruitMachine(random).spin();

        assertThat(outcome.isJackpot()).isFalse();
    }

    @Test
    void returnsAllFourIndependentlySelectedColoursInOrder() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 1, 2, 3);

        SpinOutcome outcome = new FruitMachine(random).spin();

        assertThat(outcome.slots()).extracting(Slot::colour)
                .containsExactly(Colour.BLACK, Colour.WHITE, Colour.GREEN, Colour.YELLOW);
        assertThat(outcome.isJackpot()).isFalse();
    }

    @Test
    void successiveSpinsUseTheInjectedRandomAndKeepPreviousOutcome() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 0, 0, 0, 0, 1, 2, 3);
        FruitMachine machine = new FruitMachine(random);

        SpinOutcome first = machine.spin();
        SpinOutcome second = machine.spin();

        assertThat(first.isJackpot()).isTrue();
        assertThat(second.isJackpot()).isFalse();
        verify(random, times(8)).nextInt(4);
    }

    @Test
    void outcomeDefensivelyCopiesItsSlots() {
        List<Slot> slots = new ArrayList<>(List.of(
                new Slot(Colour.BLACK), new Slot(Colour.BLACK),
                new Slot(Colour.BLACK), new Slot(Colour.BLACK)));
        SpinOutcome outcome = new SpinOutcome(slots);

        slots.set(0, new Slot(Colour.WHITE));

        assertThat(outcome.isJackpot()).isTrue();
        assertThatThrownBy(() -> outcome.slots().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 5})
    void rejectsOutcomesWithoutExactlyFourSlots(int count) {
        List<Slot> slots = java.util.Collections.nCopies(count, new Slot(Colour.BLACK));
        assertThatThrownBy(() -> new SpinOutcome(slots))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
