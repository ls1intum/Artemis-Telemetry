package de.tum.cit.aet.artemis.telemetry.config.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import de.tum.cit.aet.artemis.telemetry.service.ServerUrl;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/** Keeps the ingestion and migration definitions of URL identity identical. */
public class BackfillTelemetryInstances implements CustomTaskChange {
    @Override
    public void execute(Database database) throws CustomChangeException {
        Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        // Ascending receipt time/id makes the last linked row the deterministic latest report.
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, server_url, timestamp FROM telemetry ORDER BY timestamp, id")) {
            while (rows.next()) {
                String url;
                try {
                    url = ServerUrl.canonicalize(rows.getString("server_url"));
                } catch (IllegalArgumentException ex) {
                    // Preserve malformed legacy reports, but do not manufacture an installation identity.
                    continue;
                }
                long reportId = rows.getLong("id");
                Timestamp timestamp = rows.getTimestamp("timestamp");
                long instanceId = findOrCreate(connection, url, timestamp);
                try (var link = connection.prepareStatement("UPDATE telemetry SET instance_id = ? WHERE id = ?")) {
                    link.setLong(1, instanceId);
                    link.setLong(2, reportId);
                    link.executeUpdate();
                }
                try (var latest = connection.prepareStatement("UPDATE telemetry_instance SET latest_startup_id = ?, last_seen = ?, first_seen = COALESCE(first_seen, ?) WHERE id = ?")) {
                    latest.setLong(1, reportId);
                    latest.setTimestamp(2, timestamp);
                    latest.setTimestamp(3, timestamp);
                    latest.setLong(4, instanceId);
                    latest.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new CustomChangeException("Could not link historical telemetry to installations", ex);
        }
    }

    private long findOrCreate(Connection connection, String url, Timestamp timestamp) throws SQLException {
        try (var lookup = connection.prepareStatement("SELECT id FROM telemetry_instance WHERE server_url = ?")) {
            lookup.setString(1, url);
            try (var result = lookup.executeQuery()) {
                if (result.next()) return result.getLong(1);
            }
        }
        try (var insert = connection.prepareStatement("INSERT INTO telemetry_instance(server_url, first_seen, last_seen) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, url);
            insert.setTimestamp(2, timestamp);
            insert.setTimestamp(3, timestamp);
            insert.executeUpdate();
            try (var keys = insert.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated installation ID");
                return keys.getLong(1);
            }
        }
    }

    @Override public String getConfirmationMessage() { return "Linked valid historical telemetry URLs to installations"; }
    @Override public void setUp() { }
    @Override public void setFileOpener(ResourceAccessor accessor) { }
    @Override public ValidationErrors validate(Database database) { return new ValidationErrors(); }
}
