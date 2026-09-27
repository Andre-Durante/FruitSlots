package com.andredurante.fruitmachine.api;
import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.andredurante.fruitmachine.domain.*;
import com.andredurante.fruitmachine.service.MachineService;
@RestController
@RequestMapping("/machines")
public class MachineController {
    private final MachineService service;
    public MachineController(MachineService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<MachineResponse> create(@Valid @RequestBody CreateMachineRequest request) {
        request.validateRelationships();
        MachineConfiguration config = new MachineConfiguration(request.slotCount(),
                request.colours().stream().map(Colour::new).toList(), request.k());
        UUID id = service.create(config, request.playCostCents(), request.startingFloatCents());
        return ResponseEntity.created(URI.create("/machines/" + id)).body(MachineResponse.from(id, service.get(id)));
    }
    @GetMapping("/{id}")
    public MachineResponse get(@PathVariable UUID id) { return MachineResponse.from(id, service.get(id)); }
    @PostMapping("/{id}/plays")
    public ResponseEntity<PlayResponse> play(@PathVariable UUID id) {
        PlayOutcome result = service.play(id);
        return ResponseEntity.status(result.tier() == PrizeTier.INSUFFICIENT_FLOAT ? 400 : 200)
                .body(PlayResponse.from(result));
    }
}
