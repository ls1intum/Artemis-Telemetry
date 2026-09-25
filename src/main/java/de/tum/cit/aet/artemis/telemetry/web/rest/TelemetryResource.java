package de.tum.cit.aet.artemis.telemetry.web.rest;

import java.util.List;
import de.tum.cit.aet.artemis.telemetry.service.TelemetryService;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryInstanceDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryResource {
    private final TelemetryService telemetryService;
    public TelemetryResource(TelemetryService telemetryService) { this.telemetryService = telemetryService; }

    @GetMapping("/{id}")
    public ResponseEntity<TelemetryDTO> getTelemetry(@PathVariable Long id) {
        return ResponseEntity.ok(TelemetryDTO.from(telemetryService.get(id)));
    }

    @PostMapping
    public ResponseEntity<?> createTelemetry(@RequestBody TelemetryDTO dto) {
        if (Boolean.TRUE.equals(dto.isTestServer())) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(TelemetryDTO.from(telemetryService.record(dto)));
    }

    @GetMapping
    public List<TelemetryDTO> getAllTelemetry() { return telemetryService.getAll().stream().map(TelemetryDTO::from).toList(); }

    @GetMapping("/instances")
    public Page<TelemetryInstanceDTO> getInstances(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return telemetryService.getInstances(page, size);
    }

    @GetMapping("/instances/{id}/startups")
    public Page<TelemetryDTO> getStartups(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return telemetryService.getStartups(id, page, size);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidReport(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
