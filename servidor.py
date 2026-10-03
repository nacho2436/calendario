#!/usr/bin/env python3
# ============================================================
#  Mi Calendario · servidor web + sincronización con SQLite
#  Solo usa la biblioteca estándar de Python (sin dependencias).
#
#  - Sirve la app web (index.html, styles.css, app.js)
#  - GET  /api/ping    → prueba de conexión
#  - GET  /api/estado  → todos los usuarios (JSON)
#  - POST /api/sync    → fusiona usuarios recibidos (el más nuevo
#                        gana por "actualizado") y devuelve el resultado
#  - Los datos viven en datos/calendario.sqlite
# ============================================================
import json
import os
import sqlite3
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from socket import gethostbyname_ex, gethostname

BASE = os.path.dirname(os.path.abspath(__file__))
PUERTO = 8177
DB = os.path.join(BASE, 'datos', 'calendario.sqlite')
os.makedirs(os.path.dirname(DB), exist_ok=True)
CANDADO = threading.Lock()

ESTATICOS = {  # solo estos archivos se sirven (la ruta puede traer ?v=N)
    '/': ('index.html', 'text/html; charset=utf-8'),
    '/index.html': ('index.html', 'text/html; charset=utf-8'),
    '/styles.css': ('styles.css', 'text/css; charset=utf-8'),
    '/app.js': ('app.js', 'application/javascript; charset=utf-8'),
}


def abrir_db():
    con = sqlite3.connect(DB)
    con.execute('''CREATE TABLE IF NOT EXISTS usuarios (
        id TEXT PRIMARY KEY,
        actualizado INTEGER NOT NULL DEFAULT 0,
        datos TEXT NOT NULL)''')
    return con


def leer_usuarios():
    with CANDADO:
        with abrir_db() as con:
            filas = con.execute('SELECT datos FROM usuarios').fetchall()
    return [json.loads(f[0]) for f in filas]


def fusionar(entrantes):
    """Aplica los usuarios recibidos con la política "el más nuevo gana"
    (comparando la marca actualizado) y devuelve la lista final."""
    with CANDADO:
        with abrir_db() as con:
            actuales = {}
            for (datos,) in con.execute('SELECT datos FROM usuarios'):
                u = json.loads(datos)
                actuales[u.get('id')] = u
            for u in entrantes:
                uid = u.get('id')
                if not uid:
                    continue
                u['actualizado'] = int(u.get('actualizado') or 0)
                previo = actuales.get(uid)
                if previo is None or u['actualizado'] >= int(previo.get('actualizado') or 0):
                    actuales[uid] = u
            for uid, u in actuales.items():
                con.execute(
                    '''INSERT INTO usuarios(id, actualizado, datos) VALUES(?,?,?)
                       ON CONFLICT(id) DO UPDATE SET
                         actualizado=excluded.actualizado, datos=excluded.datos''',
                    (uid, int(u.get('actualizado') or 0), json.dumps(u, ensure_ascii=False)))
            return list(actuales.values())


class Manejador(BaseHTTPRequestHandler):
    server_version = 'MiCalendario/1.0'

    # ── utilidades ──
    def responder(self, codigo, contenido, tipo):
        self.send_response(codigo)
        self.send_header('Content-Type', tipo)
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Content-Length', str(len(contenido)))
        self.end_headers()
        self.wfile.write(contenido)

    def json_(self, obj, codigo=200):
        self.responder(codigo, json.dumps(obj, ensure_ascii=False).encode('utf-8'),
                       'application/json; charset=utf-8')

    # ── rutas ──
    def do_GET(self):
        ruta = self.path.split('?')[0]
        if ruta == '/api/ping':
            return self.json_({'ok': True, 'hora': int(time.time() * 1000)})
        if ruta == '/api/estado':
            return self.json_({'users': leer_usuarios()})
        if ruta.startswith('/api/'):
            return self.json_({'error': 'ruta desconocida'}, 404)
        if ruta in ESTATICOS:
            archivo, tipo = ESTATICOS[ruta]
            try:
                with open(os.path.join(BASE, archivo), 'rb') as f:
                    return self.responder(200, f.read(), tipo)
            except OSError:
                return self.responder(404, b'no encontrado', 'text/plain')
        return self.responder(404, b'no encontrado', 'text/plain')

    def do_POST(self):
        ruta = self.path.split('?')[0]
        if ruta != '/api/sync':
            return self.json_({'error': 'ruta desconocida'}, 404)
        try:
            largo = int(self.headers.get('Content-Length') or 0)
            cuerpo = json.loads(self.rfile.read(largo) or b'{}')
            entrantes = cuerpo.get('users') or []
            finales = fusionar(entrantes)
            return self.json_({'users': finales, 'servidorHora': int(time.time() * 1000)})
        except Exception as e:
            return self.json_({'error': str(e)}, 400)

    def log_message(self, fmt, *args):
        pass  # silencioso; el log general va al archivo redirigido


def ip_lan():
    try:
        return gethostbyname_ex(gethostname())[2][0]
    except Exception:
        return '127.0.0.1'


if __name__ == '__main__':
    srv = ThreadingHTTPServer(('0.0.0.0', PUERTO), Manejador)
    print(f'✔ Mi Calendario escuchando en el puerto {PUERTO}')
    print(f'   En este equipo : http://localhost:{PUERTO}')
    print(f'   En el celular  : http://{ip_lan()}:{PUERTO}  (misma red WiFi)')
    print(f'   Base de datos  : {DB}')
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print('\nServidor detenido.')
