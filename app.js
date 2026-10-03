'use strict';

/* ═══════════════════════════════════════════════════════════
   Mi Calendario · Notas y festivos de Colombia
   Aplicación de una sola página · datos en localStorage,
   separados por usuario (notas, categorías y tema propios).
   ═══════════════════════════════════════════════════════════ */

/* ─────────── 1. Utilidades ─────────── */
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];
const pad  = n => String(n).padStart(2, '0');
const uid  = p => `${p}_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
const esc  = s => String(s ?? '').replace(/[&<>"']/g, c =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const MESES       = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
const DIAS_CORTOS = ['Lun','Mar','Mié','Jue','Vie','Sáb','Dom'];
const DIAS_LARGOS = ['domingo','lunes','martes','miércoles','jueves','viernes','sábado'];

const keyOf    = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const todayKey = () => keyOf(new Date());

function fmtFechaLarga(key){
  const [y, m, d] = key.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  const base = `${DIAS_LARGOS[dt.getDay()]}, ${d} de ${MESES[m - 1].toLowerCase()}`;
  if (key === todayKey()) return `Hoy · ${base}`;
  if (key === keyOf(new Date(Date.now() + 864e5))) return `Mañana · ${base}`;
  if (key === keyOf(new Date(Date.now() - 864e5))) return `Ayer · ${base}`;
  return y !== new Date().getFullYear() ? `${base} de ${y}` : base;
}

/* Hora: se guarda en 24 h ("HH:mm") y se muestra en formato 12 h con am/pm */
function fmtHora(hhmm){
  if (!hhmm) return '';
  let [h, m] = hhmm.split(':').map(Number);
  const suf = h < 12 ? 'am' : 'pm';
  h = (h % 12) || 12;
  return `${h}:${pad(m)} ${suf}`;
}
function setHora12(hhmm){
  if (!hhmm){ $('#noteHora').value = ''; $('#noteMin').value = '00'; $('#noteAmpm').value = 'am'; return; }
  const [h, m] = hhmm.split(':').map(Number);
  $('#noteAmpm').value = h < 12 ? 'am' : 'pm';
  $('#noteHora').value = String((h % 12) || 12);
  $('#noteMin').value  = pad(m);
}
function getHora12(){
  const h = $('#noteHora').value;
  if (!h) return '';
  const pm = $('#noteAmpm').value === 'pm';
  return pad((Number(h) % 12) + (pm ? 12 : 0)) + ':' + ($('#noteMin').value || '00');
}

/* ─────────── 2. Festivos de Colombia (Ley 51 de 1983) ───────────
   Los festivos diferentes de Año Nuevo, 1.º de mayo, 20 de julio,
   7 de agosto, 8 y 25 de diciembre, Jueves y Viernes Santo se
   trasladan al lunes siguiente. */
function pascua(y){
  const a = y % 19, b = Math.floor(y / 100), c = y % 100,
        d = Math.floor(b / 4), e = b % 4,
        f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3),
        h = (19 * a + b - d - g + 15) % 30,
        i = Math.floor(c / 4), k = c % 4,
        l = (32 + 2 * e + 2 * i - h - k) % 7,
        m = Math.floor((a + 11 * h + 22 * l) / 451),
        mes = Math.floor((h + l - 7 * m + 114) / 31),
        dia = ((h + l - 7 * m + 114) % 31) + 1;
  return new Date(y, mes - 1, dia);
}
function lunesSiguiente(d){
  const iso = d.getDay() === 0 ? 7 : d.getDay();       // lunes = 1 … domingo = 7
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + ((8 - iso) % 7));
}
const cacheFestivos = new Map();
function festivosDelAnio(y){
  if (cacheFestivos.has(y)) return cacheFestivos.get(y);
  const p = pascua(y);
  const suma = n => new Date(p.getFullYear(), p.getMonth(), p.getDate() + n);
  const map = new Map();
  map.set(`${y}-01-01`, 'Año Nuevo');
  map.set(keyOf(lunesSiguiente(new Date(y, 0, 6))),   'Reyes Magos');
  map.set(keyOf(lunesSiguiente(new Date(y, 2, 19))),  'Día de San José');
  map.set(keyOf(suma(-3)), 'Jueves Santo');
  map.set(keyOf(suma(-2)), 'Viernes Santo');
  map.set(`${y}-05-01`, 'Día del Trabajo');
  map.set(keyOf(lunesSiguiente(suma(39))), 'Ascensión de Jesús');
  map.set(keyOf(lunesSiguiente(suma(60))), 'Corpus Christi');
  map.set(keyOf(lunesSiguiente(suma(68))), 'Sagrado Corazón de Jesús');
  map.set(keyOf(lunesSiguiente(new Date(y, 5, 29))), 'San Pedro y San Pablo');
  map.set(`${y}-07-20`, 'Día de la Independencia');
  map.set(`${y}-08-07`, 'Batalla de Boyacá');
  map.set(keyOf(lunesSiguiente(new Date(y, 7, 15))), 'Asunción de la Virgen');
  map.set(keyOf(lunesSiguiente(new Date(y, 9, 12))), 'Día de la Raza');
  map.set(keyOf(lunesSiguiente(new Date(y, 10, 1))), 'Todos los Santos');
  map.set(keyOf(lunesSiguiente(new Date(y, 10, 11))), 'Independencia de Cartagena');
  map.set(`${y}-12-08`, 'Inmaculada Concepción');
  map.set(`${y}-12-25`, 'Navidad');
  cacheFestivos.set(y, map);
  return map;
}
function nombreFestivo(key){
  const y = Number(key.slice(0, 4));
  for (const yy of [y - 1, y, y + 1]){
    if (festivosDelAnio(yy).has(key)) return festivosDelAnio(yy).get(key);
  }
  return null;
}

/* ─────────── 3. Constantes de interfaz ─────────── */
const TEMAS = [
  { id: 'claro',  nombre: 'Claro',      colores: ['#eef1f7', '#ffffff', '#4f6df5', '#d92626'] },
  { id: 'oscuro', nombre: 'Oscuro',     colores: ['#0e1117', '#171c26', '#7b96ff', '#ff6b6b'] },
  { id: 'noche',  nombre: 'Azul noche', colores: ['#0a0f2b', '#131a3d', '#6f9bff', '#ff7575'] },
  { id: 'bosque', nombre: 'Bosque',     colores: ['#0e1712', '#152219', '#4cc38a', '#ff8080'] },
  { id: 'pastel', nombre: 'Pastel',     colores: ['#fdf3f7', '#ffffff', '#c66bd6', '#e05263'] },
  { id: 'cafe',   nombre: 'Café',       colores: ['#f4ede2', '#fffdf8', '#b07d3f', '#c9342c'] },
];
const SWATCHES = ['#ef6c6c', '#f2913d', '#f5c542', '#66bb6a', '#31a3c3', '#4f6df5', '#8e6df5', '#d65cb0', '#8d99ae', '#5d4037'];
const EMOJIS = ['🙂','😎','🤓','🧑‍💻','🐱','🐶','🦊','🐼','🌸','🌈','⭐','🌙','🚀','⚡','🎨','🎵','⚽','🎮','📚','🧠','💻','🍕','🎉','🏆'];

/* ─────────── 4. Estado y almacenamiento ─────────── */
const STORE = 'calendarioColombia.v1';

function categoriasPorDefecto(){
  return [
    { id: uid('cat'), nombre: 'Personal',   color: '#8e6df5' },
    { id: uid('cat'), nombre: 'Trabajo',    color: '#4f6df5' },
    { id: uid('cat'), nombre: 'Salud',      color: '#66bb6a' },
    { id: uid('cat'), nombre: 'Cumpleaños', color: '#f2913d' },
  ];
}
function nuevoUsuario(nombre){
  const cats = categoriasPorDefecto();
  return {
    id: uid('usr'), nombre, avatar: '🙂', color: '#4f6df5', tema: 'claro',
    categorias: cats,
    notas: [{
      id: uid('note'), titulo: '¡Hola! 👋 Nota de bienvenida',
      desc: 'Haz clic en cualquier día del calendario para agregar tus notas y recordatorios.',
      fecha: todayKey(), hora: '09:00', catId: cats[0].id, color: '', repeticion: '', done: false, creada: Date.now(),
    }],
  };
}
function estadoInicial(){
  const u = nuevoUsuario('Usuario 1');
  return { version: 1, currentUserId: u.id, users: [u] };
}
function cargar(){
  try{
    const raw = localStorage.getItem(STORE);
    if (!raw) return null;
    const data = JSON.parse(raw);
    if (!data || !Array.isArray(data.users) || !data.users.length) return null;
    if (!data.users.find(u => u.id === data.currentUserId)) data.currentUserId = data.users[0].id;
    return data;
  } catch { return null; }
}
function guardar(){
  try { localStorage.setItem(STORE, JSON.stringify(state)); } catch { /* almacenamiento no disponible */ }
}

let state = cargar() || estadoInicial();
const yo  = () => state.users.find(u => u.id === state.currentUserId);

/* Helpers de dominio */
const catDe       = nota => yo().categorias.find(c => c.id === nota.catId);
const colorDe     = nota => nota.color || catDe(nota)?.color || '#8d99ae';
const notasDel    = key  => yo().notas.filter(n => n.fecha === key);
const notasVisibles = () => (ui.filterCat ? yo().notas.filter(n => n.catId === ui.filterCat) : yo().notas);

/* ─────────── Notas repetitivas ───────────
   Una nota con repetición ocurre en su fecha de inicio y, a partir de
   ella: cada día, cada 7 días, el mismo día de cada mes o el mismo día
   de cada año. Se edita/borra la serie completa. */
const REP_CORTO = { diaria: 'cada día', semanal: 'cada semana', mensual: 'cada mes', anual: 'cada año' };

function diasEntre(a, b){
  const [ay, am, ad] = a.split('-').map(Number);
  const [by, bm, bd] = b.split('-').map(Number);
  return Math.round((Date.UTC(by, bm - 1, bd) - Date.UTC(ay, am - 1, ad)) / 86400000);
}
function ocurreEn(nota, key){
  if (!nota.repeticion) return nota.fecha === key;
  if (key < nota.fecha) return false;
  const [ny, nm, nd] = nota.fecha.split('-').map(Number);
  const [y, m, d] = key.split('-').map(Number);
  switch (nota.repeticion){
    case 'diaria':  return true;
    case 'semanal': return diasEntre(nota.fecha, key) % 7 === 0;
    case 'mensual': return d === nd && (y * 12 + m) >= (ny * 12 + nm);
    case 'anual':   return d === nd && m === nm && y >= ny;
  }
  return false;
}
/** Próxima fecha en que ocurre la nota (hoy o después). */
function proximaFecha(nota){
  if (!nota.repeticion || nota.fecha >= todayKey()) return nota.fecha;
  let d = new Date();
  for (let i = 0; i < 800; i++){
    const k = keyOf(d);
    if (ocurreEn(nota, k)) return k;
    d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1);
  }
  return nota.fecha;
}
const notasQueOcurren = key => yo().notas.filter(n => ocurreEn(n, key));

/* ─────────── Notificaciones antes de la nota ───────────
   Cada nota puede tener varios avisos (minutos antes). Mientras la
   página esté abierta se verifica cada 30 s y se lanza una
   notificación del navegador + un aviso interno con sonido. */
const AVISOS_DEF = [
  { min: 5,    corto: '5 min',   label: '5 minutos antes' },
  { min: 15,   corto: '15 min',  label: '15 minutos antes' },
  { min: 30,   corto: '30 min',  label: '30 minutos antes' },
  { min: 60,   corto: '1 hora',  label: '1 hora antes' },
  { min: 120,  corto: '2 horas', label: '2 horas antes' },
  { min: 1440, corto: '1 día',   label: '1 día antes' },
];
const avisoDef = min => AVISOS_DEF.find(a => a.min === min) || { min, corto: min + ' min', label: min + ' minutos antes' };

function cargarAvisados(){
  try{
    const hoy = todayKey(), manana = keyOf(new Date(Date.now() + 864e5));
    const vigentes = JSON.parse(localStorage.getItem('calendarioAvisos') || '[]')
      .filter(k => k.includes(hoy) || k.includes(manana));
    localStorage.setItem('calendarioAvisos', JSON.stringify(vigentes));
    return new Set(vigentes);
  } catch { return new Set(); }
}
const avisados = cargarAvisados();
function persistirAvisados(){
  try { localStorage.setItem('calendarioAvisos', JSON.stringify([...avisados])); } catch {}
}

function revisarAvisos(){
  const ahora = new Date();
  const fechas = [todayKey(), keyOf(new Date(Date.now() + 864e5))];   // hoy y mañana (para el aviso de "1 día antes")
  for (const n of yo().notas){
    if (!n.hora || !n.avisos?.length) continue;
    for (const f of fechas){
      if (!ocurreEn(n, f)) continue;
      const [y, m, d]  = f.split('-').map(Number);
      const [hh, mm]   = n.hora.split(':').map(Number);
      const inicio     = new Date(y, m - 1, d, hh, mm);
      for (const av of n.avisos){
        const tAlerta = new Date(inicio.getTime() - av * 60000);
        const clave = `${n.id}|${f}|${av}`;
        if (avisados.has(clave)) continue;
        if (ahora >= tAlerta && ahora - tAlerta < 90000){
          avisados.add(clave);
          persistirAvisados();
          dispararAviso(n, av);
        }
      }
    }
  }
}

function dispararAviso(nota, av){
  const def = avisoDef(av);
  if ('Notification' in window && Notification.permission === 'granted'){
    try{
      new Notification(`⏰ ${nota.titulo}`, {
        body: `${def.label} — comienza a las ${fmtHora(nota.hora)}`,
        tag: `${nota.id}-${av}`,
      });
    } catch {}
  }
  encolarAviso({
    titulo: nota.titulo,
    detalle: `${def.label} — comienza a las ${fmtHora(nota.hora)}`,
    desc: nota.desc || '',
  });
}

/* Ventana central de aviso: hay que pulsar Aceptar (no se cierra con
   clic por fuera ni con Escape). Si llegan varios avisos, van en cola. */
const colaAvisos = [];
function encolarAviso(aviso){
  colaAvisos.push(aviso);
  if (!$('#modal-aviso').classList.contains('open')) mostrarSiguienteAviso();
}
function mostrarSiguienteAviso(){
  const a = colaAvisos.shift();
  if (!a){
    if ($('#modal-aviso').classList.contains('open')) cerrarModal($('#modal-aviso'));
    return;
  }
  $('#avisoTitulo').textContent = a.titulo;
  $('#avisoDetalle').textContent = a.detalle;
  const d = $('#avisoDesc');
  d.textContent = a.desc;
  d.style.display = a.desc ? '' : 'none';
  abrirModal('modal-aviso');
  sonarAviso();
}
function sonarAviso(){
  try{
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    [0, 0.35, 0.7].forEach(t => {
      const osc = ctx.createOscillator(), gain = ctx.createGain();
      osc.connect(gain); gain.connect(ctx.destination);
      osc.frequency.value = 830; gain.gain.value = 0.14;
      osc.start(ctx.currentTime + t); osc.stop(ctx.currentTime + t + 0.18);
    });
    setTimeout(() => ctx.close(), 1300);
  } catch {}
}

function avisosDe(nota){
  return (nota.avisos || []).map(a => avisoDef(a).corto).join(', ');
}

/* Estado de la interfaz */
const HOY = new Date();
const ui = {
  vista: 'calendario',
  vistaCal: 'mes',                       // 'hoy' | 'semana' | 'mes'
  anio: HOY.getFullYear(), mes: HOY.getMonth(),
  fecha: HOY,                            // fecha base de las vistas día y semana
  filterCat: null, busqueda: '', estado: 'todas', dayKey: null,
};

/* ─────────── 5. Render principal ─────────── */
function render(){
  $$('#tabs .tab').forEach(t => t.classList.toggle('active', t.dataset.view === ui.vista));
  $$('.view').forEach(v => v.classList.toggle('active', v.id === 'view-' + ui.vista));
  renderChipUsuario();
  aplicarTema();
  renderVista();
}
function renderVista(){
  if (ui.vista === 'calendario') renderCalendario();
  else if (ui.vista === 'notas') renderNotas();
  else renderUsuarios();
}
function refrescarTrasCambio(){
  renderVista();
  if (ui.dayKey && $('#modal-day').classList.contains('open')) pintarModalDia();
}
function renderChipUsuario(){
  const u = yo();
  $('#chipAvatar').textContent = u.avatar;
  $('#chipAvatar').style.background = u.color;
  $('#chipName').textContent = u.nombre;
}
function aplicarTema(){
  document.body.dataset.theme = yo().tema;
  $('#themeSelect').value = yo().tema;
}
function definirTema(id){
  yo().tema = id;
  aplicarTema();
  guardar();
  if (ui.vista === 'usuarios') renderUsuarios();
  toast('Tema aplicado 🎨');
}

/* ─────────── 6. Vista Calendario ─────────── */
function renderCalendario(){
  $$('#vistaSeg button').forEach(b => b.classList.toggle('active', b.dataset.vista === ui.vistaCal));
  const esDia = ui.vistaCal === 'hoy';
  $('#calWeekdays').hidden = esDia;
  $('#calGrid').hidden = esDia;
  $('#calLegend').hidden = esDia;
  $('#vistaDia').hidden = !esDia;
  if (ui.vistaCal === 'mes'){
    $('#monthTitle').textContent = `${MESES[ui.mes]} ${ui.anio}`;
    renderGrid('mes');
  } else if (ui.vistaCal === 'semana'){
    $('#monthTitle').textContent = tituloSemana();
    renderGrid('semana');
  } else {
    $('#monthTitle').textContent = fmtFechaLarga(keyOf(ui.fecha));
    renderVistaDia();
  }
  renderSidebar();
}
function mudarMes(delta){
  const d = new Date(ui.anio, ui.mes + delta, 1);
  ui.anio = d.getFullYear(); ui.mes = d.getMonth();
  renderCalendario();
}
/** Navegación ‹ › según la vista activa: mes, semana o día. */
function navegar(delta){
  if (ui.vistaCal === 'mes'){ mudarMes(delta); return; }
  const paso = ui.vistaCal === 'semana' ? 7 : 1;
  ui.fecha = new Date(ui.fecha.getFullYear(), ui.fecha.getMonth(), ui.fecha.getDate() + paso * delta);
  renderCalendario();
}
/** Lunes de la semana de la fecha dada. */
function inicioSemana(d){
  const iso = d.getDay() === 0 ? 7 : d.getDay();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() - (iso - 1));
}
function tituloSemana(){
  const ini = inicioSemana(ui.fecha);
  const fin = new Date(ini.getFullYear(), ini.getMonth(), ini.getDate() + 6);
  const abr = m => MESES[m].toLowerCase().slice(0, 3);
  return ini.getMonth() === fin.getMonth()
    ? `${ini.getDate()} – ${fin.getDate()} de ${MESES[ini.getMonth()].toLowerCase()} ${fin.getFullYear()}`
    : `${ini.getDate()} ${abr(ini.getMonth())} – ${fin.getDate()} ${abr(fin.getMonth())} ${fin.getFullYear()}`;
}

function renderSidebar(){
  const cats = yo().categorias;
  const notas = yo().notas;
  $('#sidebar').innerHTML = `
    <div class="side-head">
      <h3>Categorías</h3>
      <button class="btn icon" data-action="new-cat" title="Nueva categoría">＋</button>
    </div>
    <div class="cat-item ${ui.filterCat === null ? 'active' : ''}" data-action="filter" data-id="">
      <span class="cat-dot rainbow"></span>
      <span class="cat-name">Todas</span>
      <span class="count">${notas.length}</span>
    </div>
    ${cats.map(c => `
      <div class="cat-item ${ui.filterCat === c.id ? 'active' : ''}" data-action="filter" data-id="${c.id}">
        <span class="cat-dot" style="background:${c.color}"></span>
        <span class="cat-name">${esc(c.nombre)}</span>
        <span class="count">${notas.filter(n => n.catId === c.id).length}</span>
        <span class="cat-tools">
          <button data-action="edit-cat" data-id="${c.id}" title="Editar categoría">✏️</button>
          <button data-action="del-cat" data-id="${c.id}" title="Eliminar categoría">🗑️</button>
        </span>
      </div>`).join('')}
    <button class="btn subtle block new-cat-btn" data-action="new-cat">＋ Nueva categoría</button>
    <p class="hint">Toca una categoría para filtrar. Con ✏️ editas y con 🗑️ eliminas.</p>`;
}

/** HTML de una celda de día (la usan las vistas mensual y semanal).
    maxLineas se reduce con festivo para que todo quepa dentro del cuadro. */
function htmlCelda(d, otroMes, maxLineas, recortarFestivo = false){
  const key = keyOf(d);
  const festivo  = nombreFestivo(key);
  const esDomingo = d.getDay() === 0;
  const esSabado  = d.getDay() === 6;
  const notas = notasQueOcurren(key)
    .filter(n => !ui.filterCat || n.catId === ui.filterCat)
    .sort((a, b) => (a.hora || '').localeCompare(b.hora || ''));
  const cupo = recortarFestivo && festivo ? Math.min(maxLineas, 2) : maxLineas;
  const lineas = notas.slice(0, cupo).map(n => `
      <div class="day-note${n.done ? ' done' : ''}" style="--c:${colorDe(n)}" title="${esc(n.titulo)}">
        <i></i><span>${n.repeticion ? '🔁 ' : ''}${esc(n.titulo)}</span>
      </div>`).join('');
  const extra  = notas.length > cupo ? `<span class="more">+${notas.length - cupo} más</span>` : '';
  const tooltip = [
    festivo ? '🎉 Festivo: ' + festivo : '',
    ...notas.map(n => (n.repeticion ? '🔁 ' : '📝 ') + n.titulo + (n.hora ? ' (' + fmtHora(n.hora) + ')' : '') + (n.avisos?.length ? ' 🔔' : '')),
  ].filter(Boolean).join('\n');
  return `
    <div class="day${otroMes ? ' other' : ''}${key === todayKey() ? ' today' : ''}${festivo ? ' holiday' : ''}${esSabado ? ' weekend' : ''}${esDomingo ? ' domingo' : ''}"
         data-key="${key}" title="${esc(tooltip)}">
      <span class="day-num">${d.getDate()}</span>
      ${festivo ? `<span class="day-label">${esc(festivo)}</span>` : ''}
      <span class="day-notes">${lineas}${extra}</span>
    </div>`;
}

function renderGrid(modo){
  const grid = $('#calGrid');
  grid.classList.toggle('semana', modo === 'semana');
  let celdas = [];
  if (modo === 'mes'){
    const primero = new Date(ui.anio, ui.mes, 1);
    const offset  = (primero.getDay() + 6) % 7;      // la semana inicia el lunes
    for (let i = 0; i < 42; i++) celdas.push(new Date(ui.anio, ui.mes, 1 - offset + i));
    grid.innerHTML = celdas.map(d => htmlCelda(d, d.getMonth() !== ui.mes, 3, true)).join('');
  } else {
    const ini = inicioSemana(ui.fecha);
    for (let i = 0; i < 7; i++) celdas.push(new Date(ini.getFullYear(), ini.getMonth(), ini.getDate() + i));
    grid.innerHTML = celdas.map(d => htmlCelda(d, false, 8)).join('');
  }
}

/** Vista "Hoy": agenda completa del día seleccionado. */
function renderVistaDia(){
  const key = keyOf(ui.fecha);
  const festivo = nombreFestivo(key);
  const notas = notasQueOcurren(key)
    .filter(n => !ui.filterCat || n.catId === ui.filterCat)
    .sort((a, b) => (a.hora || '23:59').localeCompare(b.hora || '23:59'));
  $('#vistaDia').innerHTML = `
    <div class="dia-cabecera">
      <h3>${esc(fmtFechaLarga(key))}</h3>
      ${festivo ? `<span class="holiday-badge">🎉 ${esc(festivo)}</span>` : ''}
      <span class="chip-count">${notas.length} nota${notas.length === 1 ? '' : 's'}</span>
      <button class="btn primary sm" id="diaAddBtn">＋ Nota este día</button>
    </div>
    <div class="dia-lista">
      ${notas.length
        ? notas.map(n => notaCardHTML(n, { conFecha: false })).join('')
        : '<div class="empty">Sin notas este día.<br>¡Agrega una! 📝</div>'}
    </div>`;
}

/* ─────────── 7. Vista Notas ─────────── */
function renderNotas(){
  renderCatChips();
  const q = ui.busqueda.trim().toLowerCase();
  const notas = notasVisibles()
    .filter(n => (ui.estado === 'todas') || ((ui.estado === 'hechas') === !!n.done))
    .filter(n => !q || (n.titulo + ' ' + (n.desc || '')).toLowerCase().includes(q))
    .map(n => ({ n, cuando: proximaFecha(n) }))
    .sort((a, b) => (a.cuando + (a.n.hora || '')).localeCompare(b.cuando + (b.n.hora || '')));

  const cont = $('#notesList');
  if (!notas.length){
    cont.innerHTML = `
      <div class="empty">🍂 No hay notas para mostrar.
        <br><button class="btn primary" data-action="new-note">＋ Crear nota</button>
      </div>`;
    return;
  }
  let html = '', claveAnterior = null;
  for (const { n, cuando } of notas){
    if (cuando !== claveAnterior){
      claveAnterior = cuando;
      html += `<h3 class="date-group">${esc(fmtFechaLarga(cuando))}</h3>`;
    }
    html += notaCardHTML(n, { conFecha: false });
  }
  cont.innerHTML = html;
}

function notaCardHTML(n, { conFecha = true } = {}){
  const cat = catDe(n);
  const colorCat = cat?.color || '#8d99ae';
  const nombreCat = cat?.nombre || 'Sin categoría';
  return `
  <article class="note-card${n.done ? ' done' : ''}" style="--nc:${colorDe(n)}">
    <button class="note-check" data-action="toggle-nota" data-id="${n.id}"
            title="${n.done ? 'Marcar como pendiente' : 'Marcar como completada'}">${n.done ? '✓' : ''}</button>
    <div class="note-main">
      <div class="note-top">
        <h4>${esc(n.titulo)}</h4>
        <span class="chip" style="--c:${colorCat}">${esc(nombreCat)}</span>
      </div>
      <p class="note-meta">${conFecha ? '📅 ' + esc(fmtFechaLarga(proximaFecha(n))) : ''}${n.hora ? (conFecha ? ' · ⏰ ' : '⏰ ') + fmtHora(n.hora) : ''}${n.repeticion ? ' · 🔁 ' + REP_CORTO[n.repeticion] : ''}${n.avisos?.length ? ' · 🔔 ' + esc(avisosDe(n)) + ' antes' : ''}</p>
      ${n.desc ? `<p class="note-desc">${esc(n.desc)}</p>` : ''}
    </div>
    <div class="note-actions">
      <button class="btn icon" data-action="edit-nota" data-id="${n.id}" title="Editar nota">✏️</button>
      <button class="btn icon" data-action="del-nota" data-id="${n.id}" title="Eliminar nota">🗑️</button>
    </div>
  </article>`;
}

function renderCatChips(){
  $('#catChips').innerHTML = `
    <button class="chip-filter${ui.filterCat === null ? ' active' : ''}" data-action="filter" data-id="">Todas</button>
    ${yo().categorias.map(c =>
      `<button class="chip-filter${ui.filterCat === c.id ? ' active' : ''}" style="--c:${c.color}"
               data-action="filter" data-id="${c.id}">${esc(c.nombre)}</button>`).join('')}`;
}

/* ─────────── 8. Vista Usuarios ─────────── */
function renderUsuarios(){
  $('#userCards').innerHTML = state.users.map(u => {
    const activo = u.id === state.currentUserId;
    return `
    <article class="user-card${activo ? ' active' : ''}">
      ${activo ? '<span class="active-badge">✓ En uso</span>' : ''}
      <div class="avatar-big" style="--ac:${u.color}">${u.avatar}</div>
      <h3>${esc(u.nombre)}</h3>
      <p class="hint">${u.notas.length} nota${u.notas.length === 1 ? '' : 's'} · ${u.categorias.length} categoría${u.categorias.length === 1 ? '' : 's'}</p>
      <div class="user-actions">
        ${activo ? '' : `<button class="btn primary sm" data-action="use-user" data-id="${u.id}">Usar</button>`}
        <button class="btn sm" data-action="edit-user" data-id="${u.id}">Editar</button>
        <button class="btn sm danger" data-action="del-user" data-id="${u.id}">Eliminar</button>
      </div>
    </article>`;
  }).join('') + `
    <button class="user-card add" data-action="new-user">
      <span class="plus">＋</span>Nuevo usuario
    </button>`;

  $('#themeCards').innerHTML = TEMAS.map(t => `
    <button class="theme-card${yo().tema === t.id ? ' active' : ''}" data-action="set-tema" data-id="${t.id}">
      <span class="tc-prev">${t.colores.map(c => `<i style="background:${c}"></i>`).join('')}</span>
      <span class="tc-name">${t.nombre}${yo().tema === t.id ? ' ✓' : ''}</span>
    </button>`).join('');
}

/* ─────────── 9. Modales ─────────── */
let zModal = 100;
function abrirModal(id){
  const ov = $('#' + id);
  zModal += 1;                        // los modales apilados siempre abren encima
  ov.style.zIndex = zModal;
  ov.classList.add('open');
}
function cerrarModal(ov){
  ov.classList.remove('open');
  if (ov.id === 'modal-day') ui.dayKey = null;
}
function cerrarUltimo(){
  const abiertos = $$('.modal-overlay.open').filter(m => m.id !== 'modal-aviso');   // el aviso exige Aceptar
  if (abiertos.length) cerrarModal(abiertos[abiertos.length - 1]);
}

/* Selector de colores reutilizable */
function construirSwatches(cont, { auto = false, valor = '' } = {}){
  cont.innerHTML = '';
  let actual = (auto && !valor) ? '' : (valor || SWATCHES[0]);
  const marcar = () => $$('.swatch', cont).forEach(b => b.classList.toggle('selected', b.dataset.color === actual));
  if (auto){
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'swatch auto'; b.dataset.color = '';
    b.title = 'Usar el color de la categoría';
    b.onclick = () => { actual = ''; marcar(); };
    cont.appendChild(b);
  }
  for (const c of SWATCHES){
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'swatch'; b.dataset.color = c;
    b.style.background = c; b.title = c;
    b.onclick = () => { actual = c; marcar(); inp.value = c; };
    cont.appendChild(b);
  }
  const inp = document.createElement('input');
  inp.type = 'color'; inp.className = 'swatch-input';
  inp.value = actual || '#4f6df5'; inp.title = 'Color personalizado';
  inp.oninput = () => { actual = inp.value; marcar(); };
  cont.appendChild(inp);
  marcar();
  return { get: () => actual };
}

/* Modal nota */
let editandoNota = null, notePicker = null;
function abrirModalNota(nota = null, fechaPreset = null){
  editandoNota = nota;
  $('#noteModalTitle').textContent = nota ? 'Editar nota' : 'Nueva nota';
  $('#noteTitle').value = nota?.titulo || '';
  $('#noteDate').value  = nota?.fecha || fechaPreset || todayKey();
  setHora12(nota?.hora || '');
  $('#noteDesc').value  = nota?.desc || '';
  $('#noteRep').value   = nota?.repeticion || '';
  setAvisos(nota?.avisos || []);
  llenarSelectCats($('#noteCat'), nota?.catId);
  notePicker = construirSwatches($('#noteSwatches'), { auto: true, valor: nota?.color || '' });
  abrirModal('modal-note');
  setTimeout(() => $('#noteTitle').focus(), 80);
}
/* Selección múltiple de avisos en el formulario */
function setAvisos(arr){
  $$('#noteAvisos .aviso-chip').forEach(c => c.classList.toggle('selected', arr.includes(+c.dataset.min)));
}
function getAvisos(){
  return $$('#noteAvisos .aviso-chip.selected').map(c => +c.dataset.min).sort((a, b) => a - b);
}

function llenarSelectCats(sel, selId){
  const cats = yo().categorias;
  sel.innerHTML =
    cats.map(c => `<option value="${c.id}"${c.id === selId ? ' selected' : ''}>${esc(c.nombre)}</option>`).join('') +
    `<option value=""${cats.some(c => c.id === selId) ? '' : ' selected'}>Sin categoría</option>`;
}

/* Modal categoría */
let editandoCat = null, catPicker = null;
function abrirModalCat(cat = null){
  editandoCat = cat;
  $('#catModalTitle').textContent = cat ? 'Editar categoría' : 'Nueva categoría';
  $('#catName').value = cat?.nombre || '';
  catPicker = construirSwatches($('#catSwatches'), { valor: cat?.color || SWATCHES[5] });
  abrirModal('modal-cat');
  setTimeout(() => $('#catName').focus(), 80);
}

/* Modal usuario */
let editandoUser = null, userPicker = null, userEmoji = EMOJIS[0];
function abrirModalUser(u = null){
  editandoUser = u;
  userEmoji = u?.avatar || EMOJIS[0];
  $('#userModalTitle').textContent = u ? 'Editar usuario' : 'Nuevo usuario';
  $('#userName').value = u?.nombre || '';
  $('#emojiGrid').innerHTML = EMOJIS.map(e =>
    `<button type="button" class="emoji${e === userEmoji ? ' selected' : ''}" data-emoji="${e}">${e}</button>`).join('');
  userPicker = construirSwatches($('#userSwatches'), { valor: u?.color || SWATCHES[5] });
  abrirModal('modal-user');
  setTimeout(() => $('#userName').focus(), 80);
}

/* Modal día */
function abrirModalDia(key){
  ui.dayKey = key;
  pintarModalDia();
  abrirModal('modal-day');
}
function pintarModalDia(){
  if (!ui.dayKey) return;
  const festivo = nombreFestivo(ui.dayKey);
  const lista = notasQueOcurren(ui.dayKey)
    .filter(n => !ui.filterCat || n.catId === ui.filterCat)
    .sort((a, b) => (a.hora || '').localeCompare(b.hora || ''));
  $('#dayModalTitle').innerHTML =
    `${esc(fmtFechaLarga(ui.dayKey))}${festivo ? ` <span class="holiday-badge">🎉 ${esc(festivo)}</span>` : ''}`;
  $('#dayNotes').innerHTML = lista.length
    ? lista.map(n => notaCardHTML(n, { conFecha: false })).join('')
    : '<p class="empty-sm">Sin notas este día. ¡Agrega una! 📝</p>';
}

/* Modal confirmación */
let confirmarCb = null;
function confirmar(msg, cb, { titulo = 'Confirmar', ok = 'Eliminar', peligro = true } = {}){
  $('#confirmTitle').textContent = titulo;
  $('#confirmText').textContent = msg;
  const btn = $('#confirmOk');
  btn.textContent = ok;
  btn.className = 'btn ' + (peligro ? 'danger' : 'primary');
  confirmarCb = cb;
  abrirModal('modal-confirm');
}

/* Toast */
let toastTimer = null;
function toast(msg){
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 2300);
}

/* ─────────── 10. Acciones de datos ─────────── */
function guardarNota(e){
  e.preventDefault();
  const esEdicion = !!editandoNota;
  const datos = {
    titulo: $('#noteTitle').value.trim(),
    fecha:  $('#noteDate').value,
    hora:   getHora12(),
    desc:   $('#noteDesc').value.trim(),
    catId:  $('#noteCat').value,
    color:  notePicker.get(),
    repeticion: $('#noteRep').value,
    avisos: getAvisos(),
  };
  if (!datos.titulo || !datos.fecha) return;
  if (esEdicion) Object.assign(editandoNota, datos);
  else yo().notas.push({ id: uid('note'), done: false, creada: Date.now(), ...datos });
  editandoNota = null;
  guardar();
  cerrarModal($('#modal-note'));
  toast(esEdicion ? 'Nota actualizada ✏️' : 'Nota creada 📝');
  refrescarTrasCambio();
  // pedir permiso de notificaciones al guardar una nota con avisos
  if (datos.avisos.length && 'Notification' in window && Notification.permission === 'default'){
    Notification.requestPermission().then(p => {
      if (p === 'granted') toast('Notificaciones del navegador activadas 🔔');
    });
  }
}

function guardarCategoria(e){
  e.preventDefault();
  const nombre = $('#catName').value.trim();
  if (!nombre) return;
  const color = catPicker.get();
  if (editandoCat){
    editandoCat.nombre = nombre;
    editandoCat.color = color;
    toast('Categoría actualizada ✏️');
  } else {
    yo().categorias.push({ id: uid('cat'), nombre, color });
    toast('Categoría creada 🏷️');
  }
  editandoCat = null;
  guardar();
  cerrarModal($('#modal-cat'));
  refrescarTrasCambio();
}

function eliminarCategoria(id){
  const cat = yo().categorias.find(c => c.id === id);
  if (!cat) return;
  const afectadas = yo().notas.filter(n => n.catId === id).length;
  confirmar(
    `¿Eliminar la categoría "${cat.nombre}"?` +
    (afectadas ? ` Sus ${afectadas} nota(s) quedarán sin categoría.` : ''),
    () => {
      yo().notas.forEach(n => { if (n.catId === id) n.catId = ''; });
      yo().categorias = yo().categorias.filter(c => c.id !== id);
      if (ui.filterCat === id) ui.filterCat = null;
      guardar();
      refrescarTrasCambio();
      toast('Categoría eliminada 🗑️');
    });
}

function toggleNota(id){
  const n = yo().notas.find(x => x.id === id);
  if (!n) return;
  n.done = !n.done;
  guardar();
  refrescarTrasCambio();
}

function eliminarNota(id){
  confirmar('¿Eliminar esta nota? Esta acción no se puede deshacer.', () => {
    yo().notas = yo().notas.filter(n => n.id !== id);
    guardar();
    refrescarTrasCambio();
    toast('Nota eliminada 🗑️');
  });
}

function guardarUsuario(e){
  e.preventDefault();
  const nombre = $('#userName').value.trim();
  if (!nombre) return;
  if (editandoUser){
    editandoUser.nombre = nombre;
    editandoUser.avatar = userEmoji;
    editandoUser.color = userPicker.get();
    guardar();
    editandoUser = null;
    cerrarModal($('#modal-user'));
    render();
    toast('Usuario actualizado ✏️');
  } else {
    const u = nuevoUsuario(nombre);
    u.avatar = userEmoji;
    u.color = userPicker.get();
    u.tema = yo().tema;                       // arranca con el tema actual, luego cada uno es libre
    state.users.push(u);
    state.currentUserId = u.id;
    ui.filterCat = null;
    editandoUser = null;
    guardar();
    cerrarModal($('#modal-user'));
    aplicarTema();
    render();
    toast(`¡Hola, ${nombre}! 👋 Ahora tienes tu propio espacio`);
  }
}

function usarUsuario(id){
  if (id === state.currentUserId) return;
  state.currentUserId = id;
  ui.filterCat = null;
  guardar();
  aplicarTema();
  render();
  toast(`Sesión: ${yo().nombre} 👋`);
}

function eliminarUsuario(id){
  if (state.users.length <= 1){
    toast('Debe quedar al menos un usuario ⚠️');
    return;
  }
  const u = state.users.find(x => x.id === id);
  if (!u) return;
  confirmar(
    `¿Eliminar el usuario "${u.nombre}" junto con sus ${u.notas.length} nota(s) y ${u.categorias.length} categoría(s)? Esta acción no se puede deshacer.`,
    () => {
      state.users = state.users.filter(x => x.id !== id);
      if (state.currentUserId === id){
        state.currentUserId = state.users[0].id;
        ui.filterCat = null;
      }
      guardar();
      aplicarTema();
      render();
      toast('Usuario eliminado 🗑️');
    });
}

/* ─────────── 11. Exportar / importar ─────────── */
function exportar(){
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'calendario-colombia-respaldo.json';
  a.click();
  URL.revokeObjectURL(a.href);
  toast('Respaldo descargado 💾');
}
function importar(archivo){
  archivo.text().then(txt => {
    try{
      const data = JSON.parse(txt);
      if (!data || !Array.isArray(data.users) || !data.users.length) throw new Error('estructura no válida');
      confirmar(
        `¿Reemplazar TODOS los datos actuales por los del archivo (${data.users.length} usuario(s))?`,
        () => {
          state = data;
          if (!state.users.find(u => u.id === state.currentUserId)) state.currentUserId = state.users[0].id;
          ui.filterCat = null;
          guardar();
          aplicarTema();
          render();
          toast('Datos importados ✅');
        },
        { titulo: 'Importar respaldo', ok: 'Importar', peligro: false });
    } catch {
      toast('El archivo no es un respaldo válido ❌');
    }
  });
}

/* ─────────── 12. Delegación de acciones ─────────── */
function manejarAccion(e){
  const btn = e.target.closest('[data-action]');
  if (!btn) return;
  const acc = btn.dataset.action, id = btn.dataset.id;

  switch (acc){
    case 'filter':
      if (!id) ui.filterCat = null;
      else ui.filterCat = (ui.filterCat === id) ? null : id;
      renderVista();
      break;
    case 'new-cat':  abrirModalCat(); break;
    case 'edit-cat': abrirModalCat(yo().categorias.find(c => c.id === id)); break;
    case 'del-cat':  eliminarCategoria(id); break;
    case 'new-note': abrirModalNota(null, todayKey()); break;
    case 'edit-nota': abrirModalNota(yo().notas.find(n => n.id === id)); break;
    case 'del-nota':  eliminarNota(id); break;
    case 'toggle-nota': toggleNota(id); break;
    case 'new-user':  abrirModalUser(); break;
    case 'edit-user': abrirModalUser(state.users.find(u => u.id === id)); break;
    case 'use-user':  usarUsuario(id); break;
    case 'del-user':  eliminarUsuario(id); break;
    case 'set-tema':  definirTema(id); break;
  }
}

/* ─────────── 13. Inicialización ─────────── */
function inicializar(){
  $('#calWeekdays').innerHTML = DIAS_CORTOS.map(d => `<span>${d}</span>`).join('');
  $('#themeSelect').innerHTML = TEMAS.map(t => `<option value="${t.id}">${t.nombre}</option>`).join('');
  // selector de hora en formato 12 h
  $('#noteHora').innerHTML = '<option value="">— sin hora</option>' +
    Array.from({ length: 12 }, (_, i) => `<option value="${i + 1}">${i + 1}</option>`).join('');
  $('#noteMin').innerHTML  = Array.from({ length: 60 }, (_, i) => `<option value="${pad(i)}">${pad(i)}</option>`).join('');
  // chips de notificación (selección múltiple)
  $('#noteAvisos').innerHTML = AVISOS_DEF.map(a =>
    `<button type="button" class="aviso-chip" data-min="${a.min}" title="${a.label}">${a.corto}</button>`).join('');
  $('#noteAvisos').addEventListener('click', e => {
    const c = e.target.closest('.aviso-chip');
    if (c) c.classList.toggle('selected');
  });

  /* Navegación */
  $('#tabs').addEventListener('click', e => {
    const b = e.target.closest('.tab');
    if (!b) return;
    ui.vista = b.dataset.view;
    render();
  });
  $('#prevMonth').onclick = () => navegar(-1);
  $('#nextMonth').onclick = () => navegar(1);
  $('#todayBtn').onclick = () => {
    const d = new Date();
    ui.fecha = d; ui.anio = d.getFullYear(); ui.mes = d.getMonth();
    renderCalendario();
  };
  $('#vistaSeg').addEventListener('click', e => {
    const b = e.target.closest('button[data-vista]');
    if (!b) return;
    ui.vistaCal = b.dataset.vista;
    renderCalendario();
  });
  $('#userChip').onclick = () => { ui.vista = 'usuarios'; render(); };
  $('#themeSelect').onchange = e => definirTema(e.target.value);

  /* Calendario */
  $('#newNoteCalBtn').onclick = () =>
    abrirModalNota(null, ui.vistaCal === 'hoy' ? keyOf(ui.fecha) : todayKey());
  $('#calGrid').addEventListener('click', e => {
    const celda = e.target.closest('.day');
    if (celda) abrirModalDia(celda.dataset.key);
  });
  $('#vistaDia').addEventListener('click', e => {
    if (e.target.closest('#diaAddBtn')){ abrirModalNota(null, keyOf(ui.fecha)); return; }
    manejarAccion(e);
  });

  /* Delegación de listas */
  $('#sidebar').addEventListener('click', manejarAccion);
  $('#notesList').addEventListener('click', manejarAccion);
  $('#catChips').addEventListener('click', manejarAccion);
  $('#userCards').addEventListener('click', manejarAccion);
  $('#themeCards').addEventListener('click', manejarAccion);
  $('#dayNotes').addEventListener('click', manejarAccion);

  $('#emojiGrid').addEventListener('click', e => {
    const b = e.target.closest('.emoji');
    if (!b) return;
    userEmoji = b.dataset.emoji;
    $$('.emoji', $('#emojiGrid')).forEach(x => x.classList.toggle('selected', x === b));
  });

  /* Vista notas */
  $('#noteSearch').addEventListener('input', e => { ui.busqueda = e.target.value; renderNotas(); });
  $('#statusFilter').addEventListener('change', e => { ui.estado = e.target.value; renderNotas(); });
  $('#newNoteBtn').onclick = () => abrirModalNota(null, todayKey());
  $('#dayAddNote').onclick = () => abrirModalNota(null, ui.dayKey);

  /* Formularios */
  $('#noteForm').addEventListener('submit', guardarNota);
  $('#catForm').addEventListener('submit', guardarCategoria);
  $('#userForm').addEventListener('submit', guardarUsuario);
  $('#confirmOk').onclick = () => {
    const cb = confirmarCb;
    confirmarCb = null;
    cerrarModal($('#modal-confirm'));
    cb && cb();
  };

  /* Usuarios: crear, exportar, importar */
  $('#newUserBtn').onclick = () => abrirModalUser();
  $('#exportBtn').onclick = exportar;
  $('#importBtn').onclick = () => $('#importFile').click();
  $('#importFile').addEventListener('change', e => {
    if (e.target.files[0]) importar(e.target.files[0]);
    e.target.value = '';
  });

  /* Cierre de modales (la ventana de aviso solo se cierra con Aceptar) */
  $$('.modal-overlay').forEach(ov => {
    ov.addEventListener('mousedown', ev => { if (ev.target === ov && ov.id !== 'modal-aviso') cerrarModal(ov); });
    $$('[data-close]', ov).forEach(b => b.addEventListener('click', () => cerrarModal(ov)));
  });
  document.addEventListener('keydown', ev => { if (ev.key === 'Escape') cerrarUltimo(); });

  $('#avisoAceptar').addEventListener('click', mostrarSiguienteAviso);

  aplicarTema();
  guardar();   // persiste también el estado inicial la primera vez
  render();

  // verificador de notificaciones (mientras la página esté abierta)
  setTimeout(revisarAvisos, 3000);
  setInterval(revisarAvisos, 30000);
}

inicializar();
