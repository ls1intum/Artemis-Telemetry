package de.tum.cit.aet.artemis.telemetry.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import de.tum.cit.aet.artemis.telemetry.domain.TelemetryInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TelemetryInstanceRepository extends JpaRepository<TelemetryInstance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TelemetryInstance> findByServerUrl(String serverUrl);

    @Query("select i from TelemetryInstance i join Telemetry t on t.id = i.latestStartupId where t.isTestServer = false")
    Page<TelemetryInstance> findVisible(Pageable pageable);
}
