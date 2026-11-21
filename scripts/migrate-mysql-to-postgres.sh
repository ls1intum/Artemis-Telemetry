#!/bin/sh
# One-time production cutover from MySQL to PostgreSQL. Run it on the server, from /opt/artemis-telemetry/scripts,
# after the PostgreSQL release has been built and its deployment stopped at scripts/check-database-upgrade.sh.
#
#   1. (no downtime) pull the images, start PostgreSQL and let the application create the schema with Liquibase.
#   2. (downtime)    stop the application, back up MySQL, copy every row, prove the copy is identical, and reset
#                    the id counters.
#   3.               remove the MySQL container (its volume is kept as the rollback copy) and start the new application.
#
# Any failure before step 3 removes the half-built PostgreSQL volume and restarts the old application on MySQL,
# so the script can simply be run again. Nothing is deleted from MySQL.
#
# Environment: COMPOSE (default docker-compose), COMPOSE_FILE (default: the compose files next to this script),
# BACKUP_DIR (default /opt/artemis-telemetry/backups), SKIP_PULL=1 (use the local images).
set -eu

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
app_dir=$(dirname "$script_dir")
backup_dir=${BACKUP_DIR:-/opt/artemis-telemetry/backups}
compose=${COMPOSE:-docker-compose}
if [ -z "${COMPOSE_FILE:-}" ]; then
    COMPOSE_FILE="$app_dir/docker-compose.yml"
    if [ -f "$app_dir/docker-compose.prod.yml" ]; then
        COMPOSE_FILE="$COMPOSE_FILE:$app_dir/docker-compose.prod.yml"
    fi
fi
export COMPOSE_FILE

app=artemis-telemetry
schema_container=artemis-telemetry-schema
pg_volume=artemis-telemetry-postgres-data

# name:kind for every column, in the order of the PostgreSQL schema. kind: int, bool, ts (UTC), str, json.
# A column added to either schema later is caught by check_columns instead of being silently skipped.
telemetry_columns='id:int admin_name:str contact:str operator_name:str profiles:str server_url:str timestamp:ts version:str
is_production_instance:bool datasource:str number_of_nodes:int build_agent_count:int is_test_server:bool is_multi_node:bool
instance_id:int university_name:str module_features:json startup_id:str started_at:ts local_llm_deployment_enabled:bool
ci_system:str version_control_system:str distributed_data_provider:str websocket_broker:str'
instance_columns='id:int server_url:str first_seen:ts last_seen:ts latest_startup_id:int'

log() { printf '==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

mysql_exec() { docker exec -i mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 -N -B --raw "$MYSQL_DATABASE"'; }
pg_exec() { docker exec -i postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -X -q -At -v ON_ERROR_STOP=1'; }

# --- SQL generation: one definition per column kind, used for the export, the load and the verification ----------
# Transfer form on the MySQL side: text columns hex-encoded (no escaping problems for tabs, newlines, backslashes, emoji).
mysql_text() {
    case "$2" in
        int|bool) printf 'CAST(`%s` AS CHAR)' "$1" ;;
        ts)       printf "DATE_FORMAT(\`%s\`, '%%Y-%%m-%%d %%H:%%i:%%s.%%f')" "$1" ;;
        str)      printf 'HEX(`%s`)' "$1" ;;
        json)     printf 'HEX(CAST(`%s` AS CHAR))' "$1" ;;
    esac
}
# Converts a staged (transfer form) text column into the real PostgreSQL value.
pg_load() {
    case "$2" in
        int)  printf '"%s"::bigint' "$1" ;;
        bool) printf '"%s"::int::boolean' "$1" ;;
        ts)   printf '"%s"::timestamptz' "$1" ;;
        str)  printf "convert_from(decode(\"%s\", 'hex'), 'UTF8')" "$1" ;;
        json) printf "convert_from(decode(\"%s\", 'hex'), 'UTF8')::jsonb" "$1" ;;
    esac
}
# Renders a stored PostgreSQL column back into the transfer form, to compare it with MySQL's.
pg_text() {
    case "$2" in
        int)  printf '"%s"::text' "$1" ;;
        bool) printf '"%s"::int::text' "$1" ;;
        ts)   printf "to_char(\"%s\" AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI:SS.US')" "$1" ;;
        str)  printf "upper(encode(convert_to(\"%s\", 'UTF8'), 'hex'))" "$1" ;;
        json) printf "upper(encode(convert_to(\"%s\"::text, 'UTF8'), 'hex'))" "$1" ;;
    esac
}
# build FUNCTION COLUMNS: comma-separated FUNCTION(name, kind) for every column.
build() {
    out='' sep=''
    for spec in $2; do
        out="$out$sep$($1 "${spec%%:*}" "${spec#*:}")"
        sep=', '
    done
    printf '%s' "$out"
}
quoted_names() { # QUOTE COLUMNS
    out='' sep=''
    for spec in $2; do
        out="$out$sep$1${spec%%:*}$1"
        sep=', '
    done
    printf '%s' "$out"
}
stage_table() { # TABLE COLUMNS
    defs='' sep=''
    for spec in $2; do
        defs="$defs$sep\"${spec%%:*}\" text"
        sep=', '
    done
    printf 'CREATE TEMP TABLE "stg_%s" (%s) ON COMMIT DROP;\n' "$1" "$defs"
}
mysql_checksums() { # TABLE COLUMNS
    for spec in $2; do
        printf "SELECT '%s.%s', IFNULL(SHA2(GROUP_CONCAT(IFNULL(%s, 'NULL') ORDER BY id SEPARATOR ','), 256), '-') FROM \`%s\`;\n" \
            "$1" "${spec%%:*}" "$(mysql_text "${spec%%:*}" "${spec#*:}")" "$1"
    done
    printf "SELECT '%s#rows', COUNT(*) FROM \`%s\`;\n" "$1" "$1"
}
pg_checksums() { # TABLE COLUMNS
    for spec in $2; do
        printf "SELECT '%s.%s', coalesce(encode(sha256(convert_to(string_agg(coalesce(%s, 'NULL'), ',' ORDER BY id), 'UTF8')), 'hex'), '-') FROM %s;\n" \
            "$1" "${spec%%:*}" "$(pg_text "${spec%%:*}" "${spec#*:}")" "$1"
    done
    printf "SELECT '%s#rows', count(*) FROM %s;\n" "$1" "$1"
}

wait_healthy() { # CONTAINER SECONDS
    waited=0
    while :; do
        state=$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$1" 2>/dev/null) || state='missing '
        case "$state" in
            'running healthy') return 0 ;;
            'exited '*|'dead '*|'missing '*)
                docker logs --tail 60 "$1" >&2 2>&1 || true
                return 1
                ;;
        esac
        waited=$((waited + 2))
        if [ "$waited" -gt "$2" ]; then
            docker logs --tail 60 "$1" >&2 2>&1 || true
            return 1
        fi
        sleep 2
    done
}

check_columns() { # TABLE COLUMNS
    expected=$(for spec in $2; do printf '%s\n' "${spec%%:*}"; done | LC_ALL=C sort)
    mysql_actual=$(printf "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '%s';\n" "$1" | mysql_exec | LC_ALL=C sort)
    pg_actual=$(printf "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '%s';\n" "$1" | pg_exec | LC_ALL=C sort)
    [ "$mysql_actual" = "$expected" ] || die "MySQL table $1 does not have the columns this script knows. Expected: $(echo $expected) Found: $(echo $mysql_actual)"
    [ "$pg_actual" = "$expected" ] || die "PostgreSQL table $1 does not have the columns this script knows. Expected: $(echo $expected) Found: $(echo $pg_actual)"
}

# --- preconditions -----------------------------------------------------------------------------------------------
docker container inspect mysql >/dev/null 2>&1 || die 'No container named mysql: there is nothing to migrate.'
[ "$(docker inspect -f '{{.State.Running}}' mysql)" = true ] || die 'The mysql container is not running. Start it first.'
if docker container inspect postgres >/dev/null 2>&1 || docker volume inspect "$pg_volume" >/dev/null 2>&1; then
    die "A postgres container or the volume $pg_volume already exists. If it is a leftover of a failed attempt, remove it deliberately and rerun."
fi
[ -f "$app_dir/docker-compose.yml" ] || die "No docker-compose.yml in $app_dir."

umask 077
work=''
done_ok=false
app_was_running=false
cleanup() {
    status=$?
    [ -z "$work" ] || rm -rf "$work"
    docker rm -f "$schema_container" >/dev/null 2>&1 || true
    if [ "$done_ok" != true ]; then
        echo 'Migration did not complete; restoring the previous state (MySQL stays the live database).' >&2
        docker rm -f postgres >/dev/null 2>&1 || true
        docker volume rm "$pg_volume" >/dev/null 2>&1 || true
        if [ "$app_was_running" = true ]; then
            docker start "$app" >/dev/null 2>&1 || echo "WARNING: could not restart $app; start it manually." >&2
        fi
    fi
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT TERM HUP
work=$(mktemp -d)

# --- 1. preparation without downtime -------------------------------------------------------------------------------
if [ -z "${SKIP_PULL:-}" ]; then
    log 'Pulling the new application and PostgreSQL images'
    $compose pull postgres telemetry
fi

log 'Starting PostgreSQL'
$compose up -d postgres
wait_healthy postgres 180 || die 'PostgreSQL did not become healthy.'

log 'Creating the schema with Liquibase (one-off run of the new application image)'
$compose run -d --no-deps --name "$schema_container" telemetry >/dev/null
wait_healthy "$schema_container" 900 || die 'The new application did not start against the empty PostgreSQL database.'
docker rm -f "$schema_container" >/dev/null

log 'Checking that both schemas have exactly the expected columns'
check_columns telemetry "$telemetry_columns"
check_columns telemetry_instance "$instance_columns"
for table in telemetry telemetry_instance; do
    [ "$(printf 'SELECT count(*) FROM %s;\n' "$table" | pg_exec)" = 0 ] || die "PostgreSQL table $table is not empty."
done

# --- 2. downtime: stop, back up, copy, verify ----------------------------------------------------------------------
if [ "$(docker inspect -f '{{.State.Running}}' "$app" 2>/dev/null || echo false)" = true ]; then
    app_was_running=true
    log "Stopping $app (downtime starts)"
    docker stop "$app" >/dev/null
fi

log 'Writing a full MySQL backup'
mkdir -p "$backup_dir"
backup="$backup_dir/mysql-before-postgres-$(date -u +%Y%m%dT%H%M%SZ).sql"
docker exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --events --triggers --all-databases' > "$backup" \
    || { rm -f "$backup"; die 'The MySQL backup failed.'; }
if ! test -s "$backup" || ! grep -q 'Dump completed' "$backup"; then
    rm -f "$backup"
    die 'The MySQL backup is incomplete.'
fi
gzip "$backup"
log "MySQL backup written to $backup.gz"

log 'Exporting the data from MySQL'
printf 'SELECT %s FROM `telemetry` ORDER BY id;\n' "$(build mysql_text "$telemetry_columns")" | mysql_exec > "$work/telemetry.tsv"
printf 'SELECT %s FROM `telemetry_instance` ORDER BY id;\n' "$(build mysql_text "$instance_columns")" | mysql_exec > "$work/telemetry_instance.tsv"

log 'Importing the data into PostgreSQL (one transaction)'
{
    # The two tables reference each other, so row order cannot satisfy both foreign keys; the constraints are
    # checked explicitly below instead. session_replication_role needs (and the image's POSTGRES_USER has) superuser.
    printf "BEGIN;\nSET LOCAL session_replication_role = replica;\nSET LOCAL TimeZone = 'UTC';\n"
    stage_table telemetry "$telemetry_columns"
    printf "COPY \"stg_telemetry\" FROM STDIN WITH (NULL 'NULL');\n"
    cat "$work/telemetry.tsv"
    printf '\\.\n'
    stage_table telemetry_instance "$instance_columns"
    printf "COPY \"stg_telemetry_instance\" FROM STDIN WITH (NULL 'NULL');\n"
    cat "$work/telemetry_instance.tsv"
    printf '\\.\n'
    printf 'INSERT INTO telemetry_instance (%s) SELECT %s FROM "stg_telemetry_instance";\n' \
        "$(quoted_names '"' "$instance_columns")" "$(build pg_load "$instance_columns")"
    printf 'INSERT INTO telemetry (%s) SELECT %s FROM "stg_telemetry";\n' \
        "$(quoted_names '"' "$telemetry_columns")" "$(build pg_load "$telemetry_columns")"
    printf 'COMMIT;\n'
} | pg_exec

log 'Verifying row counts and the content of every column in MySQL against PostgreSQL'
{ printf 'SET SESSION group_concat_max_len = 1073741824;\n'; mysql_checksums telemetry "$telemetry_columns"; mysql_checksums telemetry_instance "$instance_columns"; } \
    | mysql_exec > "$work/mysql.checksums"
{ pg_checksums telemetry "$telemetry_columns"; pg_checksums telemetry_instance "$instance_columns"; } \
    | pg_exec | tr '|' '\t' > "$work/postgres.checksums"
if ! diff "$work/mysql.checksums" "$work/postgres.checksums" > "$work/checksums.diff"; then
    cat "$work/checksums.diff" >&2
    die 'The PostgreSQL data differs from MySQL (lines starting with < are MySQL, > are PostgreSQL).'
fi
log "Identical: $(grep -c . "$work/mysql.checksums") checks passed ($(grep '#rows' "$work/mysql.checksums" | tr '\t\n' '= ' ))"

orphans=$(printf '%s\n' \
    'SELECT (SELECT count(*) FROM telemetry t WHERE t.instance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM telemetry_instance i WHERE i.id = t.instance_id))' \
    ' + (SELECT count(*) FROM telemetry_instance i WHERE i.latest_startup_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM telemetry t WHERE t.id = i.latest_startup_id));' | pg_exec)
[ "$orphans" = 0 ] || die "$orphans rows reference a missing row."

log 'Resetting the id counters and refreshing statistics'
printf '%s\n' \
    "SELECT setval(pg_get_serial_sequence('telemetry', 'id'), COALESCE((SELECT max(id) FROM telemetry), 0) + 1, false);" \
    "SELECT setval(pg_get_serial_sequence('telemetry_instance', 'id'), COALESCE((SELECT max(id) FROM telemetry_instance), 0) + 1, false);" \
    'ANALYZE;' | pg_exec >/dev/null

# --- 3. cutover ---------------------------------------------------------------------------------------------------
# From here on the data lives in PostgreSQL. Removing the container is the marker the deployment check looks for;
# the volume artemis-telemetry-mysql-data stays untouched as the rollback copy.
log 'Removing the MySQL container (its data volume is kept)'
docker stop mysql >/dev/null
docker rm mysql >/dev/null
done_ok=true

log 'Starting the new application on PostgreSQL'
$compose up -d --no-deps --force-recreate telemetry
if wait_healthy "$app" 900; then
    # nginx resolves the upstream name once, so it keeps the old container's address until it is reloaded.
    if docker container inspect nginx >/dev/null 2>&1; then
        docker exec nginx nginx -s reload >/dev/null 2>&1 || echo 'WARNING: could not reload nginx; run: docker exec nginx nginx -s reload' >&2
    fi
    log 'Migration complete. The application is healthy on PostgreSQL.'
else
    echo "WARNING: the data is migrated, but $app is not healthy; see the log above." >&2
    exit 1
fi
echo "Rollback copies: volume artemis-telemetry-mysql-data and $backup.gz. Delete them once PostgreSQL has proven itself."
