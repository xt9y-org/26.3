#!/usr/bin/env bash
set -euo pipefail

# Graphical smoke checks one real frame and one client tick at the title screen.
# The text feature renderer only runs when world text is visible; use
# XT9Y_REQUIRE_GLYPH_SMOKE=1 only in an in-world test showing such text.
# A title-screen boot cannot legitimately require glyph-feature execution.
# This does not establish pixel parity or GPU performance gains.
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
export GALLIUM_DRIVER=llvmpipe
export SDL_VIDEODRIVER=x11
# 26.3 uses SDL3 and may prefer Vulkan. Xvfb's GLX implementation can lack
# a compatible visual; supply Mesa's Lavapipe ICD as a software Vulkan WSI
# fallback rather than incorrectly treating a driver failure as mod failure.
if [[ -f /usr/share/vulkan/icd.d/lvp_icd.x86_64.json ]]; then
  export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.x86_64.json
fi

xvfb-run -a -s "-screen 0 1280x800x24 +extension GLX +render" \
  gradle runClient --no-daemon --console=plain >"$output" 2>&1 &
pid=$!

baseline="${XT9Y_PERF_BASELINE:-0}"
require_glyph="${XT9Y_REQUIRE_GLYPH_SMOKE:-0}"
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
      if [[ "$require_glyph" != "1" || "$baseline" == "1" ]] ||
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

echo "Graphical client did not complete the required render checks (require glyph=$require_glyph)" >&2
tail -n 120 "$output" >&2
[[ -f run/logs/latest.log ]] && tail -n 100 run/logs/latest.log >&2
exit 1
