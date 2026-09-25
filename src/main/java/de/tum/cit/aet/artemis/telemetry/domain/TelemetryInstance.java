package de.tum.cit.aet.artemis.telemetry.domain;

import java.time.ZonedDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "telemetry_instance")
public class TelemetryInstance extends DomainObject {
    @Column(name = "server_url", nullable = false, unique = true)
    private String serverUrl;
    @Column(name = "first_seen")
    private ZonedDateTime firstSeen;
    @Column(name = "last_seen")
    private ZonedDateTime lastSeen;
    @Column(name = "latest_startup_id")
    private Long latestStartupId;

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String value) { serverUrl = value; }
    public ZonedDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(ZonedDateTime value) { firstSeen = value; }
    public ZonedDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(ZonedDateTime value) { lastSeen = value; }
    public Long getLatestStartupId() { return latestStartupId; }
    public void setLatestStartupId(Long value) { latestStartupId = value; }
}
