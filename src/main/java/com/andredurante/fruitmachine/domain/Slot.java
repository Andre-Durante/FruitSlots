package com.andredurante.fruitmachine.domain;

import java.util.Objects;

public record Slot(Colour colour) {
    public Slot {
        Objects.requireNonNull(colour, "colour must not be null");
    }
}
