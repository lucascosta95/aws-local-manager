#!/bin/bash
# Regenera o ícone do macOS a partir do PNG do app.
# Uso: ./scripts/generate_icns.sh
# Requer: macOS (sips e iconutil vêm com o sistema)
#
# O jpackage só aceita .icns no macOS: apontar um .png faz o app sair com o
# ícone padrão do Java. Rode este script sempre que o icon.png mudar.

set -e

cd "$(dirname "$0")/.."

SRC="desktop/src/desktopMain/resources/icon.png"
OUT="desktop/icons/icon.icns"

if [ ! -f "$SRC" ]; then
  echo "Arquivo de origem não encontrado: $SRC" >&2
  exit 1
fi

if ! command -v iconutil >/dev/null 2>&1; then
  echo "iconutil não encontrado. Este script só roda no macOS." >&2
  exit 1
fi

TMP=$(mktemp -d)
ICONSET="$TMP/icon.iconset"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$ICONSET"

echo "Gerando variantes a partir de $SRC..."
for SIZE in 16 32 128 256 512; do
  DOUBLE=$((SIZE * 2))
  sips -z $SIZE $SIZE "$SRC" --out "$ICONSET/icon_${SIZE}x${SIZE}.png" >/dev/null
  sips -z $DOUBLE $DOUBLE "$SRC" --out "$ICONSET/icon_${SIZE}x${SIZE}@2x.png" >/dev/null
done

mkdir -p "$(dirname "$OUT")"
iconutil --convert icns "$ICONSET" --output "$OUT"

echo "Pronto: $OUT"
