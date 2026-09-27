package com.andredurante.fruitmachine.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.andredurante.fruitmachine.domain.Machine;
import com.andredurante.fruitmachine.service.MachineRepository;

/** Process-local storage; contents are lost when the application stops. */
@Repository
public class InMemoryMachineRepository implements MachineRepository {
    private final ConcurrentHashMap<UUID, Machine> machines = new ConcurrentHashMap<>();

    @Override
    public Optional<Machine> findById(UUID id) {
        return Optional.ofNullable(machines.get(id));
    }

    @Override
    public Machine save(Machine machine) {
        machines.put(machine.id(), machine);
        return machine;
    }

    @Override
    public boolean existsById(UUID id) {
        return machines.containsKey(id);
    }
}
