package com.andredurante.fruitmachine.storage;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.andredurante.fruitmachine.domain.FruitMachine;
import com.andredurante.fruitmachine.service.MachineRepository;

/** State is process-local and is not persisted across restarts. */
public class InMemoryMachineRepository implements MachineRepository {
    private final ConcurrentHashMap<UUID, FruitMachine> machines = new ConcurrentHashMap<>();
    public Optional<FruitMachine> findById(UUID id) { return Optional.ofNullable(machines.get(id)); }
    public FruitMachine save(UUID id, FruitMachine machine) { machines.put(id, machine); return machine; }
    public boolean existsById(UUID id) { return machines.containsKey(id); }
}
