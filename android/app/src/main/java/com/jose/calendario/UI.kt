@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.jose.calendario

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

val LocalTema = compositionLocalOf { TEMAS.first() }

object Ui {
    var pestana by mutableStateOf(0)
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

private data class Pestana(val titulo: String, val icono: ImageVector)

/* ─────────── raíz de la app ─────────── */

@Composable
fun Aplicacion() {
    val contexto = LocalContext.current
    val tema = LocalTema.current
    val sn = remember { SnackbarHostState() }

    LaunchedEffect(tema.id) {
        (contexto as? Activity)?.window?.statusBarColor = tema.scheme.primary.toArgb()
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
}

/* ─────────── pantalla: calendario ─────────── */

@Composable
fun PantallaCalendario(sn: SnackbarHostState) {
    var mes by remember { mutableStateOf(YearMonth.now()) }
    var notaEdit by remember { mutableStateOf<Nota?>(null) }
    var nuevaFecha by remember { mutableStateOf<String?>(null) }
    var diaAbierto by remember { mutableStateOf<String?>(null) }
    var catEdit by remember { mutableStateOf<Categoria?>(null) }
    var nuevaCat by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val alc = rememberCoroutineScope()
    fun aviso(m: String) { alc.launch { sn.showSnackbar(m) } }

    val u = Store.usuario

    Column(Modifier.fillMaxSize()) {
        // título del mes
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { mes = mes.minusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowLeft, "Mes anterior") }
            Text(
                "${MESES[mes.monthValue - 1]} ${mes.year}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            IconButton(onClick = { mes = mes.plusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowRight, "Mes siguiente") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { mes = YearMonth.now() }, modifier = Modifier.weight(1f)) { Text("Hoy") }
            Button(onClick = { nuevaFecha = LocalDate.now().toString() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Nota")
            }
        }
        // categorías (filtro)
        LazyRow(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                FilterChip(
                    selected = Ui.filtroCat == null,
                    onClick = { Ui.filtroCat = null },
                    label = { Text("Todas ${u.notas.size}") },
                )
            }
            items(u.categorias) { c ->
                FilterChip(
                    selected = Ui.filtroCat == c.id,
                    onClick = { Ui.filtroCat = if (Ui.filtroCat == c.id) null else c.id },
                    label = { Text("${c.nombre} ${u.notas.count { it.catId == c.id }}") },
                    leadingIcon = { Box(Modifier.size(10.dp).background(hex(c.color), CircleShape)) },
                )
            }
            item {
                FilterChip(selected = false, onClick = { nuevaCat = true }, label = { Text("＋ Categoría") })
            }
        }
        // encabezado de días
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
        // rejilla
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        ) {
            val offset = (mes.atDay(1).dayOfWeek.value + 6) % 7
            items(42) { i ->
                val fecha = mes.atDay(1).minusDays((offset - i).toLong())
                val festivo = Festivos.nombre(fecha.toString())
                val notas = u.notas
                    .filter { it.fecha == fecha.toString() && (Ui.filtroCat == null || it.catId == Ui.filtroCat) }
                    .sortedBy { it.hora }
                CeldaDia(fecha, festivo, notas, otroMes = fecha.month != mes.month) { diaAbierto = fecha.toString() }
            }
        }
        // leyenda
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(LocalTema.current.festivo, RoundedCornerShape(3.dp)))
                Text("Festivo", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(lerp(LocalTema.current.festivo, MaterialTheme.colorScheme.surface, 0.55f), RoundedCornerShape(3.dp)))
                Text("Domingo", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).background(Color(0xFF4F6DF5), CircleShape))
                Box(Modifier.size(8.dp).background(Color(0xFFF2913D), CircleShape))
                Box(Modifier.size(8.dp).background(Color(0xFF66BB6A), CircleShape))
                Text("Notas", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }

    /* diálogos */
    notaEdit?.let { n ->
        DialogoNota(nota = n, fechaInicial = n.fecha,
            onCerrar = { notaEdit = null },
            onGuardar = { nueva -> Store.guardarNota(nueva, n.id); notaEdit = null; aviso("Nota actualizada ✏️") })
    }
    nuevaFecha?.let { f ->
        DialogoNota(nota = null, fechaInicial = f,
            onCerrar = { nuevaFecha = null },
            onGuardar = { nueva -> Store.guardarNota(nueva, null); nuevaFecha = null; aviso("Nota creada 📝") })
    }
    diaAbierto?.let { key ->
        DialogoDia(
            fecha = key,
            onCerrar = { diaAbierto = null },
            onAgregar = { nuevaFecha = key; diaAbierto = null },
            onEditar = { notaEdit = it },
            onToggle = { Store.alternarNota(it) },
            onEliminar = { n ->
                confirmar = "¿Eliminar la nota \"${n.titulo}\"?" to {
                    Store.borrarNota(n.id); aviso("Nota eliminada 🗑️")
                }
            },
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
    confirmar?.let { (msg, accion) ->
        DialogoConfirmar(mensaje = msg, onSi = { accion(); confirmar = null }, onNo = { confirmar = null })
    }
}

@Composable
fun CeldaDia(fecha: LocalDate, festivo: String?, notas: List<Nota>, otroMes: Boolean, onClick: () -> Unit) {
    val hoy = fecha == LocalDate.now()
    val domingo = fecha.dayOfWeek.value == 7
    val sabado = fecha.dayOfWeek.value == 6
    val rojo = LocalTema.current.festivo
    val surface = MaterialTheme.colorScheme.surface
    val fondo = when {
        festivo != null -> lerp(surface, rojo, 0.07f)
        domingo -> lerp(surface, rojo, 0.05f)
        sabado -> lerp(surface, MaterialTheme.colorScheme.outline, 0.07f)
        else -> surface
    }
    val borde = if (festivo != null) lerp(rojo, MaterialTheme.colorScheme.outlineVariant, 0.55f)
    else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = fondo,
        border = BorderStroke(1.dp, borde),
        modifier = Modifier.padding(2.dp).fillMaxSize().alpha(if (otroMes) 0.45f else 1f),
    ) {
        Column(Modifier.padding(4.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (hoy) {
                    Box(
                        modifier = Modifier.size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${fecha.dayOfMonth}", color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "${fecha.dayOfMonth}",
                        color = if (festivo != null || domingo) rojo else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (festivo != null) {
                Text(festivo, fontSize = 8.sp, color = rojo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                notas.take(4).forEach { Box(Modifier.size(7.dp).background(colorDeNota(it), CircleShape)) }
                if (notas.size > 4) Text("+${notas.size - 4}", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
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
    val alc = rememberCoroutineScope()
    fun aviso(m: String) { alc.launch { sn.showSnackbar(m) } }

    val u = Store.usuario
    val q = Ui.busqueda.trim().lowercase()
    val notas = u.notas
        .filter { (Ui.estado == "todas") || ((Ui.estado == "hechas") == it.done) }
        .filter { Ui.filtroCat == null || it.catId == Ui.filtroCat }
        .filter { q.isEmpty() || (it.titulo + " " + it.desc).lowercase().contains(q) }
        .sortedWith(compareBy({ it.fecha }, { it.hora }))

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
            notas.groupBy { it.fecha }.forEach { (fecha, lista) ->
                item(key = "h$fecha") {
                    Text(
                        fechaLarga(fecha),
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(lista, key = { it.id }) { n ->
                    TarjetaNota(
                        n = n,
                        onEditar = { notaEdit = n },
                        onEliminar = {
                            confirmar = "¿Eliminar la nota \"${n.titulo}\"?" to {
                                Store.borrarNota(n.id); aviso("Nota eliminada 🗑️")
                            }
                        },
                        onAlternar = { Store.alternarNota(n.id) },
                    )
                }
            }
        }
    }

    notaEdit?.let { n ->
        DialogoNota(nota = n, fechaInicial = n.fecha,
            onCerrar = { notaEdit = null },
            onGuardar = { nueva -> Store.guardarNota(nueva, n.id); notaEdit = null; aviso("Nota actualizada ✏️") })
    }
    if (nueva) {
        DialogoNota(nota = null, fechaInicial = LocalDate.now().toString(),
            onCerrar = { nueva = false },
            onGuardar = { Store.guardarNota(it, null); nueva = false; aviso("Nota creada 📝") })
    }
    confirmar?.let { (msg, accion) ->
        DialogoConfirmar(mensaje = msg, onSi = { accion(); confirmar = null }, onNo = { confirmar = null })
    }
}

@Composable
fun TarjetaNota(n: Nota, compacta: Boolean = false, onEditar: () -> Unit, onEliminar: () -> Unit, onAlternar: () -> Unit) {
    val cat = Store.usuario.categorias.find { it.id == n.catId }
    val colorCat = cat?.let { hex(it.color) } ?: Color(0xFF8D99AE)
    val colorNota = colorDeNota(n)
    val ok = Color(0xFF2FB344)

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
                        .background(if (n.done) ok else Color.Transparent, CircleShape)
                        .border(2.dp, if (n.done) ok else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onAlternar() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (n.done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            n.titulo,
                            modifier = Modifier.weight(1f, fill = false),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (n.done) TextDecoration.LineThrough else null,
                            color = if (n.done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
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
                    if (!compacta) {
                        Text(
                            "📅 ${fechaLarga(n.fecha)}${if (n.hora.isNotEmpty()) " · ⏰ ${n.hora}" else ""}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
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

/* ─────────── diálogos ─────────── */

@Composable
fun DialogoNota(nota: Nota?, fechaInicial: String, onCerrar: () -> Unit, onGuardar: (Nota) -> Unit) {
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
    var mostrarCal by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    var menuCats by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (nota == null) "Nueva nota" else "Editar nota") },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mostrarCal = true }, modifier = Modifier.weight(1f)) {
                        Text(fechaLarga(fecha), maxLines = 1, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { mostrarHora = true }, modifier = Modifier.weight(1f)) {
                        Text(if (hora.isEmpty()) "⏰ Sin hora" else "⏰ $hora", maxLines = 1, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = menuCats, onExpandedChange = { menuCats = it }) {
                    OutlinedTextField(
                        value = cats.find { it.id == catId }?.nombre ?: "Sin categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
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
                Spacer(Modifier.height(10.dp))
                Text("Color de la nota", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FilaSwatches(conAuto = true, valor = color) { color = it }
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
                        catId = catId, color = color,
                        done = nota?.done ?: false,
                        creada = nota?.creada ?: System.currentTimeMillis(),
                    ))
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
        is24Hour = true,
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
    onToggle: (String) -> Unit,
    onEliminar: (Nota) -> Unit,
) {
    val festivo = Festivos.nombre(fecha)
    val notas = Store.usuario.notas
        .filter { it.fecha == fecha && (Ui.filtroCat == null || it.catId == Ui.filtroCat) }
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
                            n = n, compacta = true,
                            onEditar = { onEditar(n) },
                            onEliminar = { onEliminar(n) },
                            onAlternar = { onToggle(n.id) },
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
