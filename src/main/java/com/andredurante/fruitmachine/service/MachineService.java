package com.andredurante.fruitmachine.service;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import com.andredurante.fruitmachine.domain.*;

/** Plain application service; input validation belongs to the API boundary. */
public class MachineService {
    private final MachineRepository repository;
    private final Supplier<Random> randomFactory;
    public MachineService(MachineRepository repository, Supplier<Random> randomFactory) {
        this.repository = repository;
        this.randomFactory = randomFactory;
    }
    public UUID create(MachineConfiguration config, long cost, long startingFloat) {
        UUID id = UUID.randomUUID();
        repository.save(id, new FruitMachine(randomFactory.get(), startingFloat, config, cost));
        return id;
    }
    public FruitMachine get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new MachineNotFoundException(id));
    }
    public PlayOutcome play(UUID id) {
        FruitMachine machine = get(id);
        PlayOutcome result = machine.play();
        if (result.tier() != PrizeTier.INSUFFICIENT_FLOAT) repository.save(id, machine);
        return result;
    }
}
