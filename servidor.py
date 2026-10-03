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


def _norm(s):
    return ' '.join((s or '').lower().split())


def fusionar_usuario(a, b):
    """Fusiona dos documentos del MISMO usuario: el perfil viene del más
    reciente, pero las notas y categorías de ambos se combinan sin
    duplicar (notas: por id y por título+fecha; categorías: por nombre,
    remapeando sus referencias)."""
    base, otro = (a, b) if a.get('actualizado', 0) >= b.get('actualizado', 0) else (b, a)
    categorias = {c['id']: c for c in base.get('categorias', [])}
    por_nombre = {_norm(c.get('nombre')): c for c in categorias.values()}
    mapa = {}
    for c in otro.get('categorias', []):
        nn = _norm(c.get('nombre'))
        if nn in por_nombre:
            mapa[c['id']] = por_nombre[nn]['id']
        else:
            categorias[c['id']] = c
            por_nombre[nn] = c
    notas = {n['id']: n for n in base.get('notas', [])}
    usados = {_norm(n.get('titulo')) + '|' + n.get('fecha', '') for n in notas.values()}
    for n in otro.get('notas', []):
        clave = _norm(n.get('titulo')) + '|' + n.get('fecha', '')
        if n['id'] in notas or clave in usados:
            continue
        m = dict(n)
        m['catId'] = mapa.get(m.get('catId'), m.get('catId'))
        notas[m['id']] = m
        usados.add(clave)
    r = dict(base)
    r['categorias'] = list(categorias.values())
    r['notas'] = list(notas.values())
    r['actualizado'] = max(a.get('actualizado', 0), b.get('actualizado', 0))
    return r


def fusionar(entrantes):
    """Aplica los usuarios recibidos y devuelve la lista final consolidada:
    - Los usuarios se emparejan por id o por nombre (mismo usuario con ids
      distintos en cada dispositivo) y se fusionan sus notas y categorías.
    - Los duplicados ya guardados con el mismo nombre también se consolidan.
    - Solo se crea un usuario si no existe ninguno con ese id o nombre."""
    with CANDADO:
        with abrir_db() as con:
            # 1. consolidar lo guardado (mismo nombre = mismo usuario)
            consolidados = {}   # nombre normalizado -> usuario
            for (datos,) in con.execute('SELECT datos FROM usuarios'):
                u = json.loads(datos)
                nn = _norm(u.get('nombre'))
                previo = consolidados.get(nn)
                consolidados[nn] = u if previo is None else fusionar_usuario(previo, u)
            # 2. aplicar los usuarios entrantes
            por_id = {u.get('id'): u for u in consolidados.values()}
            for u in entrantes:
                uid = u.get('id')
                if not uid:
                    continue
                u['actualizado'] = int(u.get('actualizado') or 0)
                previo = por_id.get(uid) or consolidados.get(_norm(u.get('nombre')))
                if previo is None:
                    consolidados[_norm(u.get('nombre'))] = u
                    por_id[uid] = u
                else:
                    nn_viejo = _norm(previo.get('nombre'))
                    f = fusionar_usuario(previo, u)
                    f['id'] = previo['id']          # conserva el id existente
                    consolidados.pop(nn_viejo, None)
                    consolidados[_norm(f.get('nombre'))] = f
                    por_id[previo['id']] = f
            # 3. reemplazar la tabla (elimina los ids duplicados antiguos)
            finales = list(consolidados.values())
            con.execute('DELETE FROM usuarios')
            for u in finales:
                con.execute('INSERT INTO usuarios(id, actualizado, datos) VALUES(?,?,?)',
                            (u['id'], int(u.get('actualizado') or 0),
                             json.dumps(u, ensure_ascii=False)))
            return finales


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
