#!/usr/bin/env bash
set -euo pipefail

# Actual Minecraft 26.3 client under Xvfb/Mesa. Smoke, not pixel parity.
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

for second in $(seq 1 220); do
  if [[ -f run/logs/latest.log ]] &&
     grep -q 'XT9Y_CLIENT_SMOKE: client ticks running' run/logs/latest.log; then
    echo "PASS: Minecraft 26.3 graphical client ticked under rendering mixins"
    exit 0
  fi
  if grep -Eiq '(MixinApplyError|InvalidInjectionException|InjectionError|FATAL|Caused by:)' "$output"; then
    echo "Client startup reported a rendering/mixin exception" >&2
    tail -n 120 "$output" >&2
    exit 1
  fi
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "Client exited before first tick" >&2
    tail -n 160 "$output" >&2
    [[ -f run/logs/latest.log ]] && tail -n 100 run/logs/latest.log >&2
    exit 1
  fi
  sleep 1
done

echo "Client did not reach its first tick within 220 seconds" >&2
tail -n 120 "$output" >&2
[[ -f run/logs/latest.log ]] && tail -n 90 run/logs/latest.log >&2
exit 1
