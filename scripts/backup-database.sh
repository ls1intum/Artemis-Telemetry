#!/bin/sh
# Run before replacing the database container: write a compressed logical backup of every
# database and keep the five newest. Stops the deployment if the backup cannot be written.
set -eu

backup_dir=${BACKUP_DIR:-/opt/artemis-telemetry/backups}

if ! docker container inspect mysql >/dev/null 2>&1; then
    echo 'No running database container; nothing to back up.'
    exit 0
fi

umask 077
mkdir -p "$backup_dir"
target="$backup_dir/mysql-$(date -u +%Y%m%dT%H%M%SZ).sql"
if ! docker exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --events --triggers --all-databases' > "$target"; then
    rm -f "$target"
    echo 'Database backup failed; deployment stopped before changing containers.' >&2
    exit 1
fi
if ! test -s "$target" || ! grep -q 'Dump completed' "$target"; then
    rm -f "$target"
    echo 'Database backup is incomplete; deployment stopped before changing containers.' >&2
    exit 1
fi
gzip "$target"
echo "Database backup written to $target.gz"
# shellcheck disable=SC2012
ls -1t "$backup_dir"/mysql-*.sql.gz | tail -n +6 | while read -r old; do rm -f "$old"; done
