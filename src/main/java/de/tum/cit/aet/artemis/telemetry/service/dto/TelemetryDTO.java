package de.tum.cit.aet.artemis.telemetry.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;

import java.time.ZonedDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TelemetryDTO(Long id, String version, String serverUrl, String operator, String adminName, List<String> profiles, String contact, ZonedDateTime timestamp,
                           Boolean isProductionInstance, Boolean isTestServer, Boolean isMultiNode, String dataSource, Integer numberOfNodes, Integer buildAgentCount) {

    public static TelemetryDTO from(Telemetry telemetry) {
        List<String> profilesList = List.of(telemetry.getProfiles().split(","));
        return new TelemetryDTO(
                telemetry.getId(),
                telemetry.getVersion(),
                telemetry.getServerUrl(),
                telemetry.getOperatorName(),
                telemetry.getAdminName(),
                profilesList,
                telemetry.getContact(),
                telemetry.getTimestamp(),
                telemetry.isProductionInstance(),
                telemetry.isTestServer(),
                telemetry.isMultiNode(),
                telemetry.getDataSource(),
                telemetry.getNumberOfNodes(),
                telemetry.getBuildAgentCount());
    }

    public static Telemetry to(TelemetryDTO telemetryDTO) {
        String profiles = String.join(",", telemetryDTO.profiles());
        Telemetry telemetry = new Telemetry();
        telemetry.setId(telemetryDTO.id());
        telemetry.setVersion(telemetryDTO.version());
        telemetry.setServerUrl(telemetryDTO.serverUrl());
        telemetry.setOperatorName(telemetryDTO.operator());
        telemetry.setAdminName(telemetryDTO.adminName());
        telemetry.setProfiles(profiles);
        telemetry.setTimestamp(telemetryDTO.timestamp());
        telemetry.setContact(telemetryDTO.contact());
        telemetry.setProductionInstance(Boolean.TRUE.equals(telemetryDTO.isProductionInstance()));
        telemetry.setTestServer(Boolean.TRUE.equals(telemetryDTO.isMultiNode()));
        telemetry.setDataSource(telemetryDTO.dataSource());
        telemetry.setNumberOfNodes(telemetryDTO.numberOfNodes() == null ? 0 : telemetryDTO.numberOfNodes());
        telemetry.setBuildAgentCount(telemetryDTO.buildAgentCount() == null ? 0 : telemetryDTO.buildAgentCount());
        telemetry.setTestServer(Boolean.TRUE.equals(telemetryDTO.isTestServer()));
        return telemetry;
    }
}
