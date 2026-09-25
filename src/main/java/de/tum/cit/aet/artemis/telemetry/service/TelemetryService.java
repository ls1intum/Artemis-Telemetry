package de.tum.cit.aet.artemis.telemetry.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryRepository;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryInstanceRepository;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryIngestionRepository;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryInstanceDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TelemetryService {
    private final TelemetryRepository startups;
    private final TelemetryInstanceRepository instances;
    private final TelemetryIngestionRepository ingestion;

    public TelemetryService(TelemetryRepository startups, TelemetryInstanceRepository instances, TelemetryIngestionRepository ingestion) {
        this.startups = startups;
        this.instances = instances;
        this.ingestion = ingestion;
    }

    public Telemetry record(TelemetryDTO dto) {
        validate(dto);
        String url = ServerUrl.canonicalize(dto.serverUrl());
        for (int attempt = 0; ; attempt++) {
            try {
                return ingestion.record(dto, url);
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
                if (attempt >= 2) {
                    throw ex;
                }
            }
        }
    }

    public Telemetry get(Long id) {
        return startups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Telemetry> getAll() { return startups.findAll(); }

    public Page<TelemetryInstanceDTO> getInstances(int page, int size) {
        var result = instances.findVisible(pageRequest(page, size, "lastSeen"));
        Map<Long, Telemetry> latest = startups.findAllById(result.stream().map(i -> i.getLatestStartupId()).toList()).stream()
                .collect(Collectors.toMap(Telemetry::getId, Function.identity()));
        return result.map(i -> new TelemetryInstanceDTO(i.getId(), i.getServerUrl(), i.getFirstSeen(), i.getLastSeen(), TelemetryDTO.from(latest.get(i.getLatestStartupId()))));
    }

    public Page<TelemetryDTO> getStartups(Long instanceId, int page, int size) {
        if (!instances.existsById(instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return startups.findByInstanceId(instanceId, pageRequest(page, size, "timestamp")).map(TelemetryDTO::from);
    }

    private PageRequest pageRequest(int page, int size, String field) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, field, "id"));
    }

    private void validate(TelemetryDTO dto) {
        requireText(dto.operator(), "Operator");
        requireText(dto.version(), "Version");
        if (dto.profiles() == null || dto.profiles().isEmpty() || dto.profiles().stream().anyMatch(p -> p == null || p.isBlank() || p.contains(","))
                || String.join(",", dto.profiles()).length() > 255) {
            throw new IllegalArgumentException("Profiles must contain non-empty names and fit in 255 characters");
        }
        for (String value : new String[] {dto.adminName(), dto.universityName(), dto.contact()}) {
            if (value != null && value.length() > 255) throw new IllegalArgumentException("Metadata must not exceed 255 characters");
        }
        if (dto.numberOfNodes() != null && dto.numberOfNodes() < 1 || dto.buildAgentCount() != null && dto.buildAgentCount() < 0) {
            throw new IllegalArgumentException("Node count must be positive and build agent count non-negative");
        }
        if (dto.numberOfNodes() != null && dto.isMultiNode() != null && dto.isMultiNode() != (dto.numberOfNodes() > 1)) {
            throw new IllegalArgumentException("isMultiNode must match numberOfNodes");
        }
        if (dto.moduleFeatures() != null && (dto.moduleFeatures().size() > 100 || dto.moduleFeatures().stream().anyMatch(f -> f == null || f.isBlank() || f.length() > 100))) {
            throw new IllegalArgumentException("Module features must contain at most 100 non-empty names of at most 100 characters");
        }
        if (dto.startupId() != null && !UUID.fromString(dto.startupId()).toString().equals(dto.startupId())) {
            throw new IllegalArgumentException("startupId must be a canonical UUID");
        }
        if (dto.startedAt() != null && dto.startedAt().isAfter(java.time.ZonedDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("startedAt must not be in the future");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank() || value.length() > 255) throw new IllegalArgumentException(name + " must contain 1 to 255 characters");
    }
}
