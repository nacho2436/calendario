#!/bin/bash
# ============================================================
#  Mi Calendario · menú interactivo del servidor
#  Uso:  ./menu.sh   (o bash menu.sh)
# ============================================================
cd "$(dirname "$0")" || exit 1

BLANCO='\033[1;37m'; VERDE='\033[1;32m'; ROJO='\033[1;31m'
AMARILLO='\033[1;33m'; AZUL='\033[1;34m'; NORMAL='\033[0m'

puerto_ocupado()  { ss -ltn 2>/dev/null | grep -q ":8177 "; }
servidor_arriba() { curl -s -o /dev/null --max-time 2 http://localhost:8177/api/ping; }
ip_lan()          { hostname -I 2>/dev/null | awk '{print $1}'; }
leer_pid()        { [ -f .servidor.pid ] && cat .servidor.pid 2>/dev/null; }

estado() {
  if servidor_arriba; then
    echo -e "${VERDE}● Corriendo${NORMAL} (PID $(leer_pid))"
  else
    echo -e "${ROJO}○ Detenido${NORMAL}"
  fi
}

arrancar() {
  if servidor_arriba; then
    echo -e "${AMARILLO}El servidor ya está corriendo.${NORMAL}"
    return
  fi
  if puerto_ocupado; then
    echo -e "${AMARILLO}• Puerto 8177 ocupado por otro proceso; liberando…${NORMAL}"
    pkill -f "http.server 8177" 2>/dev/null
    pkill -f "python3 servidor.py" 2>/dev/null
    sleep 1
  fi
  mkdir -p datos
  setsid nohup python3 servidor.py >> datos/servidor.log 2>&1 < /dev/null &
  echo $! > .servidor.pid
  sleep 1.5
  if servidor_arriba; then
    echo -e "${VERDE}✔ Servidor iniciado${NORMAL}"
    echo -e "   En el PC     : ${AZUL}http://localhost:8177${NORMAL}"
    echo -e "   En el celular: ${AZUL}http://$(ip_lan):8177${NORMAL}  (misma red WiFi)"
  else
    echo -e "${ROJO}✘ No pudo arrancar. Revisa datos/servidor.log${NORMAL}"
    rm -f .servidor.pid
  fi
}

detener() {
  if ! servidor_arriba && ! puerto_ocupado; then
    echo -e "${AMARILLO}El servidor ya está detenido.${NORMAL}"
    return
  fi
  local pid
  pid=$(pgrep -f "python3 servidor.py" | head -1)
  [ -z "$pid" ] && pid=$(leer_pid)
  if [ -n "$pid" ]; then
    kill "$pid" 2>/dev/null
    sleep 0.6
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
  fi
  pkill -f "python3 servidor.py" 2>/dev/null
  rm -f .servidor.pid
  echo -e "${VERDE}✔ Servidor detenido.${NORMAL}"
}

reiniciar() {
  echo -e "${AZUL}↻ Reiniciando…${NORMAL}"
  detener
  sleep 0.5
  arrancar
}

info() {
  if ! servidor_arriba; then
    echo -e "${AMARILLO}El servidor está detenido; arráncalo para ver la información.${NORMAL}"
    return
  fi
  echo -e "${AZUL}── Datos guardados en el servidor ──${NORMAL}"
  curl -s --max-time 3 http://localhost:8177/api/estado | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    usuarios = d.get("users", [])
    notas = sum(len(u.get("notas", [])) for u in usuarios)
    print("  Usuarios: %d   ·   Notas totales: %d" % (len(usuarios), notas))
    for u in usuarios:
        cats = len(u.get("categorias", []))
        print("   - %s: %d notas, %d categorias" % (u.get("nombre", "?"), len(u.get("notas", [])), cats))
except Exception:
    print("  (el servidor no respondio)")
'
  [ -f datos/calendario.sqlite ] && du -h datos/calendario.sqlite | awk '{print "  Base de datos: " $1 "  →  " $2}'
}

while true; do
  clear
  echo -e "${BLANCO}══════════════════════════════════════════════${NORMAL}"
  echo -e "${BLANCO}        📅  Mi Calendario · Servidor${NORMAL}"
  echo -e "${BLANCO}══════════════════════════════════════════════${NORMAL}"
  echo -e "  Estado: $(estado)"
  echo
  echo -e "   ${AZUL}1)${NORMAL} ▶  Arrancar el servidor"
  echo -e "   ${AZUL}2)${NORMAL} ■  Detener el servidor"
  echo -e "   ${AZUL}3)${NORMAL} ↻  Reiniciar el servidor"
  echo -e "   ${AZUL}4)${NORMAL} ℹ  Ver información de los datos"
  echo -e "   ${AZUL}5)${NORMAL} ✖  Salir del menú"
  echo
  read -r -p "  Elige una opción [1-5]: " op
  case "$op" in
    1) arrancar ;;
    2) read -r -p "  ¿Detener el servidor? [s/N]: " r
       if [[ "$r" == "s" || "$r" == "S" ]]; then detener; else echo -e "${AMARILLO}Cancelado.${NORMAL}"; fi ;;
    3) reiniciar ;;
    4) info ;;
    5) echo -e "${VERDE}¡Chao! 👋${NORMAL}"; exit 0 ;;
    *) echo -e "${ROJO}Opción no válida.${NORMAL}" ;;
  esac
  echo
  read -r -p "  Pulsa ENTER para volver al menú…" _
done
