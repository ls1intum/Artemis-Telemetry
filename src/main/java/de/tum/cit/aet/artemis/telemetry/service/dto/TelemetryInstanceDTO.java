package de.tum.cit.aet.artemis.telemetry.service.dto;

import java.time.ZonedDateTime;

public record TelemetryInstanceDTO(Long id, String serverUrl, ZonedDateTime firstSeen, ZonedDateTime lastSeen, TelemetryDTO latestStartup) { }
