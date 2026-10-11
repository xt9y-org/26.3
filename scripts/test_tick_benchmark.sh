#!/usr/bin/env bash
set -euo pipefail

# Real (but only single-run) server MSPT A/B benchmark. Generated save
# directories are unique; this never removes the normal Minecraft world.
mkdir -p run
control="$(mktemp -d "run/xt9y-mspt-control-XXXXXXXX")"
optimized="$(mktemp -d "run/xt9y-mspt-optimized-XXXXXXXX")"
cleanup() {
  if [[ "${XT9Y_KEEP_BENCHMARK_WORLDS:-0}" != "1" ]]; then
    rm -rf -- "$control" "$optimized"
  fi
}
trap cleanup EXIT

XT9Y_TEST_LEVEL_NAME="$(basename "$control")" \
XT9Y_TPS_BENCH=1 XT9Y_BENCH_SECONDS=60 \
XT9Y_PERF_BASELINE=1 XT9Y263_PARITY=1 bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log run/xt9y-mspt-baseline.log

XT9Y_TEST_LEVEL_NAME="$(basename "$optimized")" \
XT9Y_TPS_BENCH=1 XT9Y_BENCH_SECONDS=60 \
XT9Y263_PARITY=1 bash scripts/smoke_server.sh
cp run/xt9y263-smoke.log run/xt9y-mspt-optimized.log

python3 scripts/compare_tick_bench.py \
  run/xt9y-mspt-baseline.log run/xt9y-mspt-optimized.log
