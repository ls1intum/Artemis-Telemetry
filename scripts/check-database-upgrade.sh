#!/bin/sh
# Run before replacing the database container. Refuse unsupported in-place upgrades.
set -eu

if docker container inspect mysql >/dev/null 2>&1; then
    if ! database_version=$(docker exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -Nse "SELECT VERSION()"'); then
        echo 'Cannot determine the running MySQL version; deployment stopped before changing containers.' >&2
        exit 1
    fi
    case "$database_version" in
        9.7.2|26.7.0) ;;
        *)
            echo "MySQL $database_version is not an approved source for the pinned 26.7.0 image. Back up and use the supported 9.7.2 to 26.7.0 path; deployment stopped." >&2
            exit 1
            ;;
    esac
elif docker volume inspect artemis-telemetry-mysql-data >/dev/null 2>&1; then
    echo 'Existing MySQL data volume has no running database to verify; deployment stopped. Start its compatible MySQL version first.' >&2
    exit 1
else
    echo 'No existing MySQL data volume; a fresh installation can use MySQL 26.7.'
fi
