#!/usr/bin/env bash
# Compiles the *headless* part of Terra Realis (kernel + preview renderer + tests) without Gradle
# and without Minecraft. Everything under dev/terrarealis/kernel and dev/terrarealis/tools is
# deliberately free of Minecraft/Fabric imports so it can be built and tested anywhere.
#
# Usage:
#   tools/build-offline.sh                 compile
#   tools/build-offline.sh test            compile + run the kernel test-suite
#   tools/build-offline.sh preview <args>  compile + render preview maps
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"
OUT="$ROOT/build/offline"

if command -v javac >/dev/null 2>&1; then
  JC="javac"; JR="java"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  JC="$JAVA_HOME/bin/javac"; JR="$JAVA_HOME/bin/java"
else
  echo "No javac found. Set JAVA_HOME to a JDK (21+)." >&2
  echo "The Gradle build (./gradlew build) is the normal path; this script is for offline checks." >&2
  exit 1
fi

rm -rf "$OUT"; mkdir -p "$OUT"
DIRS=()
for d in kernel tools; do
  [[ -d "$ROOT/src/main/java/dev/terrarealis/$d" ]] && DIRS+=("$ROOT/src/main/java/dev/terrarealis/$d")
done
find "${DIRS[@]}" -name '*.java' > "$OUT/sources.txt"
"$JC" -nowarn --release 21 -d "$OUT" @"$OUT/sources.txt"
echo "compiled $(wc -l < "$OUT/sources.txt") files -> $OUT"

case "${1:-}" in
  test)    "$JR" ${JAVA_OPTS:--Xmx3g} -cp "$OUT" dev.terrarealis.tools.KernelTests ;;
  preview) shift; "$JR" ${JAVA_OPTS:--Xmx3g} -cp "$OUT" dev.terrarealis.tools.PreviewMain "$@" ;;
  "")      ;;
  *)       "$JR" ${JAVA_OPTS:--Xmx3g} -cp "$OUT" "$@" ;;
esac
