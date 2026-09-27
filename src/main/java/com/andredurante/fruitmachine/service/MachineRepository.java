package com.andredurante.fruitmachine.service;
import java.util.Optional;
import java.util.UUID;
import com.andredurante.fruitmachine.domain.FruitMachine;
public interface MachineRepository {
    Optional<FruitMachine> findById(UUID id);
    FruitMachine save(UUID id, FruitMachine machine);
    boolean existsById(UUID id);
}
