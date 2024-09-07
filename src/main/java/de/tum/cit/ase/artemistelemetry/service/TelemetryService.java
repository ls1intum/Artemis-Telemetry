package de.tum.cit.ase.artemistelemetry.service;

import de.tum.cit.ase.artemistelemetry.domain.Telemetry;
import de.tum.cit.ase.artemistelemetry.repository.TelemetryRepository;
import de.tum.cit.ase.artemistelemetry.service.dto.TelemetryDTO;
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

    public Telemetry updateTelemetryByUniversityName(Telemetry newTelemetry) {
        Telemetry telemetry = telemetryRepository.findByUniversityName(newTelemetry.getUniversityName());
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
