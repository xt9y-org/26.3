#!/usr/bin/env bash
set -euo pipefail

# Non-destructive worldgen and AI path lookup A/B test.
# Both worlds are created with the same seed in independent new directories;
# never remove or overwrite the player's regular 'world' save.
mkdir -p run
control="$(mktemp -d "run/xt9y-hotpath-control-XXXXXXXX")"
optimized="$(mktemp -d "run/xt9y-hotpath-optimized-XXXXXXXX")"
keep="${XT9Y_KEEP_PARITY_WORLDS:-0}"
cleanup() {
  if [[ "$keep" != "1" ]]; then
    rm -rf -- "$control" "$optimized"
  fi
}
trap cleanup EXIT
baseline_log="run/xt9y-hotpaths-baseline.log"
optimized_log="run/xt9y-hotpaths-optimized.log"

XT9Y_TEST_LEVEL_NAME="$(basename "$control")" \
XT9Y_TEST_WORLDGEN_SNAPSHOT=1 XT9Y_TEST_PATH_ORACLE=1 \
XT9Y263_PARITY=1 bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log "$baseline_log"

XT9Y_TEST_LEVEL_NAME="$(basename "$optimized")" \
XT9Y_TEST_WORLDGEN_SNAPSHOT=1 XT9Y_TEST_PATH_ORACLE=1 \
XT9Y_NATIVE_HOTPATHS=1 XT9Y263_PARITY=1 bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log "$optimized_log"

python3 scripts/compare_engine_parity.py "$baseline_log" "$optimized_log"
echo "Logs: $baseline_log and $optimized_log"
