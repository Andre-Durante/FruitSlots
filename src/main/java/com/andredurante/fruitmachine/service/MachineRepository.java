package com.andredurante.fruitmachine.service;

import java.util.Optional;
import java.util.UUID;

import com.andredurante.fruitmachine.domain.Machine;

/** Storage boundary. IDs and machines must be non-null. */
public interface MachineRepository {
    Optional<Machine> findById(UUID id);

    /** Inserts or replaces the machine with the same ID and returns the saved value. */
    Machine save(Machine machine);

    boolean existsById(UUID id);
}
