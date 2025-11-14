package de.tum.cit.aet.artemis.telemetry.web.rest;

import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import de.tum.cit.aet.artemis.telemetry.service.TelemetryService;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryResource {

    private final Logger log = LoggerFactory.getLogger(TelemetryResource.class);

    private final TelemetryService telemetryService;

    public TelemetryResource(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelemetryDTO> getTelemetry(@PathVariable Long id) {
        return ResponseEntity.ok(TelemetryDTO.from(telemetryService.get(id)));
    }

    @PostMapping
    public ResponseEntity<?> createTelemetry(@RequestBody TelemetryDTO telemetryDTO) {
        log.info("Received telemetry data: {}", telemetryDTO);
        try {
            telemetryService.validateTelemetry(telemetryDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
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
