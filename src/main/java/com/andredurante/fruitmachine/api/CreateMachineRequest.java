package com.andredurante.fruitmachine.api;
import java.util.List;
import java.util.HashSet;
import jakarta.validation.constraints.*;

public record CreateMachineRequest(
        @NotNull @Positive Integer slotCount,
        @NotNull @Size(min = 2) List<@NotBlank String> colours,
        @NotNull @Positive Integer k,
        @NotNull @Positive @Max(MAX_PLAY_COST_CENTS) Long playCostCents,
        @NotNull @PositiveOrZero Long startingFloatCents) {
    // Keeps the five-times-cost small prize within the range of a long.
    public static final long MAX_PLAY_COST_CENTS = Long.MAX_VALUE / 5;

    public void validateRelationships() {
        if (k > slotCount) throw new IllegalArgumentException("k must be <= slotCount");
        if (new HashSet<>(colours).size() != colours.size()) {
            throw new IllegalArgumentException("colours must contain at least 2 distinct, unique IDs");
        }
    }
}
