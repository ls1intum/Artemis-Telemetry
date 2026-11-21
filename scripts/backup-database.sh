#!/bin/sh
# Run before replacing the database container: write a compressed logical backup of the database
# and keep the five newest. Stops the deployment if the backup cannot be written.
set -eu

backup_dir=${BACKUP_DIR:-/opt/artemis-telemetry/backups}

if ! docker container inspect postgres >/dev/null 2>&1; then
    echo 'No running database container; nothing to back up.'
    exit 0
fi

umask 077
mkdir -p "$backup_dir"
target="$backup_dir/postgres-$(date -u +%Y%m%dT%H%M%SZ).dump"
if ! docker exec postgres sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' > "$target"; then
    rm -f "$target"
    echo 'Database backup failed; deployment stopped before changing containers.' >&2
    exit 1
fi
# A custom-format dump ends with its table of contents; listing it proves the archive is complete and readable.
if ! test -s "$target" || ! docker exec -i postgres pg_restore --list < "$target" >/dev/null; then
    rm -f "$target"
    echo 'Database backup is incomplete; deployment stopped before changing containers.' >&2
    exit 1
fi
echo "Database backup written to $target"
# shellcheck disable=SC2012
ls -1t "$backup_dir"/postgres-*.dump | tail -n +6 | while read -r old; do rm -f "$old"; done
