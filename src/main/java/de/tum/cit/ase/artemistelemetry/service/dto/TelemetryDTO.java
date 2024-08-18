package de.tum.cit.ase.artemistelemetry.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.ase.artemistelemetry.domain.Telemetry;

import java.time.ZonedDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TelemetryDTO(Long id, String version, String serverUrl, String operator, String adminName, List<String> profiles, String contact, ZonedDateTime timestamp) {

    public static TelemetryDTO from(Telemetry telemetry) {
        List<String> profilesList = List.of(telemetry.getProfiles().split(","));
        return new TelemetryDTO(telemetry.getId(), telemetry.getVersion(), telemetry.getServerUrl(), telemetry.getUniversityName(), telemetry.getMainAdminName(), profilesList, telemetry.getContact(), telemetry.getTimestamp());
    }

    public static Telemetry to(TelemetryDTO telemetryDTO) {
        String profiles = String.join(",", telemetryDTO.profiles());
        Telemetry telemetry = new Telemetry();
        telemetry.setId(telemetryDTO.id());
        telemetry.setVersion(telemetryDTO.version());
        telemetry.setServerUrl(telemetryDTO.serverUrl());
        telemetry.setUniversityName(telemetryDTO.operator());
        telemetry.setMainAdminName(telemetryDTO.adminName());
        telemetry.setProfiles(profiles);
        telemetry.setTimestamp(telemetryDTO.timestamp());
        telemetry.setContact(telemetryDTO.contact());
        return telemetry;
    }
}
