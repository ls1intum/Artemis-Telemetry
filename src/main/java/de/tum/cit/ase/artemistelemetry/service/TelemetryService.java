package de.tum.cit.ase.artemistelemetry.service;

import de.tum.cit.ase.artemistelemetry.domain.Telemetry;
import de.tum.cit.ase.artemistelemetry.repository.TelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
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
}
