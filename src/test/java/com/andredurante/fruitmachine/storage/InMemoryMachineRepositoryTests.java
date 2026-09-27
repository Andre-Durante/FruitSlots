package com.andredurante.fruitmachine.storage;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.andredurante.fruitmachine.domain.Machine;
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
        Machine original = new Machine(UUID.randomUUID());
        Machine other = new Machine(UUID.randomUUID());
        assertThat(repository.save(original)).isSameAs(original);
        repository.save(other);
        assertThat(repository.findById(original.id())).containsSame(original);
        assertThat(repository.existsById(original.id())).isTrue();

        Machine replacement = new Machine(original.id());
        repository.save(replacement);
        assertThat(repository.findById(original.id())).containsSame(replacement);
        assertThat(repository.findById(other.id())).containsSame(other);
    }
}
