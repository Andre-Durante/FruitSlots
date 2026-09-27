package com.andredurante.fruitmachine.service;
import java.util.UUID;
public class MachineNotFoundException extends RuntimeException {
    public MachineNotFoundException(UUID id) { super("Machine not found: " + id); }
}
