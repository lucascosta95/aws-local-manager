#!/bin/bash
# Baixa e organiza os ícones oficiais da AWS (Architecture Icons)
# Uso: ./scripts/download_aws_icons.sh
# Requer: curl, unzip

set -e

DEST="desktop/src/desktopMain/resources/icons/aws"
TMP="tmp_aws_icons"
ZIP_URL="https://d1.awsstatic.com/onedam/marketing-channels/website/aws/en_US/architecture/approved/architecture-icons/Icon-package_01302026.31b40d126ed27079b708594940ad577a86150582.zip"

echo "Baixando AWS Architecture Icons..."
mkdir -p "$TMP" "$DEST"
curl -L "$ZIP_URL" -o "$TMP/icons.zip"

echo "Extraindo..."
unzip -q "$TMP/icons.zip" -d "$TMP"

echo "Copiando PNGs 32px dos serviços (Arch_*)..."
find "$TMP" -name "Arch_*_32.png" | while read f; do
    cp "$f" "$DEST/"
done

echo "Limpando temporários..."
rm -rf "$TMP"

echo "Concluído! Ícones disponíveis em: $DEST"
