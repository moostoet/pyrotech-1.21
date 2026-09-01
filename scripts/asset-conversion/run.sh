#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

SRC_TMP=$(mktemp -d)
trap 'rm -rf "$SRC_TMP"' EXIT
git archive 1.12 src/main/resources/assets/pyrotech | tar -x -C "$SRC_TMP" --strip-components=4

OUT=src/main/resources/assets/pyrotech
rm -rf "$OUT/blockstates" "$OUT/lang" "$OUT/models" "$OUT/sounds" "$OUT/sounds.json" "$OUT/textures"

VANILLA_JAR=$(find ~/.gradle/caches/neoformruntime/artifacts -name 'minecraft_1.21.1_client.jar' 2>/dev/null | head -1 || true)

python3 scripts/asset-conversion/convert.py \
  --src "$SRC_TMP/pyrotech" \
  --out "$OUT" \
  ${VANILLA_JAR:+--vanilla-jar "$VANILLA_JAR"} \
  --report docs/asset-conversion-report.md
