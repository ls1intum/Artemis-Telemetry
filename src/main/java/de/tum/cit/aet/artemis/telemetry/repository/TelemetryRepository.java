package de.tum.cit.aet.artemis.telemetry.repository;

import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryRepository extends CrudRepository<Telemetry, Long> {
    Telemetry findByOperatorName(String operatorName);
}
