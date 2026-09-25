#!/bin/sh
set -eu
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
cat > "$test_dir/docker" <<'MOCK'
#!/bin/sh
case "$1 $2" in
    'container inspect') test "$SCENARIO" != fresh && test "$SCENARIO" != orphan ;;
    'volume inspect') test "$SCENARIO" = orphan ;;
    'exec mysql')
        test "$SCENARIO" != unavailable || exit 1
        printf '%s\n' "$SCENARIO"
        ;;
    *) exit 99 ;;
esac
MOCK
chmod +x "$test_dir/docker"
for scenario in 9.6.0 8.4.11 9.7.2 26.7.0 26.7.1 fresh orphan unavailable; do
    case "$scenario" in 9.7.2|26.7.0|fresh) expected=0 ;; *) expected=1 ;; esac
    actual=0
    SCENARIO="$scenario" PATH="$test_dir:$PATH" sh "$script_dir/check-database-upgrade.sh" > "$test_dir/output" 2>&1 || actual=$?
    if test "$actual" != "$expected"; then
        cat "$test_dir/output"
        echo "Unexpected result for $scenario: $actual (expected $expected)" >&2
        exit 1
    fi
done
echo 'Passed all 8 database upgrade preflight scenarios.'
