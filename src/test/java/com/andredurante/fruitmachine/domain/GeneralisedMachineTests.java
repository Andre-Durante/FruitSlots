package com.andredurante.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneralisedMachineTests {
    private SpinOutcome outcome(String... ids) {
        return new SpinOutcome(java.util.Arrays.stream(ids)
                .map(id -> new Slot(new Colour(id))).toList());
    }

    @Test
    void streakResetsAndDoesNotWrapOrCombineSeparateRuns() {
        assertThat(outcome("A", "A", "B", "A", "A").hasMatchingRun(3)).isFalse();
        assertThat(outcome("A", "A", "B", "B", "A").hasMatchingRun(3)).isFalse();
        assertThat(outcome("A", "B", "B", "B").hasMatchingRun(3)).isTrue();
        assertThat(outcome("A", "A", "A", "B").hasMatchingRun(3)).isTrue();
        assertThat(outcome("A", "A", "A", "A").hasMatchingRun(3)).isTrue();
    }

    @Test
    void supportsWindowOneAndWindowEqualToSlotCount() {
        assertThat(outcome("A", "B").hasMatchingRun(1)).isTrue();
        assertThat(outcome("A", "A", "A").hasMatchingRun(3)).isTrue();
        assertThat(outcome("A", "A", "B").hasMatchingRun(3)).isFalse();
    }

    @Test
    void usesColourIdsRatherThanObjectIdentity() {
        assertThat(outcome("A", "A", "A").isJackpot()).isTrue();
        assertThat(outcome("A", "B", "A").isFullHouse()).isFalse();
        assertThat(outcome("A", "B", "C").isFullHouse()).isTrue();
    }

    @Test
    void supportsHundredsOfColoursAndConfiguredSlotCount() {
        List<Colour> colours = IntStream.range(0, 500)
                .mapToObj(i -> new Colour("colour-" + i)).toList();
        Random random = mock(Random.class);
        when(random.nextInt(500)).thenReturn(499);
        FruitMachine machine = new FruitMachine(random, new MachineConfiguration(1000, colours, 20));
        SpinOutcome spin = machine.spin();
        assertThat(spin.slots()).hasSize(1000).allMatch(s -> s.colour().equals(colours.get(499)));
        verify(random, times(1000)).nextInt(500);
    }

    @Test
    void generalizedRunsKeepSingleFlatPayout() {
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(0, 0, 0, 1, 1, 1);
        FruitMachine machine = new FruitMachine(random,
                new MachineConfiguration(6, List.of(Colour.BLACK, Colour.WHITE), 3));
        PlayOutcome play = machine.play();
        assertThat(play.tier()).isEqualTo(PrizeTier.SMALL_PRIZE);
        assertThat(play.paidCents()).isEqualTo(500);
        assertThat(play.floatCents()).isEqualTo(99_400);
    }

    @Test
    void shorterRunsDoNotWinWhenWindowIsLarger() {
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(0, 0, 1, 1, 0, 0);
        PlayOutcome play = new FruitMachine(random,
                new MachineConfiguration(6, List.of(Colour.BLACK, Colour.WHITE), 3)).play();
        assertThat(play.tier()).isEqualTo(PrizeTier.NO_PRIZE);
        assertThat(play.floatCents()).isEqualTo(99_900);
    }

    @Test
    void fullHouseNeedsDistinctSlotsNotEveryColourInPalette() {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(0, 1, 2);
        PlayOutcome play = new FruitMachine(random,
                new MachineConfiguration(3, MachineConfiguration.DEFAULT.colours(), 1)).play();
        assertThat(play.tier()).isEqualTo(PrizeTier.FULL_HOUSE);
        assertThat(play.paidCents()).isEqualTo(49_950);
    }

    @Test
    void singleSlotJackpotTakesPrecedenceOverOtherTiers() {
        PlayOutcome play = new FruitMachine(new Random(1),
                new MachineConfiguration(1, List.of(new Colour("only")), 1)).play();
        assertThat(play.tier()).isEqualTo(PrizeTier.JACKPOT);
        assertThat(play.paidCents()).isEqualTo(99_900);
    }

    @Test
    void paletteIsAnImmutableSnapshot() {
        List<Colour> palette = new ArrayList<>(List.of(Colour.BLACK));
        MachineConfiguration config = new MachineConfiguration(4, palette, 2);
        palette.clear();
        assertThat(config.colours()).containsExactly(Colour.BLACK);
        assertThatThrownBy(() -> config.colours().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new MachineConfiguration(0, List.of(Colour.BLACK), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MachineConfiguration(4, List.of(), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MachineConfiguration(4, List.of(new Colour("A"), new Colour("A")), 2))
                .isInstanceOf(IllegalArgumentException.class);
        for (int k : new int[] {0, 5}) {
            assertThatThrownBy(() -> new MachineConfiguration(4, List.of(Colour.BLACK), k))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void longRowWithLargeWindowFindsRunAtEnd() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            slots.add(new Slot(i % 2 == 0 ? Colour.BLACK : Colour.WHITE));
        }
        for (int i = 0; i < 50_000; i++) {
            slots.add(new Slot(Colour.GREEN));
        }
        SpinOutcome spin = new SpinOutcome(slots);
        assertThat(spin.hasMatchingRun(50_000)).isTrue();
        assertThat(spin.hasMatchingRun(50_001)).isFalse();
    }
}
