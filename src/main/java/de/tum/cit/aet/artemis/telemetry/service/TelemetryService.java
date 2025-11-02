package de.tum.cit.aet.artemis.telemetry.service;

import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryRepository;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    public Telemetry saveNewTelemetry(Telemetry telemetry) {
        telemetry.setId(null);
        telemetry.setTimestamp(ZonedDateTime.now());
        return telemetryRepository.save(telemetry);
    }

    public Telemetry save(Telemetry telemetry) {
        return telemetryRepository.save(telemetry);
    }

    public Telemetry get(Long id) {
        return telemetryRepository.findById(id).orElseThrow();
    }

    public void delete(Long id) {
        telemetryRepository.deleteById(id);
    }

    public List<Telemetry> getAll() {
        return StreamSupport.stream(telemetryRepository.findAll().spliterator(), false)
                .toList();
    }

    public Telemetry updateTelemetryByOperatorName(Telemetry newTelemetry) {
        Telemetry telemetry = telemetryRepository.findByOperatorName(newTelemetry.getOperatorName());
        if (telemetry == null) {
            return telemetryRepository.save(newTelemetry);
        }
        newTelemetry.setId(telemetry.getId());
        return telemetryRepository.save(newTelemetry);
    }

    public void validateTelemetry(TelemetryDTO telemetry) throws IllegalArgumentException {
        if (telemetry.operator() == null || telemetry.operator().isEmpty()) {
            throw new IllegalArgumentException("University name must not be empty");
        }
        if (telemetry.profiles() == null || telemetry.profiles().isEmpty()) {
            throw new IllegalArgumentException("Profiles must not be empty");
        }
        if (telemetry.serverUrl() == null || telemetry.serverUrl().isEmpty()) {
            throw new IllegalArgumentException("Server URL must not be empty");
        }
        if (telemetry.version() == null || telemetry.version().isEmpty()) {
            throw new IllegalArgumentException("Version must not be empty");
        }
    }
}
