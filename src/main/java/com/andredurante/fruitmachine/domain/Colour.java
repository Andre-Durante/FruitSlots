package com.andredurante.fruitmachine.domain;

/** A colour is identified by a case-sensitive ID, not its position in a palette. */
public record Colour(String id) {
    public static final Colour BLACK = new Colour("BLACK");
    public static final Colour WHITE = new Colour("WHITE");
    public static final Colour GREEN = new Colour("GREEN");
    public static final Colour YELLOW = new Colour("YELLOW");

    public Colour {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("colour ID must not be blank");
        }
    }
}
