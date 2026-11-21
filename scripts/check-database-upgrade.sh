#!/bin/sh
# Run before replacing the database container. Refuse every state in which recreating the containers could
# start the application on an empty or unsupported database.
set -eu

# The MySQL container is removed by scripts/migrate-mysql-to-postgres.sh as its very last step, so a MySQL
# container that still exists means the production data has not (completely) been moved to PostgreSQL.
if docker container inspect mysql >/dev/null 2>&1; then
    echo 'A MySQL container still exists: the data has not been migrated to PostgreSQL. Run scripts/migrate-mysql-to-postgres.sh first; deployment stopped.' >&2
    exit 1
fi

if docker container inspect postgres >/dev/null 2>&1; then
    if ! version=$(docker exec postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "SHOW server_version_num"'); then
        echo 'Cannot determine the running PostgreSQL version; deployment stopped before changing containers.' >&2
        exit 1
    fi
    # server_version_num is major * 10000 + minor, e.g. 190003 for 19.3. A data directory cannot be opened by another major version.
    case "$version" in
        19????) ;;
        *)
            echo "PostgreSQL server version $version does not match the pinned PostgreSQL 19 image. Dump and restore the data into a new volume to change the major version; deployment stopped." >&2
            exit 1
            ;;
    esac
elif docker volume inspect artemis-telemetry-postgres-data >/dev/null 2>&1; then
    echo 'Existing PostgreSQL data volume has no running database to verify; deployment stopped. Start its compatible PostgreSQL version first.' >&2
    exit 1
elif docker volume inspect artemis-telemetry-mysql-data >/dev/null 2>&1; then
    echo 'A MySQL data volume exists but there is no PostgreSQL database: the data has not been migrated. Run scripts/migrate-mysql-to-postgres.sh first; deployment stopped.' >&2
    exit 1
else
    echo 'No existing database volume; a fresh installation can use PostgreSQL 19.'
fi
