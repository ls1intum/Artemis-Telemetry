package de.tum.cit.aet.artemis.telemetry.web.rest;

import java.util.List;
import de.tum.cit.aet.artemis.telemetry.service.TelemetryService;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryInstanceDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardResource {
    private final TelemetryService telemetry;
    public DashboardResource(TelemetryService telemetry) { this.telemetry = telemetry; }

    @GetMapping("/api/dashboard")
    public List<TelemetryInstanceDTO> dashboard() { return telemetry.getDashboard(); }
}
