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
import random
import sqlite3
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from socket import gethostbyname_ex, gethostname
import socket as socket_red

BASE = os.path.dirname(os.path.abspath(__file__))
PUERTO = 8177
PUERTO_DESCUBRIMIENTO = 8178
DB = os.path.join(BASE, 'datos', 'calendario.sqlite')
os.makedirs(os.path.dirname(DB), exist_ok=True)
CANDADO = threading.Lock()
CANDADO_LOG = threading.Lock()

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
    con.execute('''CREATE TABLE IF NOT EXISTS eliminados (
        id TEXT PRIMARY KEY, nombre TEXT NOT NULL, ts INTEGER NOT NULL)''')
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
    # bajas de notas: una eliminación en cualquier dispositivo se propaga
    # (gana si es posterior a la creación de la nota; se olvidan a los 90 días)
    bajas = {}
    for baja in list(base.get('notasEliminadas', [])) + list(otro.get('notasEliminadas', [])):
        clave = '%s|%s|%s' % (baja.get('id', ''), _norm(baja.get('titulo', '')), baja.get('fecha', ''))
        if clave not in bajas or baja.get('ts', 0) > bajas[clave].get('ts', 0):
            bajas[clave] = baja

    def nota_eliminada(n):
        for baja in bajas.values():
            mismo = (baja.get('id') and baja.get('id') == n.get('id')) or (
                _norm(baja.get('titulo', '')) == _norm(n.get('titulo', '')) and
                baja.get('fecha') == n.get('fecha'))
            if mismo and baja.get('ts', 0) > n.get('creada', 0):
                return True
        return False

    limite = int(time.time() * 1000) - 90 * 24 * 3600 * 1000
    r = dict(base)
    r['categorias'] = list(categorias.values())
    r['notas'] = [n for n in notas.values() if not nota_eliminada(n)]
    r['notasEliminadas'] = [b for b in bajas.values() if b.get('ts', 0) > limite]
    r['actualizado'] = max(a.get('actualizado', 0), b.get('actualizado', 0))
    return r


def _leer_bajas(con):
    return [{'id': f[0], 'nombre': f[1], 'ts': f[2]}
            for f in con.execute('SELECT id, nombre, ts FROM eliminados')]


def _aplicar_bajas(usuarios, bajas):
    """Quita los usuarios eliminados: una baja gana si es más reciente
    que la última modificación del usuario (por id o por nombre)."""
    vivos = []
    for u in usuarios:
        nn = _norm(u.get('nombre'))
        eliminado = any(
            (b['id'] == u.get('id') or _norm(b.get('nombre')) == nn)
            and b['ts'] > int(u.get('actualizado') or 0)
            for b in bajas)
        if not eliminado:
            vivos.append(u)
    return vivos


def fusionar(entrantes, bajas_entrantes=None):
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
            # 3. registrar bajas (eliminaciones) y aplicarlas
            for b in bajas_entrantes or []:
                if not b.get('id'):
                    continue
                previo = con.execute('SELECT ts FROM eliminados WHERE id=?', (b['id'],)).fetchone()
                ts = int(b.get('ts') or 0)
                if previo is None:
                    con.execute('INSERT INTO eliminados(id, nombre, ts) VALUES(?,?,?)',
                                (b['id'], b.get('nombre') or '', ts))
                elif ts > previo[0]:
                    con.execute('UPDATE eliminados SET nombre=?, ts=? WHERE id=?',
                                (b.get('nombre') or '', ts, b['id']))
            bajas = _leer_bajas(con)
            finales = _aplicar_bajas(list(consolidados.values()), bajas)
            limite = int(time.time() * 1000) - 90 * 24 * 3600 * 1000
            con.execute('DELETE FROM eliminados WHERE ts < ?', (limite,))
            # 4. reemplazar la tabla (elimina los ids duplicados antiguos)
            con.execute('DELETE FROM usuarios')
            for u in finales:
                con.execute('INSERT INTO usuarios(id, actualizado, datos) VALUES(?,?,?)',
                            (u['id'], int(u.get('actualizado') or 0),
                             json.dumps(u, ensure_ascii=False)))
            return finales, _leer_bajas(con)


ID_ARCHIVO = os.path.join(BASE, 'datos', 'id_servidor.txt')


def id_servidor():
    """Identificador persistente del servidor (para que el celular no
    duplique el mismo servidor detectado por varias IP)."""
    try:
        with open(ID_ARCHIVO) as f:
            v = f.read().strip()
        if v:
            return v
    except OSError:
        pass
    v = uuid.uuid4().hex[:12]
    with open(ID_ARCHIVO, 'w') as f:
        f.write(v)
    return v


CANDADO_CODIGO = threading.Lock()
CODIGO = {'codigo': None, 'expira': 0.0, 'usado': False}


def generar_codigo():
    with CANDADO_CODIGO:
        CODIGO['codigo'] = ''.join(random.choices('0123456789', k=6))
        CODIGO['expira'] = time.time() + 300      # válido 5 minutos
        CODIGO['usado'] = False
        return CODIGO['codigo'], CODIGO['expira']


def hilo_descubrimiento():
    """Responde por UDP a los celulares que buscan servidores en la red."""
    try:
        s = socket_red.socket(socket_red.AF_INET, socket_red.SOCK_DGRAM)
        s.setsockopt(socket_red.SOL_SOCKET, socket_red.SO_REUSEADDR, 1)
        s.bind(('', PUERTO_DESCUBRIMIENTO))
    except Exception as e:
        print(f'• Descubrimiento en red no disponible: {e}')
        return
    while True:
        try:
            datos, addr = s.recvfrom(1024)
            if b'MICALENDARIO' in datos.upper():
                resp = json.dumps({'app': 'micalendario', 'id': id_servidor(),
                                   'nombre': gethostname(), 'puerto': PUERTO}).encode()
                s.sendto(resp, addr)
        except Exception:
            pass


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
        if ruta == '/api/vinculacion':
            if 'nuevo=1' in (self.path.split('?')[1] if '?' in self.path else ''):
                generar_codigo()
            with CANDADO_CODIGO:
                valido = (CODIGO['codigo'] and not CODIGO['usado']
                          and CODIGO['expira'] > time.time())
            if not valido:
                generar_codigo()
            with CANDADO_CODIGO:
                return self.json_({'codigo': CODIGO['codigo'],
                                   'expira_en': max(0, int(CODIGO['expira'] - time.time()))})
        if ruta == '/api/estado':
            with CANDADO:
                with abrir_db() as con:
                    bajas = _leer_bajas(con)
                    guardados = [json.loads(f[0]) for f in con.execute('SELECT datos FROM usuarios')]
                    usuarios = _aplicar_bajas(guardados, bajas)
            return self.json_({'users': usuarios, 'eliminados': bajas})
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
        if ruta == '/api/vincular':
            try:
                largo = int(self.headers.get('Content-Length') or 0)
                d = json.loads(self.rfile.read(largo) or b'{}')
                with CANDADO_CODIGO:
                    ok = (CODIGO['codigo'] == str(d.get('codigo', '')).strip()
                          and not CODIGO['usado'] and CODIGO['expira'] > time.time())
                    if ok:
                        CODIGO['usado'] = True
                if ok:
                    with CANDADO_LOG:
                        with open(os.path.join(BASE, 'datos', 'servidor.log'), 'a') as f:
                            f.write(f'{time.strftime("%Y-%m-%d %H:%M:%S")} {self.client_address[0]} '
                                    f'VINCULADO dispositivo={d.get("dispositivo", "?")}\n')
                    return self.json_({'ok': True, 'servidor': gethostname(), 'puerto': PUERTO})
                return self.json_({'ok': False, 'error': 'codigo incorrecto o expirado'}, 403)
            except Exception as e:
                return self.json_({'error': str(e)}, 400)
        if ruta != '/api/sync':
            return self.json_({'error': 'ruta desconocida'}, 404)
        try:
            largo = int(self.headers.get('Content-Length') or 0)
            cuerpo = json.loads(self.rfile.read(largo) or b'{}')
            entrantes = cuerpo.get('users') or []
            bajas = cuerpo.get('eliminados') or []
            finales, todas_bajas = fusionar(entrantes, bajas)
            return self.json_({'users': finales, 'eliminados': todas_bajas,
                               'servidorHora': int(time.time() * 1000)})
        except Exception as e:
            return self.json_({'error': str(e)}, 400)

    def log_message(self, fmt, *args):
        with CANDADO_LOG:
            with open(os.path.join(BASE, 'datos', 'servidor.log'), 'a') as f:
                f.write(f'{time.strftime("%Y-%m-%d %H:%M:%S")} {self.client_address[0]} {fmt % args}\n')


def ip_lan():
    try:
        return gethostbyname_ex(gethostname())[2][0]
    except Exception:
        return '127.0.0.1'


if __name__ == '__main__':
    threading.Thread(target=hilo_descubrimiento, daemon=True).start()
    srv = ThreadingHTTPServer(('0.0.0.0', PUERTO), Manejador)
    print(f'✔ Mi Calendario escuchando en el puerto {PUERTO}')
    print(f'   En este equipo : http://localhost:{PUERTO}')
    print(f'   En el celular  : http://{ip_lan()}:{PUERTO}  (misma red WiFi)')
    print(f'   Base de datos  : {DB}')
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print('\nServidor detenido.')
