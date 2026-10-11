#!/usr/bin/env bash
set -euo pipefail

# Dedicated-server smoke only. This is not yet a performance or parity test.
mkdir -p run
printf 'eula=true\n' > run/eula.txt
cat > run/server.properties <<'PROPERTIES'
level-seed=73452981
level-type=minecraft:normal
view-distance=5
simulation-distance=4
max-tick-time=-1
online-mode=false
spawn-protection=0
PROPERTIES

fifo="$PWD/run/.xt9y263-smoke-in"
log="$PWD/run/xt9y263-smoke.log"
rm -f "$fifo"
mkfifo "$fifo"
(gradle runServer --no-daemon --console=plain "$@" < "$fifo" > "$log" 2>&1) &
launcher=$!

cleanup() {
  exec 3>&- || true
  kill "$launcher" 2>/dev/null || true
  wait "$launcher" 2>/dev/null || true
  rm -f "$fifo"
}
trap cleanup EXIT

exec 3>"$fifo"
for second in $(seq 1 240); do
  if grep -Eq 'Done \([^)]+\)!|Done \(' "$log"; then
    echo "Server startup succeeded after $second seconds."
    if [[ "${XT9Y263_PARITY:-0}" == "1" ]]; then
      # Spawn chunks alone are not necessarily persisted in 26.3.
      # Force 169 chunks from -6..+6 in both axes, then wait for generation.
      if ! printf 'forceload add -96 -96 96 96\n' >&3; then
        echo "Server quit before forceload" >&2
        tail -n 140 "$log"
        exit 1
      fi
      sleep "${XT9Y_BENCH_SECONDS:-25}"
    fi
    if ! printf 'save-all flush\nstop\n' >&3; then
      echo "Server quit before save/stop" >&2
      tail -n 140 "$log"
      exit 1
    fi
    exec 3>&-
    for _ in $(seq 1 30); do
      if ! kill -0 "$launcher" 2>/dev/null; then
        tail -n 80 "$log"
        exit 0
      fi
      sleep 1
    done
    echo "Server started but did not stop within 30s" >&2
    tail -n 120 "$log"
    exit 1
  fi
  if ! kill -0 "$launcher" 2>/dev/null; then
    echo "Server exited before startup" >&2
    tail -n 180 "$log"
    exit 1
  fi
  sleep 1
done
echo "Server timed out before startup" >&2
tail -n 180 "$log"
exit 1
