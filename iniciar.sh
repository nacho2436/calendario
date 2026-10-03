#!/bin/bash
# ============================================================
#  Mi Calendario · arrancar el servidor
#  Uso:  ./iniciar.sh
# ============================================================
cd "$(dirname "$0")" || exit 1
mkdir -p datos

# ¿ya está corriendo?
if curl -s -o /dev/null --max-time 2 "http://localhost:8177/api/ping"; then
    echo "✔ El servidor ya está corriendo en http://localhost:8177"
    exit 0
fi

# liberar el puerto si quedó un servidor viejo ocupándolo
if ss -ltn 2>/dev/null | grep -q ":8177 "; then
    echo "• Puerto 8177 ocupado por otro proceso; liberando…"
    pkill -f "http.server 8177" 2>/dev/null
    pkill -f "servidor.py" 2>/dev/null
    sleep 1
fi

setsid nohup python3 servidor.py >> datos/servidor.log 2>&1 < /dev/null &
echo $! > .servidor.pid
sleep 1.5

if curl -s -o /dev/null --max-time 3 "http://localhost:8177/api/ping"; then
    IP=$(hostname -I 2>/dev/null | awk '{print $1}')
    echo "✔ Servidor iniciado (PID $(cat .servidor.pid))"
    echo "   En el PC     : http://localhost:8177"
    [ -n "$IP" ] && echo "   En el celular: http://$IP:8177   (misma red WiFi)"
    echo "   Detener con  : ./detener.sh"
else
    echo "✘ No pudo arrancar. Revisa datos/servidor.log"
    exit 1
fi
