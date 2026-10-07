#!/bin/bash
# Construye el paquete deb de Mi Calendario.
# Uso: paquetes/deb/construir.sh [versión]   (por defecto 2.5)
set -e
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
VERSION="${1:-2.5}"
PLATILLA="$RAIZ/paquetes/deb/plantilla"
DESTINO="$RAIZ/paquetes/dist"
ARBOL="$DESTINO/arbol-deb"

rm -rf "$ARBOL"
mkdir -p "$ARBOL" "$DESTINO"

# 1) plantilla (control, scripts, servicio, accesos, menú)
cp -r "$PLATILLA"/. "$ARBOL"/

# 2) la app web tal cual está en el proyecto
for f in index.html app.js styles.css servidor.py; do
    cp "$RAIZ/$f" "$ARBOL/opt/micalendario/$f"
done

# 3) versión en el control
sed -i "s/^Version:.*/Version: $VERSION/" "$ARBOL/DEBIAN/control"

# 4) permisos
find "$ARBOL" -type d -exec chmod 755 {} \;
chmod 555 "$ARBOL/DEBIAN"/postinst "$ARBOL/DEBIAN"/prerm
chmod 755 "$ARBOL/usr/bin/micalendario-menu"
find "$ARBOL/opt/micalendario" -type f -exec chmod 444 {} \;

# 5) construir
dpkg-deb --root-owner-group --build "$ARBOL" "$DESTINO/MiCalendario-Colombia-$VERSION.deb"
echo
echo "✔ Paquete creado: $DESTINO/MiCalendario-Colombia-$VERSION.deb"
dpkg-deb -I "$DESTINO/MiCalendario-Colombia-$VERSION.deb" | head -8
