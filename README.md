Use `docker compose up` for quick start.

## Development

### Running [Artemis](https://github.com/ls1intum/Artemis) in parallel (locally)
Adjust the port on which the telemetry service and its database are running (with the default configuration Artemis will be running on port `8080` and a mysql database on port `3306`).

Adjust the `docker-compose.yml` accordingly, e.g. the following adjustments will be needed to run the telemetry service on port `8081` and its database on port `3307`:  
```
services:
telemetry:
  ports:
    - '127.0.0.1:8081:8080'
mysql:
  ports:
    - "3307:3306"
```

For using the local telemetry service in a development setup you will need to adjust the `application-dev.yml` (within Artemis, not within the telemetry service) accordingly:
```
artemis:
  telemetry:
    enabled: true
    sendAdminDetails: true
    destination: http://localhost:8081
```

We use basic authentication for getting the data from the telemetry service. You will need to adjust the `application.yml` of the telemetry service accordingly:
```
telemetry:
    user: <user>
    password: <password>
```

## Instance overview and startup history

`POST /api/telemetry` accepts startup reports without authentication. Reads require the
configured HTTP Basic credentials (`telemetry.user` and `telemetry.password`).

- `GET /api/telemetry/instances?page=0&size=20` returns one entry per canonical URL,
  its first/last receipt times and `latestStartup` snapshot.
- `GET /api/telemetry/instances/{id}/startups?page=0&size=20` returns that installation's history.
- The original `GET /api/telemetry` and `GET /api/telemetry/{id}` still return startup reports.

The new endpoints return Spring pages (`content`, `totalElements`, `totalPages`, etc.).
Pages start at zero; the maximum page size is 100. Invalid pagination returns 400,
unknown IDs return 404. A URL's scheme and hostname are case-insensitive; default ports
and trailing slashes are normalized. Different schemes, non-default ports and path
case remain distinct installations. URLs cannot include credentials, queries or fragments.

A report can additionally contain `universityName`, `moduleFeatures` (a JSON array),
`numberOfNodes`, `buildAgentCount`, `isMultiNode`, `startupId` (a UUID), `startedAt`
(an ISO-8601 timestamp), and `isLocalLLMDeploymentEnabled`. The existing `operator`
field remains the operator name. Counts and features absent from older payloads remain
unknown; an empty feature array means no optional module features are enabled.

The collector owns the receipt `timestamp` and database IDs. Repeated delivery of the
same `startupId` for the same URL returns the existing report. Legacy reports without
this ID append a new history entry each time. The newest `startedAt` determines the
current snapshot, falling back to receipt time for old senders. Delayed older reports
remain in history. Starts more than five minutes in the future are rejected.

Reports marked `isTestServer: true` return 204 without being stored, even from older
Artemis releases. Existing test reports are retained in history; installations whose
latest historical report is a test report are excluded from the overview.

## Deployment and upgrade

Deploy this collector before upgrading Artemis senders. Artemis reports once from the
`core & scheduling` node, ten minutes after readiness, and gathers connected-node counts
at that time. Worker-only restarts do not produce a report. Test and development
servers do not report; shutdown before the delay expires cancels the report.

When telemetry is enabled, Artemis requires meaningful values for `info.operatorName`,
`info.operatorAdminName`, and `info.universityName`. Set these before upgrading Artemis.
`artemis.telemetry.sendAdminDetails=false` continues to omit administrator name and
contact from transmission; local configuration validation still applies.

Liquibase adds `telemetry_instance` and links the existing `telemetry` rows as startup
history. It preserves historical IDs, original URLs and measurements. Malformed legacy
URLs remain accessible through the historical API, without an invented installation.
No existing changeset is rewritten. Back up the database before upgrading. The schema
is additive, but an old collector does not maintain the new overview after rollback;
restore the pre-upgrade backup when rolling back the complete deployment.

The runtime remains Java 25. Spring Boot's dependency set is used with current stable
HikariCP, Liquibase and MySQL Connector/J overrides. The database image advances from
MySQL 9.6 to 26.7. Follow MySQL's supported upgrade path through the 9.7 LTS series:
upgrade the existing data directory to `mysql:9.7.2` first, then to the configured
`mysql:26.7.0`; keep a backup for rollback. The deployment workflow checks the running
database before pulling or recreating any container and stops on an unsupported version
or an existing data volume whose version cannot be verified. Only `9.7.2` and
`26.7.0` are accepted for an existing database, preventing accidental downgrades too. Complete the 9.7 upgrade
and rerun deployment. Fresh installations without a data volume can start directly. See the
[MySQL upgrade paths](https://dev.mysql.com/doc/refman/26.7/en/upgrade-paths.html).

## Verification

Run `./gradlew test bootJar` with Java 25 and Docker available. Tests use a disposable
MySQL 26.7 container, apply the original schema with populated legacy data, then run
the migration and exercise ingestion, concurrency, authentication and pagination.
Run `docker build -t artemis-telemetry:local .` to verify the production image.
