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
    private boolean isMultiNode;

    @Column(name = "datasource")
    private String dataSource;

    @Column(name = "number_of_nodes")
    private int numberOfNodes;

    @Column(name = "build_agent_count")
    private int buildAgentCount;

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

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public int getBuildAgentCount() {
        return buildAgentCount;
    }

    public void setBuildAgentCount(int buildAgentCount) {
        this.buildAgentCount = buildAgentCount;
    }

    public boolean isTestServer() {
        return isTestServer;
    }

    public void setTestServer(boolean testServer) {
        isTestServer = testServer;
    }

    public boolean isMultiNode() {
        return isMultiNode;
    }

    public void setMultiNode(boolean multiNode) {
        isMultiNode = multiNode;
    }
}
