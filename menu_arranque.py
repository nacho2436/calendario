#!/usr/bin/env python3
# ============================================================
#  Mi Calendario · menú desplegable del servicio (Linux y Windows)
#
#  Panel que aparece al arrancar la app desde el menú de
#  aplicaciones / menú Inicio: muestra el estado del servidor y un
#  menú desplegable con las acciones (encender, apagar, abrir...).
#
#  Solo usa biblioteca estándar de Python (tkinter incluida).
# ============================================================
import os
import subprocess
import sys
import threading
import time
import webbrowser
import tkinter as tk
from tkinter import ttk
import urllib.request

BASE = os.path.dirname(os.path.abspath(__file__))
PUERTO = 8177
URL = f'http://127.0.0.1:{PUERTO}'
ES_WINDOWS = os.name == 'nt'

# Los datos en el mismo sitio que usan el servicio del deb (systemd)
# y el instalador de Windows (iniciar-windows.bat)
if ES_WINDOWS:
    DATOS = os.path.join(os.environ.get('LOCALAPPDATA') or os.path.expanduser('~'),
                         'MiCalendario')
else:
    DATOS = os.path.expanduser('~/.local/share/micalendario')


def entorno():
    e = dict(os.environ)
    e['MCALENDARIO_DATOS'] = DATOS
    return e


def encendido():
    """¿Responde el servidor? (da igual quién lo haya arrancado)"""
    try:
        with urllib.request.urlopen(URL + '/api/ping', timeout=1) as r:
            return r.status == 200
    except Exception:
        return False


def _flags_ocultas():
    # en Windows no abrir consolas negras para los comandos
    return subprocess.CREATE_NO_WINDOW if ES_WINDOWS else 0


def _con_systemd():
    if ES_WINDOWS:
        return False
    try:
        return subprocess.run(['systemctl', '--user', 'cat', 'micalendario.service'],
                              capture_output=True).returncode == 0
    except OSError:
        return False


CON_SYSTEMD = _con_systemd()


def arrancar_servidor():
    if CON_SYSTEMD:
        subprocess.run(['systemctl', '--user', 'start', 'micalendario.service'],
                       capture_output=True)
    else:
        subprocess.Popen([sys.executable, os.path.join(BASE, 'servidor.py')],
                         cwd=BASE, env=entorno(),
                         stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                         stderr=subprocess.DEVNULL,
                         creationflags=_flags_ocultas(),
                         start_new_session=not ES_WINDOWS)


def apagar_servidor():
    if CON_SYSTEMD:
        subprocess.run(['systemctl', '--user', 'stop', 'micalendario.service'],
                       capture_output=True)
    else:
        subprocess.run([sys.executable, os.path.join(BASE, 'servidor.py'), 'stop'],
                       cwd=BASE, env=entorno(), capture_output=True,
                       creationflags=_flags_ocultas())


def ip_lan():
    """IP de este equipo en la red local (para abrir desde el celular)."""
    import socket
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))     # no envía nada, solo elige la interfaz
        ip = s.getsockname()[0]
        s.close()
        return ip
    except OSError:
        return '127.0.0.1'


def _esperar_encendido(segundos=6):
    for _ in range(segundos * 4):
        if encendido():
            return True
        time.sleep(0.25)
    return False


def _esperar_apagado(segundos=5):
    for _ in range(segundos * 4):
        if not encendido():
            return True
        time.sleep(0.25)
    return False


ACCIONES = [
    '▶  Encender y abrir la app',
    '▶  Encender servidor',
    '↻  Reiniciar servidor',
    '■  Apagar servidor',
    '🌐  Abrir la app en el navegador',
    '📱  Dirección para el celular',
]


def ejecutar(accion):
    """Ejecuta la acción elegida (llamar en un hilo) y devuelve el mensaje."""
    try:
        if accion.startswith('▶  Encender y abrir'):
            if not encendido():
                arrancar_servidor()
                _esperar_encendido()
            webbrowser.open(URL)
            return 'Abierta en el navegador 🌐'
        if accion.startswith('▶  Encender servidor'):
            if encendido():
                return 'Ya estaba encendido ●'
            arrancar_servidor()
            return 'Servidor encendido ●' if _esperar_encendido() else 'No pudo arrancar ✖'
        if accion.startswith('↻'):
            apagar_servidor()
            _esperar_apagado()
            arrancar_servidor()
            return 'Servidor reiniciado ↻' if _esperar_encendido() else 'No pudo arrancar ✖'
        if accion.startswith('■'):
            apagar_servidor()
            _esperar_apagado()
            return 'Servidor apagado ○'
        if accion.startswith('🌐'):
            if not encendido():
                arrancar_servidor()
                _esperar_encendido()
            webbrowser.open(URL)
            return 'Abierta en el navegador 🌐'
        if accion.startswith('📱'):
            return f'En el celular (misma red WiFi):  http://{ip_lan()}:{PUERTO}'
    except Exception as e:
        return f'Error: {e}'
    return ''


def main():
    raiz = tk.Tk()
    raiz.title('Mi Calendario · Servicio')
    raiz.resizable(False, False)
    raiz.attributes('-topmost', True)
    raiz.after(4000, lambda: raiz.attributes('-topmost', False))

    marco = ttk.Frame(raiz, padding=16)
    marco.pack(fill='both', expand=True)

    ttk.Label(marco, text='📅  Mi Calendario', font=('TkDefaultFont', 14, 'bold')).pack()
    estado = ttk.Label(marco, text='', font=('TkDefaultFont', 10))
    estado.pack(pady=(2, 10))

    ttk.Label(marco, text='¿Qué quieres hacer?').pack(anchor='w')
    combo = ttk.Combobox(marco, state='readonly', width=32, values=ACCIONES)
    combo.pack(pady=(2, 10))

    resultado = ttk.Label(marco, text='', wraplength=300,
                          font=('TkDefaultFont', 9), foreground='#0a7')
    resultado.pack(pady=(0, 8))

    def refrescar():
        if encendido():
            estado.config(text=f'●  Encendido   —   {URL}', foreground='#0a7')
        else:
            estado.config(text='○  Apagado', foreground='#c33')
        combo.set(ACCIONES[4] if encendido() else ACCIONES[0])

    def al_terminar(msg):
        resultado.config(text=msg)
        boton.config(state='normal')
        refrescar()

    def al_ejecutar():
        boton.config(state='disabled')
        resultado.config(text='Trabajando…')
        accion = combo.get()
        def hilo():
            msg = ejecutar(accion)
            raiz.after(0, lambda: al_terminar(msg))
        threading.Thread(target=hilo, daemon=True).start()

    boton = ttk.Button(marco, text='Ejecutar', command=al_ejecutar)
    boton.pack()

    refrescar()
    raiz.mainloop()


if __name__ == '__main__':
    main()
