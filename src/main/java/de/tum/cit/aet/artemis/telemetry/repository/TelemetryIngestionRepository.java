package de.tum.cit.aet.artemis.telemetry.repository;

import java.time.ZonedDateTime;
import de.tum.cit.aet.artemis.telemetry.domain.Telemetry;
import de.tum.cit.aet.artemis.telemetry.domain.TelemetryInstance;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** One transaction per attempt; the service retries conflicting first-instance inserts after rollback. */
@Repository
public class TelemetryIngestionRepository {
    private final TelemetryRepository startups;
    private final TelemetryInstanceRepository instances;

    public TelemetryIngestionRepository(TelemetryRepository startups, TelemetryInstanceRepository instances) {
        this.startups = startups;
        this.instances = instances;
    }

    @Transactional
    public Telemetry record(TelemetryDTO dto, String canonicalUrl) {
        var now = ZonedDateTime.now(java.time.ZoneOffset.UTC);
        var instance = instances.findByServerUrl(canonicalUrl).orElseGet(() -> {
            var created = new TelemetryInstance();
            created.setServerUrl(canonicalUrl);
            created.setFirstSeen(now);
            created.setLastSeen(now);
            return instances.saveAndFlush(created);
        });
        if (dto.startupId() != null) {
            var existing = startups.findByInstanceIdAndStartupId(instance.getId(), dto.startupId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        var startup = TelemetryDTO.to(dto);
        startup.setServerUrl(canonicalUrl);
        startup.setInstanceId(instance.getId());
        startup.setTimestamp(now);
        startups.saveAndFlush(startup);
        var previous = instance.getLatestStartupId() == null ? null : startups.findById(instance.getLatestStartupId()).orElse(null);
        if (previous == null || !effectiveTime(startup).isBefore(effectiveTime(previous))) {
            instance.setLatestStartupId(startup.getId());
        }
        if (instance.getFirstSeen() == null || now.isBefore(instance.getFirstSeen())) {
            instance.setFirstSeen(now);
        }
        if (instance.getLastSeen() == null || now.isAfter(instance.getLastSeen())) {
            instance.setLastSeen(now);
        }
        instances.save(instance);
        return startup;
    }

    private static java.time.Instant effectiveTime(Telemetry t) {
        var time = t.getStartedAt() == null ? t.getTimestamp() : t.getStartedAt();
        return time == null ? java.time.Instant.MIN : time.toInstant();
    }
}
