#!/bin/bash
# ============================================================
#  Mi Calendario · detener el servidor (interactivo)
#  Uso:  ./detener.sh
# ============================================================
cd "$(dirname "$0")" || exit 1

PID=""
[ -f .servidor.pid ] && PID=$(cat .servidor.pid)

if [ -z "$PID" ] || ! kill -0 "$PID" 2>/dev/null; then
    # sin PID válido: buscar el proceso directamente
    PID=$(pgrep -f "servidor.py" | head -1)
fi

if [ -z "$PID" ]; then
    echo "El servidor no está corriendo."
    rm -f .servidor.pid
    exit 0
fi

read -r -p "¿Detener el servidor de Mi Calendario (PID $PID)? [s/N]: " RESP
case "$RESP" in
    s|S|si|Sí|SÍ)
        kill "$PID" 2>/dev/null
        rm -f .servidor.pid
        sleep 0.5
        if kill -0 "$PID" 2>/dev/null; then
            kill -9 "$PID" 2>/dev/null
        fi
        echo "✔ Servidor detenido."
        ;;
    *)
        echo "Cancelado. El servidor sigue corriendo en http://localhost:8177"
        ;;
esac
