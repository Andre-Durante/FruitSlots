package com.andredurante.fruitmachine.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.andredurante.fruitmachine.domain.Machine;

@Service
public class MachineService {
    private final MachineRepository repository;

    public MachineService(MachineRepository repository) {
        this.repository = repository;
    }

    public Optional<Machine> findById(UUID id) {
        return repository.findById(id);
    }

    public Machine save(Machine machine) {
        return repository.save(machine);
    }

    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
