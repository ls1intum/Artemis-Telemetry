#!/bin/sh
set -eu
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
cat > "$test_dir/docker" <<'MOCK'
#!/bin/sh
mysql_container=0 postgres_container=0 mysql_volume=0 postgres_volume=0 version=
case "$SCENARIO" in
    pg-19.0)               postgres_container=1 postgres_volume=1 version=190000 ;;
    pg-19.3)               postgres_container=1 postgres_volume=1 version=190003 ;;
    pg-18.4)               postgres_container=1 postgres_volume=1 version=180004 ;;
    pg-20.0)               postgres_container=1 postgres_volume=1 version=200000 ;;
    pg-unavailable)        postgres_container=1 postgres_volume=1 ;;
    migrated-keeps-mysql-volume) postgres_container=1 postgres_volume=1 mysql_volume=1 version=190000 ;;
    legacy-mysql)          mysql_container=1 mysql_volume=1 ;;
    half-migrated)         mysql_container=1 mysql_volume=1 postgres_container=1 postgres_volume=1 version=190000 ;;
    mysql-volume-only)     mysql_volume=1 ;;
    orphan-postgres-volume) postgres_volume=1 ;;
    fresh) ;;
esac
case "$1 $2 $3" in
    'container inspect mysql') test "$mysql_container" = 1 ;;
    'container inspect postgres') test "$postgres_container" = 1 ;;
    'volume inspect artemis-telemetry-mysql-data') test "$mysql_volume" = 1 ;;
    'volume inspect artemis-telemetry-postgres-data') test "$postgres_volume" = 1 ;;
    'exec postgres sh')
        test -n "$version" || exit 1
        printf '%s\n' "$version"
        ;;
    *) exit 99 ;;
esac
MOCK
chmod +x "$test_dir/docker"
count=0
for scenario in pg-19.0 pg-19.3 pg-18.4 pg-20.0 pg-unavailable migrated-keeps-mysql-volume legacy-mysql half-migrated mysql-volume-only orphan-postgres-volume fresh; do
    case "$scenario" in pg-19.0|pg-19.3|migrated-keeps-mysql-volume|fresh) expected=0 ;; *) expected=1 ;; esac
    actual=0
    SCENARIO="$scenario" PATH="$test_dir:$PATH" sh "$script_dir/check-database-upgrade.sh" > "$test_dir/output" 2>&1 || actual=$?
    if test "$actual" != "$expected"; then
        cat "$test_dir/output"
        echo "Unexpected result for $scenario: $actual (expected $expected)" >&2
        exit 1
    fi
    count=$((count + 1))
done
echo "Passed all $count database upgrade preflight scenarios."
