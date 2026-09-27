package com.andredurante.fruitmachine.storage;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.andredurante.fruitmachine.domain.FruitMachine;
import java.util.Random;
import com.andredurante.fruitmachine.service.MachineRepository;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMachineRepositoryTests {
    private final MachineRepository repository = new InMemoryMachineRepository();

    @Test
    void unknownIdIsAbsent() {
        UUID id = UUID.randomUUID();
        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void savesAndReplacesByIdWithoutAffectingOtherMachines() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        FruitMachine original = new FruitMachine(new Random(1));
        FruitMachine other = new FruitMachine(new Random(2));
        assertThat(repository.save(id, original)).isSameAs(original);
        repository.save(otherId, other);
        assertThat(repository.findById(id)).containsSame(original);
        assertThat(repository.existsById(id)).isTrue();

        FruitMachine replacement = new FruitMachine(new Random(3));
        repository.save(id, replacement);
        assertThat(repository.findById(id)).containsSame(replacement);
        assertThat(repository.findById(otherId)).containsSame(other);
    }
}
