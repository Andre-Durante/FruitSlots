package com.andredurante.fruitmachine.api;
import java.util.List;
import java.util.UUID;
import com.andredurante.fruitmachine.domain.*;
public record MachineResponse(UUID id, long floatCents, long freePlays, Configuration config) {
    public record Configuration(int slotCount, List<String> colours, int k, long playCostCents, long startingFloatCents) {}
    public static MachineResponse from(UUID id, FruitMachine machine) {
        MachineConfiguration c = machine.configuration();
        return new MachineResponse(id, machine.floatCents(), machine.freePlays(), new Configuration(c.slotCount(),
                c.colours().stream().map(Colour::id).toList(), c.smallPrizeWindow(), machine.playCostCents(), machine.startingFloatCents()));
    }
}
