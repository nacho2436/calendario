@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.jose.calendario

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/* ─────────── constantes y helpers ─────────── */

val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
    "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
val MESES_MIN = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
    "agosto", "septiembre", "octubre", "noviembre", "diciembre")
val DIAS_CORTOS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
val DIAS_LARGOS = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
val SWATCHES = listOf("#ef6c6c", "#f2913d", "#f5c542", "#66bb6a", "#31a3c3",
    "#4f6df5", "#8e6df5", "#d65cb0", "#8d99ae", "#5d4037")
val EMOJIS = listOf("🙂", "😎", "🤓", "🐱", "🐶", "🦊", "🐼", "🌸", "🌈", "⭐", "🌙",
    "🚀", "⚡", "🎨", "🎵", "⚽", "🎮", "📚", "💻", "🍕", "🎉", "🏆")

val LocalTema = androidx.compose.runtime.compositionLocalOf { TEMAS.first() }

object Ui {
    var pestana by mutableStateOf(0)
    var vistaCal by mutableStateOf("mes")            // "hoy" | "semana" | "mes"
    var fechaBase by mutableStateOf(LocalDate.now()) // fecha de las vistas día/semana
    var filtroCat by mutableStateOf<String?>(null)
    var busqueda by mutableStateOf("")
    var estado by mutableStateOf("todas")
}

fun hex(color: String): Color = Color(android.graphics.Color.parseColor(color))

fun colorDeNota(n: Nota): Color {
    if (n.color.isNotEmpty()) return hex(n.color)
    return Store.usuario.categorias.find { it.id == n.catId }?.color?.let { hex(it) }
        ?: Color(0xFF8D99AE)
}

fun notasQueOcurren(fecha: LocalDate): List<Nota> =
    Store.usuario.notas.filter { Repeticiones.ocurreEn(it, fecha) }

fun fechaLarga(f: String): String {
    val d = try { LocalDate.parse(f) } catch (e: Exception) { return f }
    val hoy = LocalDate.now()
    val base = "${DIAS_LARGOS[d.dayOfWeek.value % 7]}, ${d.dayOfMonth} de ${MESES_MIN[d.monthValue - 1]}"
    val anio = if (d.year != hoy.year) " de ${d.year}" else ""
    return when (d) {
        hoy -> "Hoy · $base$anio"
        hoy.plusDays(1) -> "Mañana · $base$anio"
        hoy.minusDays(1) -> "Ayer · $base$anio"
        else -> "$base$anio"
    }
}

/** Lunes de la semana de la fecha dada. */
fun inicioSemana(d: LocalDate): LocalDate = d.minusDays(((d.dayOfWeek.value + 6) % 7).toLong())

private data class Pestana(val titulo: String, val icono: ImageVector)

fun pedirPermisoNotificaciones(contexto: android.content.Context, lanzador: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(contexto, "android.permission.POST_NOTIFICATIONS") !=
        PackageManager.PERMISSION_GRANTED
    ) {
        lanzador.launch("android.permission.POST_NOTIFICATIONS")
    }
}

/* ─────────── raíz de la app ─────────── */

@Composable
fun Aplicacion() {
    val contexto = LocalContext.current
    val tema = LocalTema.current
    val sn = remember { SnackbarHostState() }

    LaunchedEffect(tema.id) {
        (contexto as? Activity)?.window?.statusBarColor = tema.scheme.primary.toArgb()
    }

    // motor de avisos: revisar cada 30 s mientras la app esté abierta
    LaunchedEffect(Unit) {
        MotorAvisos.iniciar(contexto)
        while (true) {
            MotorAvisos.revisar(contexto)
            delay(30_000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(sn) },
        bottomBar = {
            NavigationBar {
                val pestanas = listOf(
                    Pestana("Calendario", Icons.Filled.DateRange),
                    Pestana("Notas", Icons.Filled.Edit),
                    Pestana("Usuarios", Icons.Filled.Person),
                )
                pestanas.forEachIndexed { i, p ->
                    NavigationBarItem(
                        icon = { Icon(p.icono, contentDescription = p.titulo) },
                        label = { Text(p.titulo) },
                        selected = Ui.pestana == i,
                        onClick = { Ui.pestana = i },
                    )
                }
            }
        },
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
            when (Ui.pestana) {
                0 -> PantallaCalendario(sn)
                1 -> PantallaNotas(sn)
                else -> PantallaUsuarios()
            }
        }
    }

    // ventana central de aviso: hay que pulsar Aceptar
    if (MotorAvisos.cola.isNotEmpty()) {
        DialogoAviso(MotorAvisos.cola.first()) { MotorAvisos.aceptar() }
    }
}

@Composable
fun DialogoAviso(a: MotorAvisos.AvisoPendiente, onAceptar: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },   // solo se cierra con Aceptar
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🔔", fontSize = 44.sp)
                Spacer(Modifier.height(6.dp))
                Text(a.titulo, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    a.detalle, color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                if (a.desc.isNotEmpty()) {
                    Text(a.desc, fontSize = 13.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onAceptar, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("✔ Aceptar")
            }
        },
    )
}

/* ─────────── pantalla: calendario ─────────── */

@Composable
fun PantallaCalendario(sn: SnackbarHostState) {
    var notaEdit by remember { mutableStateOf<Nota?>(null) }
    var nuevaFecha by remember { mutableStateOf<String?>(null) }
    var diaAbierto by remember { mutableStateOf<String?>(null) }
    var catEdit by remember { mutableStateOf<Categoria?>(null) }
    var nuevaCat by remember { mutableStateOf(false) }
    var gestionCats by remember { mutableStateOf(false) }
    var eliminarRepetida by remember { mutableStateOf<Pair<Nota, String>?>(null) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val alc = rememberCoroutineScope()
    fun aviso(m: String) { alc.launch { sn.showSnackbar(m) } }
    fun pedirEliminarNota(n: Nota, f: String){
        if (n.repeticion.isNotEmpty()) eliminarRepetida = n to f
        else confirmar = "¿Eliminar la nota \"${n.titulo}\"?" to { Store.borrarNota(n.id); aviso("Nota eliminada 🗑️") }
    }

    val u = Store.usuario
    val mesBase = YearMonth.from(Ui.fechaBase)
    val enFechaActual = when (Ui.vistaCal) {
        "mes", "infografia" -> mesBase == YearMonth.now()
        else -> Ui.fechaBase == LocalDate.now()
    }

    fun navegar(delta: Int) {
        Ui.fechaBase = when (Ui.vistaCal) {
            "mes", "infografia" -> Ui.fechaBase.plusMonths(delta.toLong())
            "semana" -> Ui.fechaBase.plusWeeks(delta.toLong())
            else -> Ui.fechaBase.plusDays(delta.toLong())
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── título con flechas ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navegar(-1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowLeft, "Anterior")
            }
            Text(
                when (Ui.vistaCal) {
                    "mes", "infografia" -> "${MESES[mesBase.monthValue - 1]} ${mesBase.year}"
                    "semana" -> tituloSemana()
                    else -> fechaLarga(Ui.fechaBase.toString())
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            IconButton(onClick = { navegar(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowRight, "Siguiente")
            }
        }
        // ── selector de vista centrado + botón redondo de nueva nota ──
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
            SelectorVista(Modifier.align(Alignment.Center))
            if (!enFechaActual) {
                TextButton(
                    onClick = { Ui.fechaBase = LocalDate.now() },
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) { Text("Hoy", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            Button(
                onClick = {
                    nuevaFecha = if (Ui.vistaCal == "hoy") Ui.fechaBase.toString() else LocalDate.now().toString()
                },
                modifier = Modifier.align(Alignment.CenterEnd).size(38.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) { Icon(Icons.Filled.Add, contentDescription = "Nueva nota") }
        }
        // ── categorías (solo nombre, con punto de color) ──
        LazyRow(
            modifier = Modifier.padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                FilterChip(
                    selected = Ui.filtroCat == null,
                    onClick = { Ui.filtroCat = null },
                    label = { Text("Todas") },
                )
            }
            items(u.categorias) { c ->
                FilterChip(
                    selected = Ui.filtroCat == c.id,
                    onClick = { Ui.filtroCat = if (Ui.filtroCat == c.id) null else c.id },
                    label = { Text(c.nombre) },
                    leadingIcon = { Box(Modifier.size(8.dp).background(hex(c.color), CircleShape)) },
                )
            }
            item {
                FilterChip(selected = false, onClick = { gestionCats = true },
                    label = { Text("⚙️") })
            }
        }

        when (Ui.vistaCal) {
            "hoy" -> VistaDia(
                onNuevaNota = { nuevaFecha = Ui.fechaBase.toString() },
                onEditar = { notaEdit = it },
                onEliminar = { n, f -> pedirEliminarNota(n, f) },
                onAlternar = { id, f -> Store.alternarNota(id, f) },
            )
            "infografia" -> VistaInfografia(
                onNuevaNota = { nuevaFecha = LocalDate.now().toString() },
                onEditar = { notaEdit = it },
                onEliminar = { n ->
                    confirmar = "¿Eliminar la nota \"${n.titulo}\"?" to { Store.borrarNota(n.id); aviso("Nota eliminada 🗑️") }
                },
                onAlternar = { Store.alternarNota(it) },
            )
            else -> {
                // ── encabezado de días ──
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    DIAS_CORTOS.forEachIndexed { i, d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i == 6) LocalTema.current.festivo else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                val esMes = Ui.vistaCal == "mes"
                val fechas: List<LocalDate> = if (esMes) {
                    val primero = mesBase.atDay(1)
                    val offset = (primero.dayOfWeek.value + 6) % 7
                    (0..41).map { primero.minusDays((offset - it).toLong()) }
                } else {
                    val ini = inicioSemana(Ui.fechaBase)
                    (0..6).map { ini.plusDays(it.toLong()) }
                }
                // rejilla: de base las filas se reparten la pantalla, pero crecen
                // solas cuando un día tiene más notas de las que caben
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    val filaBase = maxOf(60.dp, (maxHeight - 20.dp) / 6)
                    val minFila = if (esMes) filaBase else maxHeight
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        fechas.chunked(7).forEach { semanaFechas ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = minFila)
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                semanaFechas.forEach { fecha ->
                                    val festivo = Festivos.nombre(fecha.toString())
                                    val notas = notasQueOcurren(fecha)
                                        .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
                                        .sortedBy { it.hora }
                                    CeldaDia(
                                        fecha = fecha,
                                        festivo = festivo,
                                        notas = notas,
                                        otroMes = esMes && fecha.month != mesBase.month,
                                        maxPildoras = if (esMes) 6 else 10,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    ) { diaAbierto = fecha.toString() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* diálogos */
    notaEdit?.let { n ->
        DialogoNota(nota = n, fechaInicial = n.fecha,
            onCerrar = { notaEdit = null },
            onGuardar = { nueva, con -> Store.guardarNota(nueva, n.id, con); notaEdit = null; aviso("Nota actualizada ✏️") })
    }
    nuevaFecha?.let { f ->
        DialogoNota(nota = null, fechaInicial = f,
            onCerrar = { nuevaFecha = null },
            onGuardar = { nueva, con -> Store.guardarNota(nueva, null, con); nuevaFecha = null
                aviso(if (con.isEmpty()) "Nota creada 📝" else "Nota creada y compartida 👥") })
    }
    diaAbierto?.let { key ->
        DialogoDia(
            fecha = key,
            onCerrar = { diaAbierto = null },
            onAgregar = { nuevaFecha = key; diaAbierto = null },
            onEditar = { notaEdit = it },
            onToggle = { id, f -> Store.alternarNota(id, f) },
            onEliminar = { n, f -> pedirEliminarNota(n, f) },
        )
    }
    if (gestionCats) {
        DialogoGestionCategorias(
            onCerrar = { gestionCats = false },
            onNueva = { gestionCats = false; nuevaCat = true },
            onEditar = { gestionCats = false; catEdit = it },
        )
    }
    if (nuevaCat || catEdit != null) {
        DialogoCategoria(
            cat = catEdit,
            onCerrar = { nuevaCat = false; catEdit = null },
            onGuardar = { c ->
                if (catEdit == null) { Store.crearCategoria(c); aviso("Categoría creada 🏷️") }
                else { Store.actualizarCategoria(c); aviso("Categoría actualizada ✏️") }
                nuevaCat = false; catEdit = null
            },
        )
    }
    eliminarRepetida?.let { (n, f) ->
        DialogoEliminarRepetida(nota = n,
            onCerrar = { eliminarRepetida = null },
            onSoloDia = { Store.excluirOcurrencia(n.id, f); eliminarRepetida = null; aviso("Ocurrencia eliminada 🗑️") },
            onSerie = { Store.borrarNota(n.id); eliminarRepetida = null; aviso("Serie eliminada 🗑️") })
    }
    confirmar?.let { (msg, accion) ->
        DialogoConfirmar(mensaje = msg, onSi = { accion(); confirmar = null }, onNo = { confirmar = null })
    }
}

fun tituloSemana(): String {
    val ini = inicioSemana(Ui.fechaBase)
    val fin = ini.plusDays(6)
    val abr = { m: Int -> MESES[m].toLowerCase().substring(0, 3) }
    return if (ini.month == fin.month)
        "${ini.dayOfMonth} – ${fin.dayOfMonth} de ${MESES_MIN[ini.monthValue - 1]} ${fin.year}"
    else
        "${ini.dayOfMonth} ${abr(ini.monthValue)} – ${fin.dayOfMonth} ${abr(fin.monthValue)} ${fin.year}"
}

@Composable
fun SelectorVista(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            listOf("hoy" to "Hoy", "semana" to "Semana", "mes" to "Mes", "infografia" to "Infografía").forEach { (id, label) ->
                val activo = Ui.vistaCal == id
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activo) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            if (activo) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(50))
                        .clickable { Ui.vistaCal = id }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** Píldora compacta de nota (título de color sobre fondo tenue). */
@Composable
fun PildoraNota(n: Nota, pequeña: Boolean, hecha: Boolean = false) {
    // letra blanca en negrita sobre el color sólido (ligeramente oscurecido
    // para que el blanco siempre tenga contraste, incluso en amarillos)
    val fondo = lerp(colorDeNota(n), Color.Black, 0.25f)
    Text(
        (if (n.repeticion.isNotEmpty()) "🔁 " else "") + n.titulo,
        // 2 líneas: en el celular la celda es angosta y con 1 línea no se lee el título
        fontSize = if (pequeña) 9.sp else 10.sp,
        lineHeight = if (pequeña) 11.sp else 13.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textDecoration = if (hecha) TextDecoration.LineThrough else null,
        modifier = Modifier
            .alpha(if (hecha) 0.55f else 1f)
            .background(fondo, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .fillMaxWidth(),
    )
}

@Composable
fun CeldaDia(
    fecha: LocalDate,
    festivo: String?,
    notas: List<Nota>,
    otroMes: Boolean,
    maxPildoras: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hoy = fecha == LocalDate.now()
    val domingo = fecha.dayOfWeek.value == 7
    val sabado = fecha.dayOfWeek.value == 6
    val rojo = LocalTema.current.festivo
    val surface = MaterialTheme.colorScheme.surface
    val fondo = when {
        festivo != null -> lerp(surface, rojo, 0.08f)
        domingo -> lerp(surface, rojo, 0.05f)
        sabado -> lerp(surface, MaterialTheme.colorScheme.outline, 0.05f)
        else -> surface
    }
    val borde = if (festivo != null) lerp(rojo, MaterialTheme.colorScheme.outlineVariant, 0.55f)
    else MaterialTheme.colorScheme.outlineVariant
    val pequeña = maxPildoras <= 6

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = fondo,
        border = BorderStroke(1.dp, borde),
        modifier = modifier.alpha(if (otroMes) 0.45f else 1f),
    ) {
        Column(Modifier.padding(3.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (hoy) {
                    Box(
                        modifier = Modifier.size(21.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${fecha.dayOfMonth}", color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "${fecha.dayOfMonth}",
                        color = if (festivo != null || domingo) rojo else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (festivo != null) {
                Text(
                    festivo,
                    fontSize = if (pequeña) 7.sp else 8.sp,
                    color = rojo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.weight(1f))
            notas.take(maxPildoras).forEach { PildoraNota(it, pequeña, notaHechaEn(it, fecha.toString())) }
            if (notas.size > maxPildoras) {
                Text(
                    "+${notas.size - maxPildoras} más",
                    fontSize = if (pequeña) 8.sp else 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Vista "Hoy": agenda completa del día seleccionado. */
@Composable
fun VistaDia(
    onNuevaNota: () -> Unit,
    onEditar: (Nota) -> Unit,
    onEliminar: (Nota, String) -> Unit,
    onAlternar: (String, String) -> Unit,
) {
    val festivo = Festivos.nombre(Ui.fechaBase.toString())
    val notas = notasQueOcurren(Ui.fechaBase)
        .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
        .sortedBy { it.hora }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(fechaLarga(Ui.fechaBase.toString()), fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium)
                    if (festivo != null) {
                        Text("🎉 $festivo", color = LocalTema.current.festivo,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${notas.size} nota${if (notas.size == 1) "" else "s"}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Button(onClick = onNuevaNota) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Nota")
                }
            }
        }
        if (notas.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sin notas este día. ¡Agrega una! 📝", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(notas, key = { it.id }) { n ->
                    TarjetaNota(
                        n = n, compacta = true, fecha = Ui.fechaBase.toString(),
                        onEditar = { onEditar(n) },
                        onEliminar = { onEliminar(n, Ui.fechaBase.toString()) },
                        onAlternar = { onAlternar(n.id, Ui.fechaBase.toString()) },
                    )
                }
            }
        }
    }
}

/** Vista "Infografía": todas las notas del mes como línea de tiempo. */
@Composable
fun VistaInfografia(
    onNuevaNota: () -> Unit,
    onEditar: (Nota) -> Unit,
    onEliminar: (Nota) -> Unit,
    onAlternar: (String) -> Unit,
) {
    val mes = YearMonth.from(Ui.fechaBase)
    val rojo = LocalTema.current.festivo
    val dias = (1..mes.lengthOfMonth()).map { mes.atDay(it) }
        .map { d -> d to notasQueOcurren(d)
            .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
            .sortedBy { it.hora } }
        .filter { it.second.isNotEmpty() }
    val festivosMes = (1..mes.lengthOfMonth()).count { Festivos.nombre(mes.atDay(it).toString()) != null }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // resumen del mes
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                dias.sumOf { it.second.size }.toString() to "notas",
                dias.size.toString() to "días con notas",
                festivosMes.toString() to "festivos",
            ).forEach { (num, txt) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(num, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text(txt, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        if (dias.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin notas en este mes 📝", color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onNuevaNota) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Nota")
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(top = 10.dp)) {
                items(dias.size, key = { dias[it].first.toString() }) { i ->
                    val (fecha, notas) = dias[i]
                    val festivo = Festivos.nombre(fecha.toString())
                    val hoy = fecha == LocalDate.now()
                    val domingo = fecha.dayOfWeek.value == 7
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // eje: círculo del día + línea conectora
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(48.dp).fillMaxHeight(),
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp)
                                    .background(
                                        when {
                                            festivo != null -> rojo
                                            domingo -> lerp(rojo, MaterialTheme.colorScheme.surface, 0.35f)
                                            else -> MaterialTheme.colorScheme.primary
                                        }, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${fecha.dayOfMonth}", color = Color.White,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            if (i < dias.size - 1) {
                                Box(
                                    modifier = Modifier.fillMaxHeight().padding(top = 3.dp)
                                        .width(3.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            RoundedCornerShape(2.dp)),
                                )
                            }
                        }
                        // contenido: fecha + notas
                        Column(Modifier.weight(1f).padding(start = 10.dp, top = 2.dp, bottom = 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fechaLarga(fecha.toString()),
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f, fill = false))
                                if (hoy) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("HOY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            if (festivo != null) {
                                Text("🎉 $festivo", color = rojo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            notas.forEach { n -> FilaInfografia(n, fecha.toString()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilaInfografia(n: Nota, fecha: String = "") {
    val c = colorDeNota(n)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, c.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(9.dp).background(c, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(
                (if (n.repeticion.isNotEmpty()) "🔁 " else "") + n.titulo,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (notaHechaEn(n, fecha)) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            val meta = buildString {
                if (n.hora.isNotEmpty()) append("⏰ ${fmtHora(n.hora)}")
                if (n.repeticion.isNotEmpty()) { if (isNotEmpty()) append(" "); append("🔁") }
                if (n.avisos.isNotEmpty()) { if (isNotEmpty()) append(" "); append("🔔") }
            }
            if (meta.isNotEmpty()) {
                Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ─────────── pantalla: notas ─────────── */

@Composable
fun PantallaNotas(sn: SnackbarHostState) {
    var notaEdit by remember { mutableStateOf<Nota?>(null) }
    var nueva by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var eliminarRepetida by remember { mutableStateOf<Pair<Nota, String>?>(null) }
    val alc = rememberCoroutineScope()
    fun aviso(m: String) { alc.launch { sn.showSnackbar(m) } }

    val u = Store.usuario
    val q = Ui.busqueda.trim().lowercase()
    val notas = u.notas
        .filter { (Ui.estado == "todas") || ((Ui.estado == "hechas") == it.done) }
        .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
        .filter { q.isEmpty() || (it.titulo + " " + it.desc).lowercase().contains(q) }
        .sortedWith(compareBy({ Repeticiones.proximaFecha(it).toString() }, { it.hora }))

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = Ui.busqueda,
            onValueChange = { Ui.busqueda = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text("Buscar notas...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { FilterChip(selected = Ui.filtroCat == null, onClick = { Ui.filtroCat = null }, label = { Text("Todas") }) }
            items(u.categorias) { c ->
                FilterChip(
                    selected = Ui.filtroCat == c.id,
                    onClick = { Ui.filtroCat = if (Ui.filtroCat == c.id) null else c.id },
                    label = { Text(c.nombre) },
                    leadingIcon = { Box(Modifier.size(10.dp).background(hex(c.color), CircleShape)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("todas" to "Todas", "pendientes" to "Pendientes", "hechas" to "Completadas").forEach { (v, t) ->
                FilterChip(selected = Ui.estado == v, onClick = { Ui.estado = v }, label = { Text(t) })
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { nueva = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Nueva")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            if (notas.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🍂", fontSize = 32.sp)
                        Text("No hay notas para mostrar", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            notas.groupBy { Repeticiones.proximaFecha(it).toString() }.forEach { (fecha, lista) ->
                item(key = "h$fecha") {
                    Text(
                        fechaLarga(fecha),
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(lista, key = { it.id }) { n ->
                    val fechaN = Repeticiones.proximaFecha(n).toString()
                    TarjetaNota(
                        n = n, fecha = fechaN,
                        onEditar = { notaEdit = n },
                        onEliminar = {
                            if (n.repeticion.isNotEmpty()) eliminarRepetida = n to fechaN
                            else confirmar = "¿Eliminar la nota \"${n.titulo}\"?" to {
                                Store.borrarNota(n.id); aviso("Nota eliminada 🗑️")
                            }
                        },
                        onAlternar = { Store.alternarNota(n.id, fechaN) },
                    )
                }
            }
        }
    }

    notaEdit?.let { n ->
        DialogoNota(nota = n, fechaInicial = n.fecha,
            onCerrar = { notaEdit = null },
            onGuardar = { nueva, con -> Store.guardarNota(nueva, n.id, con); notaEdit = null; aviso("Nota actualizada ✏️") })
    }
    if (nueva) {
        DialogoNota(nota = null, fechaInicial = LocalDate.now().toString(),
            onCerrar = { nueva = false },
            onGuardar = { n2, con -> Store.guardarNota(n2, null, con); nueva = false
                aviso(if (con.isEmpty()) "Nota creada 📝" else "Nota creada y compartida 👥") })
    }
    eliminarRepetida?.let { (n, f) ->
        DialogoEliminarRepetida(nota = n,
            onCerrar = { eliminarRepetida = null },
            onSoloDia = { Store.excluirOcurrencia(n.id, f); eliminarRepetida = null; aviso("Ocurrencia eliminada 🗑️") },
            onSerie = { Store.borrarNota(n.id); eliminarRepetida = null; aviso("Serie eliminada 🗑️") })
    }
    confirmar?.let { (msg, accion) ->
        DialogoConfirmar(mensaje = msg, onSi = { accion(); confirmar = null }, onNo = { confirmar = null })
    }
}

@Composable
fun DialogoEliminarRepetida(nota: Nota, onCerrar: () -> Unit, onSoloDia: () -> Unit, onSerie: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nota repetitiva") },
        text = { Text("\"${nota.titulo}\" se repite. ¿Qué quieres eliminar?") },
        confirmButton = { TextButton(onClick = onSoloDia) { Text("Solo este día") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSerie) { Text("Toda la serie", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}

@Composable
fun TarjetaNota(n: Nota, compacta: Boolean = false, fecha: String = "", onEditar: () -> Unit, onEliminar: () -> Unit, onAlternar: () -> Unit) {
    val f = if (fecha.isEmpty()) n.fecha else fecha
    val hecha = notaHechaEn(n, f)
    val cat = Store.usuario.categorias.find { it.id == n.catId }
    val colorCat = cat?.let { hex(it.color) } ?: Color(0xFF8D99AE)
    val colorNota = colorDeNota(n)
    val ok = Color(0xFF2FB344)

    val meta = buildString {
        if (!compacta) append("📅 ${fechaLarga(Repeticiones.proximaFecha(n).toString())}")
        if (n.hora.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("⏰ ${fmtHora(n.hora)}") }
        if (n.repeticion.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("🔁 ${Repeticiones.CORTO[n.repeticion] ?: ""}") }
        if (n.avisos.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("🔔 ${n.avisos.joinToString(", ") { AvisosDef.corto(it) }} antes") }
        if (n.origenId.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("👥 compartida") }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(colorNota))
            Row(
                modifier = Modifier.weight(1f).padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(26.dp)
                        .background(if (hecha) ok else Color.Transparent, CircleShape)
                        .border(2.dp, if (hecha) ok else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onAlternar() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (hecha) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            n.titulo,
                            modifier = Modifier.weight(1f, fill = false),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (hecha) TextDecoration.LineThrough else null,
                            color = if (hecha) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cat?.nombre ?: "Sin categoría",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorCat,
                            modifier = Modifier
                                .background(colorCat.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    if (meta.isNotEmpty()) {
                        Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    if (n.desc.isNotEmpty()) {
                        Text(
                            n.desc, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compacta) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Filled.Edit, "Editar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/* ─────────── pantalla: usuarios ─────────── */

@Composable
fun PantallaUsuarios() {
    var editUser by remember { mutableStateOf<Usuario?>(null) }
    var nuevo by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Usuarios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Cada usuario tiene sus propias notas, categorías y tema.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Button(onClick = { nuevo = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Nuevo")
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Store.app.users.forEach { usr ->
                val activo = usr.id == Store.app.currentUserId
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (activo) 2.dp else 1.dp,
                        if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(60.dp)
                                .background(lerp(hex(usr.color), MaterialTheme.colorScheme.surface, 0.82f), CircleShape)
                                .border(2.dp, hex(usr.color), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text(usr.avatar, fontSize = 26.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(usr.nombre, fontWeight = FontWeight.Bold)
                        Text(
                            "${usr.notas.size} notas · ${usr.categorias.size} categorías",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!activo) {
                                Button(
                                    onClick = { Store.cambiarUsuario(usr.id); Ui.filtroCat = null },
                                    modifier = Modifier.height(36.dp),
                                ) { Text("Usar", fontSize = 12.sp) }
                            } else {
                                Text("✓ En uso", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp))
                            }
                            OutlinedButton(onClick = { editUser = usr }, modifier = Modifier.height(36.dp)) {
                                Text("Editar", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    confirmar = "¿Eliminar el usuario \"${usr.nombre}\" con sus ${usr.notas.size} nota(s)? Esta acción no se puede deshacer." to {
                                        Store.borrarUsuario(usr.id); Ui.filtroCat = null
                                    }
                                },
                                modifier = Modifier.height(36.dp),
                            ) { Text("Eliminar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("🎨 Apariencia — tema del usuario actual",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TEMAS.forEach { t ->
                val activo = t.id == Store.usuario.tema
                Surface(
                    onClick = { Store.definirTema(t.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (activo) 2.dp else 1.dp,
                        if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            t.muestra.forEach { Box(Modifier.size(13.dp).background(it, RoundedCornerShape(4.dp))) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.nombre + if (activo) " ✓" else "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("🔄 Sincronización con el servidor",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Conecta esta app con el servidor de tu PC (misma red WiFi) para compartir las notas. En la web se sincroniza solo; aquí usa el botón.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        SeccionSincronizacion()
        Spacer(Modifier.height(20.dp))
    }

    if (nuevo || editUser != null) {
        DialogoUsuario(
            u = editUser,
            onCerrar = { nuevo = false; editUser = null },
            onGuardar = { usr ->
                if (editUser == null) {
                    Store.crearUsuario(usr)
                    Ui.filtroCat = null
                } else {
                    Store.actualizarUsuario(usr)
                }
                nuevo = false; editUser = null
            },
        )
    }
    confirmar?.let { (msg, accion) ->
        DialogoConfirmar(mensaje = msg, onSi = { accion(); confirmar = null }, onNo = { confirmar = null })
    }
}

@Composable
fun SeccionSincronizacion() {
    val contexto = LocalContext.current
    val prefs = remember { contexto.getSharedPreferences("calendario_colombia", android.content.Context.MODE_PRIVATE) }
    var urlManual by remember { mutableStateOf(prefs.getString("servidor", "") ?: "") }
    var estado by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }
    var encontrados by remember { mutableStateOf(listOf<Sincronizacion.ServidorEncontrado>()) }
    var elegido by remember { mutableStateOf<Sincronizacion.ServidorEncontrado?>(null) }
    var codigo by remember { mutableStateOf("") }
    val alcance = rememberCoroutineScope()
    val guardado = prefs.getString("servidor", null)

    Column {
        if (guardado != null) {
            Text("✔ Vinculado a: $guardado", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        // 1) buscar el servidor en la red
        Button(
            onClick = {
                buscando = true; estado = "🔎 Buscando servidores en la red…"
                encontrados = emptyList(); elegido = null
                alcance.launch {
                    val lista = withContext(Dispatchers.IO) { Sincronizacion.buscarServidores() }
                    encontrados = lista
                    buscando = false
                    estado = when {
                        lista.isEmpty() -> "No se encontró nada · ¿PC y celular en la misma red WiFi y el servidor encendido? (./menu.sh)"
                        else -> "Elige tu servidor y escribe el código que muestra la web"
                    }
                }
            },
            enabled = !buscando && !ocupado,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(if (buscando) " Buscando…" else " Buscar servidor en la red")
        }

        encontrados.forEach { s ->
            val activo = elegido?.id == s.id
            Surface(
                onClick = { elegido = s },
                shape = RoundedCornerShape(10.dp),
                color = if (activo) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (activo) 2.dp else 1.dp,
                    if (activo) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp),
                        tint = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(s.etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(s.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // 2) código de vinculación
        if (elegido != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = codigo,
                onValueChange = { if (it.length <= 6) codigo = it.filter { c -> c.isDigit() } },
                label = { Text("Código de 6 dígitos (lo muestra la web)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val servidor = elegido
                    if (servidor == null || codigo.length < 6) {
                        estado = "Escribe el código completo que muestra la web"
                        return@Button
                    }
                    ocupado = true; estado = "Vinculando…"
                    alcance.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                Sincronizacion.vincular(servidor.url, codigo, android.os.Build.MODEL ?: "Android")
                            }
                            val respuesta = withContext(Dispatchers.IO) {
                                Sincronizacion.httpPost("${servidor.url}/api/sync", Store.exportarUsuariosJson())
                            }
                            val cambios = Store.importarYFusionar(respuesta)
                            prefs.edit().putString("servidor", servidor.url).apply()
                            estado = "✔ Vinculado a ${servidor.nombre} · sincronizado ($cambios cambio/s)"
                            codigo = ""
                        } catch (e: Exception) {
                            estado = if (e.message?.contains("403") == true || e.message?.contains("código") == true)
                                "✘ Código incorrecto o expirado · pide uno nuevo en la web"
                            else "✘ ${e.message ?: "error de conexión"}"
                        }
                        ocupado = false
                    }
                },
                enabled = !ocupado,
            ) { Text("🔗 Vincular y sincronizar") }
        }

        // 3) sincronizar de nuevo con lo ya vinculado
        if (guardado != null) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    ocupado = true; estado = "Sincronizando…"
                    alcance.launch {
                        try {
                            val respuesta = withContext(Dispatchers.IO) {
                                Sincronizacion.httpPost("$guardado/api/sync", Store.exportarUsuariosJson())
                            }
                            val cambios = Store.importarYFusionar(respuesta)
                            estado = if (cambios > 0) "✔ Sincronizado ($cambios cambio/s)" else "✔ Sincronizado (sin cambios)"
                        } catch (e: Exception) {
                            estado = "✘ ${e.message ?: "no se pudo conectar"}"
                        }
                        ocupado = false
                    }
                },
                enabled = !ocupado,
            ) { Text("↻ Sincronizar ahora") }
        }

        if (estado.isNotEmpty()) {
            Text(estado, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
        }

        // alternativa manual
        Spacer(Modifier.height(10.dp))
        Text("— o escribe la dirección manualmente —", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = urlManual,
            onValueChange = { urlManual = it },
            label = { Text("http://IP-del-PC:8177") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/* ─────────── diálogos ─────────── */

@Composable
fun DialogoNota(nota: Nota?, fechaInicial: String, onCerrar: () -> Unit, onGuardar: (Nota, List<String>) -> Unit) {
    val contexto = LocalContext.current
    val cats = Store.usuario.categorias
    var titulo by remember { mutableStateOf(nota?.titulo ?: "") }
    var fecha by remember { mutableStateOf(fechaInicial) }
    var hora by remember { mutableStateOf(nota?.hora ?: "") }
    var desc by remember { mutableStateOf(nota?.desc ?: "") }
    var catId by remember {
        mutableStateOf(
            if (nota != null && cats.any { it.id == nota.catId }) nota.catId
            else (cats.firstOrNull()?.id ?: "")
        )
    }
    var color by remember { mutableStateOf(nota?.color ?: "") }
    var repeticion by remember { mutableStateOf(nota?.repeticion ?: "") }
    var avisos by remember { mutableStateOf(nota?.avisos ?: emptyList<Int>()) }
    var mostrarCal by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    var menuCats by remember { mutableStateOf(false) }
    var menuRep by remember { mutableStateOf(false) }
    val otros = Store.app.users.filter { it.id != Store.app.currentUserId }
    var compartidos by remember { mutableStateOf(emptyList<String>()) }
    val permisoNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (nota == null) "Nueva nota" else "Editar nota") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mostrarCal = true }, modifier = Modifier.weight(1f)) {
                        Text(fechaLarga(fecha), maxLines = 1, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { mostrarHora = true }, modifier = Modifier.weight(1f)) {
                        Text(if (hora.isEmpty()) "⏰ Sin hora" else "⏰ ${fmtHora(hora)}", maxLines = 1, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = menuCats, onExpandedChange = { menuCats = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = cats.find { it.id == catId }?.nombre ?: "Sin categoría",
                            onValueChange = {},
                            readOnly = true, label = { Text("Categoría") },
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, "Categoría") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        DropdownMenu(expanded = menuCats, onDismissRequest = { menuCats = false }) {
                            cats.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.nombre) },
                                    leadingIcon = { Box(Modifier.size(10.dp).background(hex(c.color), CircleShape)) },
                                    onClick = { catId = c.id; menuCats = false },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Sin categoría") },
                                onClick = { catId = ""; menuCats = false },
                            )
                        }
                    }
                    ExposedDropdownMenuBox(expanded = menuRep, onExpandedChange = { menuRep = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = Repeticiones.OPCIONES.firstOrNull { it.first == repeticion }?.second ?: "No repetir",
                            onValueChange = {},
                            readOnly = true, label = { Text("Repetir") },
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, "Repetir") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        DropdownMenu(expanded = menuRep, onDismissRequest = { menuRep = false }) {
                            Repeticiones.OPCIONES.forEach { (v, t) ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = { repeticion = v; menuRep = false },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Color de la nota", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FilaSwatches(conAuto = true, valor = color) { color = it }
                Spacer(Modifier.height(10.dp))
                Text("Notificarme antes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvisosDef.OPCIONES.forEach { (min, etiqueta) ->
                        FilterChip(
                            selected = avisos.contains(min),
                            onClick = {
                                avisos = if (avisos.contains(min)) avisos - min else (avisos + min).sorted()
                            },
                            label = { Text(etiqueta) },
                        )
                    }
                }
                if (nota == null && otros.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Compartir con (opcional)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Aparece también en su calendario; cada uno la tacha o elimina por separado.",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        otros.forEach { u ->
                            FilterChip(
                                selected = compartidos.contains(u.id),
                                onClick = {
                                    compartidos = if (compartidos.contains(u.id)) compartidos - u.id else compartidos + u.id
                                },
                                label = { Text("${u.avatar} ${u.nombre}") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titulo.isNotBlank() && fecha.isNotEmpty()) {
                    onGuardar(Nota(
                        id = nota?.id ?: UUID.randomUUID().toString(),
                        titulo = titulo.trim(), desc = desc.trim(), fecha = fecha, hora = hora,
                        catId = catId, color = color, repeticion = repeticion, avisos = avisos,
                        done = nota?.done ?: false,
                        creada = nota?.creada ?: System.currentTimeMillis(),
                        origenId = nota?.origenId ?: "",
                    ), if (nota == null) compartidos else emptyList())
                    if (avisos.isNotEmpty()) pedirPermisoNotificaciones(contexto, permisoNotif)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )

    if (mostrarCal) {
        DialogoFecha(
            fecha,
            onListo = { ms -> fecha = LocalDate.ofEpochDay(ms / 86400000L).toString(); mostrarCal = false },
            onCerrar = { mostrarCal = false },
        )
    }
    if (mostrarHora) {
        DialogoHora(
            hora,
            onListo = { h, m -> hora = "%02d:%02d".format(h, m); mostrarHora = false },
            onCerrar = { mostrarHora = false },
        )
    }
}

@Composable
fun DialogoFecha(actual: String, onListo: (Long) -> Unit, onCerrar: () -> Unit) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(actual).toEpochDay() * 86400000L
        } catch (e: Exception) { System.currentTimeMillis() },
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = { onListo(estado.selectedDateMillis ?: 0L) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) { DatePicker(state = estado) }
}

@Composable
fun DialogoHora(actual: String, onListo: (Int, Int) -> Unit, onCerrar: () -> Unit) {
    val partes = actual.split(":")
    val estado = rememberTimePickerState(
        initialHour = partes.getOrNull(0)?.toIntOrNull() ?: 9,
        initialMinute = partes.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = false,   // formato 12 h con am/pm
    )
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Hora del recordatorio") },
        text = { TimePicker(state = estado) },
        confirmButton = { TextButton(onClick = { onListo(estado.hour, estado.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
fun FilaSwatches(conAuto: Boolean, valor: String, onElegir: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (conAuto) {
            Box(
                modifier = Modifier.size(30.dp)
                    .background(
                        Brush.sweepGradient(listOf(
                            Color(0xFFEF6C6C), Color(0xFFF5C542), Color(0xFF66BB6A),
                            Color(0xFF4F6DF5), Color(0xFF8E6DF5), Color(0xFFD65CB0), Color(0xFFEF6C6C))),
                        CircleShape)
                    .border(
                        if (valor.isEmpty()) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        CircleShape)
                    .clickable { onElegir("") },
            )
        }
        SWATCHES.forEach { c ->
            Box(
                modifier = Modifier.size(30.dp)
                    .background(hex(c), CircleShape)
                    .border(
                        if (valor == c) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        CircleShape)
                    .clickable { onElegir(c) },
            )
        }
    }
}

@Composable
fun DialogoGestionCategorias(
    onCerrar: () -> Unit,
    onNueva: () -> Unit,
    onEditar: (Categoria) -> Unit,
) {
    var confirmando by remember { mutableStateOf<Categoria?>(null) }
    val cats = Store.usuario.categorias
    val notas = Store.usuario.notas
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Categorías") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                cats.forEachIndexed { i, c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Box(Modifier.size(12.dp).background(hex(c.color), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${notas.count { it.catId == c.id }} notas",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { Store.moverCategoria(c.id, -1) },
                            enabled = i > 0, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Subir", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { Store.moverCategoria(c.id, +1) },
                            enabled = i < cats.size - 1, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Bajar", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onEditar(c) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Edit, "Editar", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { confirmando = c }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Delete, "Eliminar", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNueva) { Text("＋ Nueva") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
    confirmando?.let { c ->
        DialogoConfirmar(
            mensaje = "¿Eliminar la categoría \"${c.nombre}\"? Sus ${notas.count { it.catId == c.id }} nota(s) quedarán sin categoría.",
            onSi = { Store.borrarCategoria(c.id); confirmando = null },
            onNo = { confirmando = null },
        )
    }
}

@Composable
fun DialogoCategoria(cat: Categoria?, onCerrar: () -> Unit, onGuardar: (Categoria) -> Unit) {
    var nombre by remember { mutableStateOf(cat?.nombre ?: "") }
    var color by remember { mutableStateOf(cat?.color ?: SWATCHES[5]) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (cat == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text("Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FilaSwatches(conAuto = false, valor = color) { color = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    onGuardar(Categoria(cat?.id ?: UUID.randomUUID().toString(), nombre.trim(), color))
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoUsuario(u: Usuario?, onCerrar: () -> Unit, onGuardar: (Usuario) -> Unit) {
    var nombre by remember { mutableStateOf(u?.nombre ?: "") }
    var avatar by remember { mutableStateOf(u?.avatar ?: EMOJIS[0]) }
    var color by remember { mutableStateOf(u?.color ?: SWATCHES[5]) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (u == null) "Nuevo usuario" else "Editar usuario") },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text("Avatar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EMOJIS.forEach { e ->
                        Text(
                            e, fontSize = 22.sp,
                            modifier = Modifier
                                .background(
                                    if (e == avatar) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp))
                                .border(
                                    if (e == avatar) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    else BorderStroke(1.dp, Color.Transparent),
                                    RoundedCornerShape(8.dp))
                                .clickable { avatar = e }
                                .padding(6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Color del avatar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FilaSwatches(conAuto = false, valor = color) { color = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    if (u == null) onGuardar(Store.nuevoUsuario(nombre.trim(), avatar, color))
                    else onGuardar(u.copy(nombre = nombre.trim(), avatar = avatar, color = color))
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoDia(
    fecha: String,
    onCerrar: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Nota) -> Unit,
    onToggle: (String, String) -> Unit,
    onEliminar: (Nota, String) -> Unit,
) {
    val festivo = Festivos.nombre(fecha)
    val notas = notasQueOcurren(LocalDate.parse(fecha))
        .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
        .sortedBy { it.hora }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Column {
                Text(fechaLarga(fecha), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (festivo != null) {
                    Text("🎉 $festivo", color = LocalTema.current.festivo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                if (notas.isEmpty()) {
                    Text("Sin notas este día. ¡Agrega una! 📝",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    notas.forEach { n ->
                        TarjetaNota(
                            n = n, compacta = true, fecha = fecha,
                            onEditar = { onEditar(n) },
                            onEliminar = { onEliminar(n, fecha) },
                            onAlternar = { onToggle(n.id, fecha) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAgregar) { Text("＋ Agregar nota") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
fun DialogoConfirmar(
    mensaje: String,
    onSi: () -> Unit,
    onNo: () -> Unit,
    titulo: String = "Confirmar",
    textoOk: String = "Eliminar",
) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onSi) { Text(textoOk, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onNo) { Text("Cancelar") } },
    )
}
