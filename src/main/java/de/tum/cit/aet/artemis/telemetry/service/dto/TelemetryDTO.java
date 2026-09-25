package de.tum.cit.aet.artemis.telemetry.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import java.time.ZonedDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryDTO(Long id, String version, String serverUrl, String operator, String adminName, List<String> profiles, String contact,
        ZonedDateTime timestamp, Boolean isProductionInstance, Boolean isTestServer, Boolean isMultiNode, String dataSource,
        Integer numberOfNodes, Integer buildAgentCount, String universityName, List<String> moduleFeatures, String startupId,
        ZonedDateTime startedAt, Boolean isLocalLLMDeploymentEnabled) {

    public static TelemetryDTO from(Telemetry t) {
        return new TelemetryDTO(t.getId(), t.getVersion(), t.getServerUrl(), t.getOperatorName(), t.getAdminName(),
                t.getProfiles() == null || t.getProfiles().isEmpty() ? List.of() : List.of(t.getProfiles().split(",")), t.getContact(), t.getTimestamp(),
                t.isProductionInstance(), t.isTestServer(), t.isMultiNode(), t.getDataSource(), t.getNumberOfNodes(), t.getBuildAgentCount(),
                t.getUniversityName(), t.getModuleFeatures(), t.getStartupId(), t.getStartedAt(), t.getLocalLLMDeploymentEnabled());
    }

    public static Telemetry to(TelemetryDTO dto) {
        Telemetry t = new Telemetry();
        t.setVersion(dto.version());
        t.setServerUrl(dto.serverUrl());
        t.setOperatorName(dto.operator());
        t.setAdminName(dto.adminName());
        t.setProfiles(dto.profiles() == null || dto.profiles().isEmpty() ? null : String.join(",", dto.profiles()));
        t.setContact(dto.contact());
        t.setProductionInstance(Boolean.TRUE.equals(dto.isProductionInstance()));
        t.setTestServer(Boolean.TRUE.equals(dto.isTestServer()));
        t.setMultiNode(dto.isMultiNode());
        t.setDataSource(dto.dataSource());
        t.setNumberOfNodes(dto.numberOfNodes());
        t.setBuildAgentCount(dto.buildAgentCount());
        t.setUniversityName(dto.universityName());
        t.setModuleFeatures(dto.moduleFeatures() == null || dto.moduleFeatures().isEmpty() ? null : List.copyOf(dto.moduleFeatures()));
        t.setStartupId(dto.startupId());
        t.setStartedAt(dto.startedAt());
        t.setLocalLLMDeploymentEnabled(dto.isLocalLLMDeploymentEnabled());
        return t;
    }
}
