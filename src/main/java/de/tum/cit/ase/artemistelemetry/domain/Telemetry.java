package de.tum.cit.ase.artemistelemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

@Entity
@Table(name = "telemetry")
public class Telemetry extends DomainObject {

    @Column(name = "server_url")
    private String serverUrl;

    @Column(name = "version")
    private String version;

    @Column(name = "university_name")
    private String universityName;

    @Column(name = "admin_name")
    private String mainAdminName;
    
    @Column(name = "profiles")
    private String profiles;

    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public String getMainAdminName() {
        return mainAdminName;
    }

    public void setMainAdminName(String mainAdminName) {
        this.mainAdminName = mainAdminName;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
