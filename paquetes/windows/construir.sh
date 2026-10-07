#!/bin/bash
# Construye el instalador MSI de Mi Calendario (con wixl de msitools).
# Uso: paquetes/windows/construir.sh [versión]   (por defecto 2.5 → 2.5.0)
set -e
command -v wixl >/dev/null || { echo "Falta wixl: sudo apt-get install msitools wixl"; exit 1; }

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
VERSION="${1:-2.5}"
MSI_VERSION="${VERSION}.0"
DESTINO="$RAIZ/paquetes/dist"
ESCENARIO="$DESTINO/msi-escenario"

rm -rf "$ESCENARIO"
mkdir -p "$ESCENARIO" "$DESTINO"

# 1) preparar la versión en el .wxs
sed "s/Version=\"[0-9.]*\" Manufacturer/Version=\"$MSI_VERSION\" Manufacturer/" \
    "$RAIZ/paquetes/windows/micalendario.wxs" > "$ESCENARIO/micalendario.wxs"

# 2) copiar los archivos que van dentro del MSI
for f in index.html app.js styles.css servidor.py menu_arranque.py; do
    cp "$RAIZ/$f" "$ESCENARIO/$f"
done
cp "$RAIZ/paquetes/windows"/iniciar-windows.bat \
   "$RAIZ/paquetes/windows"/detener-windows.bat \
   "$RAIZ/paquetes/windows"/abrir-web.bat \
   "$RAIZ/paquetes/windows"/menu-windows.bat \
   "$RAIZ/paquetes/windows"/LEEME.txt "$ESCENARIO"/

# 3) compilar el MSI
cd "$ESCENARIO"
wixl -v -o "$DESTINO/MiCalendario-Colombia-$VERSION.msi" micalendario.wxs
echo
echo "✔ Instalador creado: $DESTINO/MiCalendario-Colombia-$VERSION.msi"
msiinfo export "$DESTINO/MiCalendario-Colombia-$VERSION.msi" Property | head -12
