#!/usr/bin/env bash
set -euo pipefail

# Graphical client smoke only. Verifies a real render frame and (unless
# baseline mode is enabled) execution of the text-rendering optimization.
# This does not establish pixel parity or performance gains.
mkdir -p run
rm -f run/logs/latest.log
output="$(mktemp -t xt9y-client-smoke-XXXXXX.log)"
pid=0
cleanup() {
  if [[ "$pid" -ne 0 ]]; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
  rm -f "$output"
}
trap cleanup EXIT

export XT9Y_CLIENT_SMOKE=1
export LIBGL_ALWAYS_SOFTWARE=1
export MESA_GL_VERSION_OVERRIDE=4.6
export GALLIUM_DRIVER=llvmpipe

xvfb-run -a -s "-screen 0 1280x800x24" \
  gradle runClient --no-daemon --console=plain >"$output" 2>&1 &
pid=$!

baseline="${XT9Y_PERF_BASELINE:-0}"
for second in $(seq 1 220); do
  log="run/logs/latest.log"
  if grep -Eiq '(MixinApplyError|InvalidInjectionException|InjectionError|FATAL|Caused by:)' "$output"; then
    echo "Graphical client reported an exception" >&2
    tail -n 120 "$output" >&2
    exit 1
  fi
  if [[ -f "$log" ]]; then
    if [[ "$baseline" == "1" ]] &&
       grep -q 'XT9Y_CLIENT_SMOKE: glyph optimization executed' "$log"; then
      echo "ERROR: glyph optimization executed despite baseline mode" >&2
      exit 1
    fi
    if grep -q 'XT9Y_CLIENT_SMOKE: client ticks running' "$log" &&
       grep -q 'XT9Y_CLIENT_SMOKE: rendered a complete client frame' "$log"; then
      if [[ "$baseline" == "1" ]] ||
         grep -q 'XT9Y_CLIENT_SMOKE: glyph optimization executed' "$log"; then
        echo "PASS: Minecraft 26.3 client rendered and ticked (baseline=$baseline)"
        exit 0
      fi
    fi
  fi
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "Graphical client exited before rendering validation" >&2
    tail -n 160 "$output" >&2
    [[ -f "$log" ]] && tail -n 100 "$log" >&2
    exit 1
  fi
  sleep 1
done

echo "Graphical client did not complete the required render/glyph checks" >&2
tail -n 120 "$output" >&2
[[ -f run/logs/latest.log ]] && tail -n 100 run/logs/latest.log >&2
exit 1
