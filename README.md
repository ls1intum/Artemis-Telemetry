# Artemis Telemetry

## Private dashboard

Open `/` on the telemetry server for the Angular **22.2.0** dashboard. It shows version,
module-feature, database, and core-node distributions, a searchable university/contact
directory, and paginated startup history. Every distribution counts each URL once using
its latest report. Test instances are excluded; missing information stays unknown.
Filters apply to the charts and directory together. “Last reported” is a startup receipt,
not an uptime signal. The initial view includes all known non-test instances; use the environment selector
to narrow the view to production.

Artemis versions are ordered numerically from newest to oldest, with releases before
their prereleases and unreported versions last. The directory additionally hides entries
containing “test” or “staging” (case-insensitive) in their URL, university, operator, name, or email,
instances whose URL contains `stud.k8s.aet.cit.tum.de`, and entries missing all three of name,
email, and university. This directory-only filter
is enabled by default and can be switched off to show every matching entry. The directory
is sorted by last reported, newest first, with up to 50 entries per page.

### Single account

The dashboard reuses the existing configured account. There is no registration, account
database, or user-management screen. Set these in the VM's `config/telemetry.env` (already
loaded by Docker Compose):

```dotenv
TELEMETRY_USER=your-username
TELEMETRY_PASSWORD=your-long-unique-password
```

Alternatively configure `telemetry.user` and `telemetry.password` in an external Spring
YAML file. Keep actual credentials out of Git. Restart the container after changing them.
Existing installations with these properties set require no additional credentials.

The browser uses an HttpOnly, SameSite=Lax session cookie with a 30-minute inactivity
timeout. Login/logout require CSRF tokens, and credentials are not saved in localStorage
or sessionStorage. The production Compose override explicitly sets `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`
so the browser only sends the session cookie over HTTPS. The nginx proxy also supplies
HTTPS forwarding headers. Local HTTP development leaves Secure disabled. Bind the backend to loopback or a trusted proxy;
do not expose a directly reachable HTTP backend in production. Signing out invalidates
the session. HTTP Basic remains supported for existing read-only API clients.

### Local development and builds

Use Java 25, Docker (for tests), and Node **24.21.0** (or an Angular-compatible version).

```sh
npm ci --prefix client
npm start --prefix client        # http://localhost:4200, proxies /api to localhost:8080
./gradlew bootRun                # provide database and account configuration via env/YAML
npm test --prefix client
npm run typecheck --prefix client
./gradlew test bootJar           # builds and packages Angular into the executable JAR
```

`docker build .` builds Angular in a Node stage, then packages it in the Java image.
There is no extra frontend container. The existing proxy and deployment workflow serve
both UI and API at the same origin. `-PskipClient` is for Docker's prebuilt client only;
normal local JAR builds compile the client automatically.

The authenticated `GET /api/dashboard` endpoint returns only the latest snapshot for
each visible instance in two bulk database queries. It never loads the full history.
Contacts are taken from the current report, so a later administrator-details opt-out
does not resurrect older contact data in the directory.

Use `docker compose up` for quick start.

## Development

### Running [Artemis](https://github.com/ls1intum/Artemis) in parallel (locally)
Adjust the port on which the telemetry service and its database are running (with the default configuration Artemis will be running on port `8080` and a PostgreSQL database on port `5432`).

Adjust the `docker-compose.yml` accordingly, e.g. the following adjustments will be needed to run the telemetry service on port `8081` and its database on port `5433`:  
```
services:
telemetry:
  ports:
    - '127.0.0.1:8081:8080'
postgres:
  ports:
    - "127.0.0.1:5433:5432"
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
field remains the operator name. Missing, `null`, or empty `profiles` and `moduleFeatures`
arrays are accepted and stored as SQL `NULL`; non-empty arrays retain their values.

Newer Artemis versions also send the systems an installation runs: `ciSystem` (`localci`,
`jenkins` or `none`), `versionControlSystem` (`localvc` or `none`), `distributedDataProvider`
(`hazelcast`, `redis` or `local`) and `websocketBroker` (`external` for a STOMP relay to a broker
such as ActiveMQ Artemis, `simple` for the in-memory broker). Older senders omit them and
the columns stay `NULL`. A value must be a lowercase label of 1 to 32 letters, digits, hyphens or
underscores, otherwise the report is rejected with 400. Unknown properties of a report are ignored,
so a newer Artemis can add fields before the collector knows them. Deploy this collector before
Artemis versions that send the new fields, so that none of them is dropped.

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

The runtime remains Java 25. The database is PostgreSQL 19 (`postgres:19`, data in the
`artemis-telemetry-postgres-data` volume). Minor releases are picked up on every deployment; a
data directory cannot be opened by another major version, so the deployment workflow checks the
running database before pulling or recreating any container and stops unless it is PostgreSQL 19,
or the volume exists without a running database. To change the major version later, dump the data
and restore it into a new volume. Fresh installations without a data volume start directly.
The database connection is configured in `docker-compose.yml` (not in `config/telemetry.env`).
The database password has no default: Compose refuses to start without `POSTGRES_PASSWORD`, which it
reads from an `.env` file next to the compose file (not in Git). Locally, `cp .env.example .env`;
on the server, `/opt/artemis-telemetry/.env` (mode 600) holds a generated value, for example from
`openssl rand -hex 24`. PostgreSQL only applies the password when it creates the volume, so changing it
later also needs `ALTER USER telemetry PASSWORD '...'`. PostgreSQL listens on `127.0.0.1:5432` of the host only.
For running the application outside Docker (`./gradlew bootRun`), `application.yml` expects the
database from `docker compose up postgres` with the user `telemetry` and the password `telemetry`
(use `POSTGRES_PASSWORD=telemetry` locally, or override `SPRING_DATASOURCE_PASSWORD`).

### Migrating a MySQL installation to PostgreSQL

Production ran on MySQL until the move to PostgreSQL. The deployment of that release is
deliberately stopped by `scripts/check-database-upgrade.sh` while a `mysql` container exists, so
merging it can never start the application on an empty database. Migrate once, on the server:

1. Merge to `main`. The workflow builds and pushes the image, copies the new compose file and the
   scripts to `/opt/artemis-telemetry`, and then fails at the preflight check. The old application
   keeps running on MySQL.
2. Create `/opt/artemis-telemetry/.env` (mode 600, owned by the deploy user) containing
   `POSTGRES_PASSWORD=<openssl rand -hex 24>`; the deployment workflow never touches it.
   Then run `sh /opt/artemis-telemetry/scripts/migrate-mysql-to-postgres.sh` on the server. It
   - pulls the images, starts PostgreSQL and lets the new application create the schema (no downtime);
   - stops the application, writes a full MySQL dump to `/opt/artemis-telemetry/backups/mysql-before-postgres-*.sql.gz`,
     copies both tables, and compares row counts and a checksum of every column between MySQL and
     PostgreSQL. Text is transferred hex-encoded, so quotes, tabs, line breaks and emoji survive unchanged;
   - resets the id counters, removes the `mysql` container, starts the application on PostgreSQL and
     reloads nginx so that it finds the new container.

   Any failure before the last step removes the half-built PostgreSQL volume and restarts the old
   application on MySQL; fix the cause and run the script again. Expect a few minutes of downtime
   (stop, dump, copy, verify, start).
3. Re-run the failed deployment workflow. The preflight now passes and the normal deployment follows.

Rollback after a successful migration: the `artemis-telemetry-mysql-data` volume and the dump are
untouched. Redeploy the previous commit with its compose file; it starts `mysql:26.10.0` on that volume.
Reports received after the cutover exist only in PostgreSQL. Delete the volume and the dump once
PostgreSQL has proven itself. Anything that connected to MySQL port 3306 directly (the old compose
file published it on all interfaces) has to be pointed at PostgreSQL.

The copy assumes the `datetime` columns hold UTC, which the application guaranteed through
`hibernate.jdbc.time_zone: UTC`. To confirm on the server beforehand, run
`docker exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" -e "SELECT MAX(timestamp), UTC_TIMESTAMP() FROM telemetry"'`:
the two values should be close together. Delete `scripts/migrate-mysql-to-postgres.sh` after the migration.

## Verification

Run `./gradlew test bootJar` with Java 25 and Docker available. Tests use a disposable
PostgreSQL 19 container, apply the original schema with populated legacy data, then run
the migration and exercise ingestion, concurrency, authentication and pagination.
Run `docker build -t artemis-telemetry:local .` to verify the production image.
