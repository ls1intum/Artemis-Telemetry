package de.tum.cit.aet.artemis.telemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "telemetry")
public class Telemetry extends DomainObject {

    @Column(name = "server_url")
    private String serverUrl;

    @Column(name = "version")
    private String version;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "admin_name")
    private String adminName;
    
    @Column(name = "profiles")
    private String profiles;

    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    @Column(name = "contact")
    private String contact;

    @Column(name = "is_production_instance")
    private boolean isProductionInstance;

    @Column(name = "is_test_server")
    private boolean isTestServer;

    @Column(name = "is_multi_node")
    private Boolean isMultiNode;

    @Column(name = "datasource")
    private String dataSource;

    @Column(name = "number_of_nodes")
    private Integer numberOfNodes;

    @Column(name = "build_agent_count")
    private Integer buildAgentCount;

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isProductionInstance() {
        return isProductionInstance;
    }

    public void setProductionInstance(boolean productionInstance) {
        isProductionInstance = productionInstance;
    }

    public Integer getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(Integer numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Integer getBuildAgentCount() {
        return buildAgentCount;
    }

    public void setBuildAgentCount(Integer buildAgentCount) {
        this.buildAgentCount = buildAgentCount;
    }

    public boolean isTestServer() {
        return isTestServer;
    }

    public void setTestServer(boolean testServer) {
        isTestServer = testServer;
    }

    public Boolean isMultiNode() {
        return isMultiNode;
    }

    public void setMultiNode(Boolean multiNode) {
        isMultiNode = multiNode;
    }

    @Column(name = "instance_id")
    private Long instanceId;

    public Long getInstanceId() { return instanceId; }

    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    @Column(name = "university_name")
    private String universityName;

    public String getUniversityName() { return universityName; }

    public void setUniversityName(String universityName) { this.universityName = universityName; }

    @Column(name = "startup_id")
    private String startupId;

    public String getStartupId() { return startupId; }

    public void setStartupId(String startupId) { this.startupId = startupId; }

    @Column(name = "started_at")
    private ZonedDateTime startedAt;

    public ZonedDateTime getStartedAt() { return startedAt; }

    public void setStartedAt(ZonedDateTime startedAt) { this.startedAt = startedAt; }

    @Column(name = "local_llm_deployment_enabled")
    private Boolean localLLMDeploymentEnabled;

    public Boolean getLocalLLMDeploymentEnabled() { return localLLMDeploymentEnabled; }

    public void setLocalLLMDeploymentEnabled(Boolean localLLMDeploymentEnabled) { this.localLLMDeploymentEnabled = localLLMDeploymentEnabled; }

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "module_features")
    private List<String> moduleFeatures;

    public List<String> getModuleFeatures() { return moduleFeatures; }

    public void setModuleFeatures(List<String> moduleFeatures) { this.moduleFeatures = moduleFeatures; }
}
