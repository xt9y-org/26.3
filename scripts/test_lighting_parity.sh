#!/usr/bin/env bash
set -euo pipefail

# Isolate both lighting runs in uniquely named worlds, rather than deleting
# run/world (which might be an actual test save). Logs survive cleanup.
mkdir -p run
control="$(mktemp -d "run/xt9y-light-control-XXXXXXXX")"
optimized="$(mktemp -d "run/xt9y-light-optimized-XXXXXXXX")"
cleanup() {
  if [[ "${XT9Y_KEEP_PARITY_WORLDS:-0}" != "1" ]]; then
    rm -rf -- "$control" "$optimized"
  fi
}
trap cleanup EXIT

XT9Y_TEST_LEVEL_NAME="$(basename "$control")" \
XT9Y_TEST_LIGHT_ORACLE=1 XT9Y_PERF_BASELINE=1 XT9Y263_PARITY=1 \
  bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log run/xt9y-light-baseline.log

XT9Y_TEST_LEVEL_NAME="$(basename "$optimized")" \
XT9Y_TEST_LIGHT_ORACLE=1 XT9Y263_PARITY=1 \
  bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log run/xt9y-light-optimized.log

python3 scripts/check_lighting.py \
  run/xt9y-light-baseline.log run/xt9y-light-optimized.log
