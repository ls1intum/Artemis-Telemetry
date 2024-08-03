package de.tum.cit.ase.artemistelemetry.repository;

import de.tum.cit.ase.artemistelemetry.domain.Telemetry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryRepository extends CrudRepository<Telemetry, Long> {
    Telemetry findByUniversityName(String universityName);
}
