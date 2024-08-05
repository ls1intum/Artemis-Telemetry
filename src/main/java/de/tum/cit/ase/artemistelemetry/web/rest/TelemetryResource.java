package de.tum.cit.ase.artemistelemetry.web.rest;

import de.tum.cit.ase.artemistelemetry.domain.Telemetry;
import de.tum.cit.ase.artemistelemetry.service.TelemetryService;
import de.tum.cit.ase.artemistelemetry.service.dto.TelemetryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryResource {

    private final TelemetryService telemetryService;

    public TelemetryResource(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelemetryDTO> getTelemetry(@PathVariable Long id) {
        return ResponseEntity.ok(TelemetryDTO.from(telemetryService.get(id)));
    }

    @PostMapping
    public ResponseEntity<TelemetryDTO> postTelemetry(@RequestBody TelemetryDTO telemetryDTO) {
        Telemetry savedTelemetry = telemetryService.saveNewTelemetry(TelemetryDTO.to(telemetryDTO));
        return ResponseEntity.ok(TelemetryDTO.from(savedTelemetry));
    }

    @GetMapping
    public ResponseEntity<List<TelemetryDTO>> getAllTelemetry() {
        return ResponseEntity.ok(telemetryService.getAll().stream()
                .map(TelemetryDTO::from)
                .toList());
    }
}
