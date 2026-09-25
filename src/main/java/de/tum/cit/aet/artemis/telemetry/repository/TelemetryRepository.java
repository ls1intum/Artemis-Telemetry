package de.tum.cit.aet.artemis.telemetry.repository;

import java.util.Optional;
import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    Optional<Telemetry> findByInstanceIdAndStartupId(Long instanceId, String startupId);
    Page<Telemetry> findByInstanceId(Long instanceId, Pageable pageable);
}
