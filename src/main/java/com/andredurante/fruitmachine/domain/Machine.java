package com.andredurante.fruitmachine.domain;

import java.util.Objects;
import java.util.UUID;

/** Machine identity only; game state and behavior will be added later. */
public record Machine(UUID id) {
    public Machine {
        Objects.requireNonNull(id, "id must not be null");
    }
}
