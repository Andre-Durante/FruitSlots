package com.andredurante.fruitmachine.api;
import java.util.List;
import com.andredurante.fruitmachine.domain.*;
public record PlayResponse(PrizeTier outcome, List<String> slots, long prizeCents,
                           long paidCents, long freePlaysCredited, long floatCents) {
    public static PlayResponse from(PlayOutcome result) {
        return new PlayResponse(result.tier(), result.spin()
                .map(s -> s.slots().stream().map(slot -> slot.colour().id()).toList()).orElse(List.of()),
                result.prizeCents(), result.paidCents(), result.freePlays(), result.floatCents());
    }
}
