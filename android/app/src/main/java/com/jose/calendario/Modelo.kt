package com.jose.calendario

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Categoria(val id: String, val nombre: String, val color: String)

data class Nota(
    val id: String,
    val titulo: String,
    val desc: String = "",
    val fecha: String,                 // yyyy-MM-dd (fecha de inicio)
    val hora: String = "",             // HH:mm en 24 h (opcional)
    val catId: String = "",
    val color: String = "",            // "" → usa el color de la categoría
    val repeticion: String = "",       // "" | diaria | semanal | mensual | anual
    val avisos: List<Int> = emptyList(), // minutos antes (varios)
    val done: Boolean = false,
    val creada: Long = System.currentTimeMillis(),
)

data class Usuario(
    val id: String,
    val nombre: String,
    val avatar: String = "🙂",
    val color: String = "#4f6df5",
    val tema: String = "claro",
    val categorias: List<Categoria> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val actualizado: Long = 0L,   // marca de sincronización (el más nuevo gana)
)

data class BajaUsuario(val id: String, val nombre: String, val ts: Long)

data class AppState(
    val version: Int = 1,
    val currentUserId: String,
    val users: List<Usuario>,
    val eliminados: List<BajaUsuario> = emptyList(),
)

/* ─────────── Repeticiones ─────────── */

object Repeticiones {
    val OPCIONES = listOf(
        "" to "No repetir",
        "diaria" to "Todos los días",
        "semanal" to "Cada semana",
        "mensual" to "Cada mes",
        "anual" to "Cada año",
    )
    val CORTO = mapOf(
        "diaria" to "cada día", "semanal" to "cada semana",
        "mensual" to "cada mes", "anual" to "cada año",
    )

    /** ¿La nota ocurre en la fecha dada? (la repetición parte de su fecha inicial) */
    fun ocurreEn(n: Nota, fecha: LocalDate): Boolean {
        val inicio = try { LocalDate.parse(n.fecha) } catch (e: Exception) { return false }
        if (n.repeticion.isEmpty()) return inicio == fecha
        if (fecha.isBefore(inicio)) return false
        return when (n.repeticion) {
            "diaria" -> true
            "semanal" -> ChronoUnit.DAYS.between(inicio, fecha) % 7 == 0L
            "mensual" -> fecha.dayOfMonth == inicio.dayOfMonth
            "anual" -> fecha.dayOfMonth == inicio.dayOfMonth && fecha.monthValue == inicio.monthValue
            else -> false
        }
    }

    /** Próxima fecha en que ocurre la nota (hoy o después). */
    fun proximaFecha(n: Nota): LocalDate {
        val inicio = try { LocalDate.parse(n.fecha) } catch (e: Exception) { return LocalDate.now() }
        val hoy = LocalDate.now()
        if (n.repeticion.isEmpty() || !inicio.isBefore(hoy)) return inicio
        var d = hoy
        repeat(800) {
            if (ocurreEn(n, d)) return d
            d = d.plusDays(1)
        }
        return inicio
    }
}

/* ─────────── Avisos ─────────── */

object AvisosDef {
    val OPCIONES = listOf(
        5 to "5 min", 15 to "15 min", 30 to "30 min",
        60 to "1 hora", 120 to "2 horas", 1440 to "1 día",
    )
    fun etiqueta(min: Int) = when (min) {
        5 -> "5 minutos antes"; 15 -> "15 minutos antes"; 30 -> "30 minutos antes"
        60 -> "1 hora antes"; 120 -> "2 horas antes"; 1440 -> "1 día antes"
        else -> "$min minutos antes"
    }
    fun corto(min: Int) = OPCIONES.firstOrNull { it.first == min }?.second ?: "$min min"
}

/** Hora guardada en 24 h, mostrada en 12 h con am/pm. */
fun fmtHora(hora: String): String {
    if (hora.isEmpty()) return ""
    val partes = hora.split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: return hora
    val m = (partes.getOrNull(1) ?: "00").padStart(2, '0')
    val suf = if (h < 12) "am" else "pm"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:$m $suf"
}

/**
 * Estado global de la app, persistido en SharedPreferences como JSON.
 * Cada usuario guarda sus propias notas, categorías y tema.
 */
object Store {
    private lateinit var prefs: SharedPreferences

    var app by mutableStateOf(AppState(currentUserId = "", users = emptyList()))
        private set

    val usuario: Usuario get() = app.users.firstOrNull { it.id == app.currentUserId } ?: app.users.first()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("calendario_colombia", Context.MODE_PRIVATE)
        app = prefs.getString("estado", null)
            ?.let { runCatching { deJson(it) }.getOrNull() }
            ?.let { validar(it) }
            ?: estadoInicial()
        guardar()
    }

    fun guardar() {
        val u = app.users.firstOrNull { it.id == app.currentUserId }
        if (u != null) {
            val ahora = System.currentTimeMillis()
            app = app.copy(users = app.users.map { if (it.id == u.id) it.copy(actualizado = ahora) else it })
        }
        prefs.edit().putString("estado", aJson(app)).apply()
    }

    private fun validar(s: AppState): AppState {
        if (s.users.isEmpty()) return estadoInicial()
        return if (s.users.any { it.id === s.currentUserId || it.id == s.currentUserId }) s
        else s.copy(currentUserId = s.users.first().id)
    }

    private fun conUsuario(u: Usuario): AppState =
        app.copy(users = app.users.map { if (it.id == u.id) u else it })

    /* ─────────── acciones ─────────── */

    fun guardarNota(n: Nota, idEdicion: String?) {
        val usr = usuario
        app = conUsuario(usr.copy(notas =
            if (idEdicion == null) usr.notas + n
            else usr.notas.map { if (it.id == idEdicion) n else it }))
        guardar()
    }

    fun alternarNota(id: String) {
        val usr = usuario
        app = conUsuario(usr.copy(notas = usr.notas.map { if (it.id == id) it.copy(done = !it.done) else it }))
        guardar()
    }

    fun borrarNota(id: String) {
        val usr = usuario
        app = conUsuario(usr.copy(notas = usr.notas.filter { it.id != id }))
        guardar()
    }

    fun crearCategoria(c: Categoria) {
        val usr = usuario
        app = conUsuario(usr.copy(categorias = usr.categorias + c))
        guardar()
    }

    fun actualizarCategoria(c: Categoria) {
        val usr = usuario
        app = conUsuario(usr.copy(categorias = usr.categorias.map { if (it.id == c.id) c else it }))
        guardar()
    }

    fun borrarCategoria(id: String) {
        val usr = usuario
        app = conUsuario(usr.copy(
            notas = usr.notas.map { if (it.catId == id) it.copy(catId = "") else it },
            categorias = usr.categorias.filter { it.id != id }))
        guardar()
    }

    fun nuevoUsuario(nombre: String, avatar: String, color: String): Usuario {
        val cats = categoriasPorDefecto()
        return Usuario(
            id = UUID.randomUUID().toString(), nombre = nombre, avatar = avatar, color = color,
            tema = usuario.tema, categorias = cats,
            notas = listOf(notaBienvenida(cats.first().id)),
        )
    }

    fun crearUsuario(u: Usuario) {
        app = app.copy(users = app.users + u, currentUserId = u.id)
        guardar()
    }

    fun actualizarUsuario(u: Usuario) {
        app = app.copy(users = app.users.map { if (it.id == u.id) u else it })
        guardar()
    }

    fun borrarUsuario(id: String) {
        if (app.users.size <= 1) return
        val u = app.users.firstOrNull { it.id == id } ?: return
        val users = app.users.filter { it.id != id }
        val actual = if (app.currentUserId == id) users.first().id else app.currentUserId
        val baja = BajaUsuario(id, u.nombre, System.currentTimeMillis())
        app = AppState(app.version, actual, users, app.eliminados + baja)
        guardar()
    }

    fun cambiarUsuario(id: String) {
        if (app.users.any { it.id == id } && id != app.currentUserId) {
            app = app.copy(currentUserId = id)
            guardar()
        }
    }

    fun definirTema(id: String) {
        app = conUsuario(usuario.copy(tema = id))
        guardar()
    }

    /* ─────────── datos iniciales ─────────── */

    private fun newId() = UUID.randomUUID().toString()

    private fun categoriasPorDefecto() = listOf(
        Categoria(newId(), "Personal", "#8e6df5"),
        Categoria(newId(), "Trabajo", "#4f6df5"),
        Categoria(newId(), "Salud", "#66bb6a"),
        Categoria(newId(), "Cumpleaños", "#f2913d"),
    )

    private fun notaBienvenida(catId: String) = Nota(
        id = newId(),
        titulo = "¡Hola! 👋 Nota de bienvenida",
        desc = "Toca cualquier día del calendario para agregar tus notas y recordatorios.",
        fecha = LocalDate.now().toString(), hora = "09:00", catId = catId,
    )

    private fun estadoInicial(): AppState {
        val cats = categoriasPorDefecto()
        val u = Usuario(
            id = newId(), nombre = "Usuario 1",
            categorias = cats, notas = listOf(notaBienvenida(cats.first().id)),
        )
        return AppState(currentUserId = u.id, users = listOf(u))
    }

    /* ─────────── sincronización con el servidor web ─────────── */

    fun exportarUsuariosJson(): String = JSONObject().apply {
        put("users", JSONArray(app.users.map { usuarioAJson(it) }))
        put("eliminados", JSONArray(app.eliminados.map { bajaAJson(it) }))
    }.toString()

    private fun normTxt(s: String?): String =
        (s ?: "").lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    /** Fusiona dos documentos del mismo usuario: perfil del más reciente,
     *  notas y categorías de ambos combinadas sin duplicar (notas por id y
     *  por título+fecha; categorías por nombre, remapeando referencias). */
    private fun fusionarUsuario(a: Usuario, b: Usuario): Usuario {
        val base = if (a.actualizado >= b.actualizado) a else b
        val otro = if (base === a) b else a
        val categorias = LinkedHashMap(base.categorias.associateBy { it.id })
        val porNombre = HashMap<String, Categoria>()
        categorias.values.forEach { porNombre[normTxt(it.nombre)] = it }
        val mapa = HashMap<String, String>()
        for (c in otro.categorias){
            val nn = normTxt(c.nombre)
            val existente = porNombre[nn]
            if (existente != null) mapa[c.id] = existente.id
            else { categorias[c.id] = c; porNombre[nn] = c }
        }
        val notas = LinkedHashMap(base.notas.associateBy { it.id })
        val usados = HashSet<String>()
        notas.values.forEach { usados.add(normTxt(it.titulo) + "|" + it.fecha) }
        for (n in otro.notas){
            val clave = normTxt(n.titulo) + "|" + n.fecha
            if (notas.containsKey(n.id) || clave in usados) continue
            val m = n.copy(catId = mapa[n.catId] ?: n.catId)
            notas[m.id] = m
            usados.add(clave)
        }
        return base.copy(
            categorias = categorias.values.toList(),
            notas = notas.values.toList(),
            actualizado = maxOf(a.actualizado, b.actualizado))
    }

    /** Fusiona los usuarios del servidor emparejando por id o por nombre
     *  (mismo usuario con ids distintos en cada dispositivo). Devuelve cambios. */
    fun importarYFusionar(texto: String): Int {
        val raiz = JSONObject(texto)
        val lista = raiz.optJSONArray("users") ?: return 0
        val entrantes = (0 until lista.length()).map { usuarioDeJson(lista.getJSONObject(it)) }
        // 1. fusionar bajas locales y del servidor
        val bajasEntrantes = raiz.optJSONArray("eliminados")?.let { ja ->
            (0 until ja.length()).map { bajaDeJson(ja.getJSONObject(it)) }
        } ?: emptyList()
        val mapaBajas = HashMap<String, BajaUsuario>()
        (app.eliminados + bajasEntrantes).forEach { b ->
            val k = b.id + "|" + normTxt(b.nombre)
            val prev = mapaBajas[k]
            if (prev == null || b.ts > prev.ts) mapaBajas[k] = b
        }
        val limite = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
        val bajas = mapaBajas.values.filter { it.ts > limite }
        val eliminado = { u: Usuario ->
            bajas.any { b -> (b.id == u.id || normTxt(b.nombre) == normTxt(u.nombre)) && b.ts > u.actualizado }
        }
        // 2. fusionar usuarios (los eliminados no vuelven)
        var cambios = 0
        val fusionados = app.users.filter { !eliminado(it) }.toMutableList()
        for (r in entrantes){
            if (eliminado(r)) continue
            var i = fusionados.indexOfFirst { it.id == r.id }
            if (i < 0) i = fusionados.indexOfFirst { normTxt(it.nombre) == normTxt(r.nombre) }
            if (i < 0){
                fusionados.add(r); cambios++
            } else {
                val f = fusionarUsuario(fusionados[i], r).copy(id = fusionados[i].id)
                if (f != fusionados[i]) cambios++
                fusionados[i] = f
            }
        }
        var nuevo = app.copy(users = fusionados, eliminados = bajas)
        if (!fusionados.any { it.id == nuevo.currentUserId }){
            nuevo = nuevo.copy(currentUserId = fusionados.firstOrNull()?.id ?: nuevo.currentUserId)
        }
        app = nuevo
        guardar()
        return cambios
    }

    /* ─────────── serialización JSON (org.json) ─────────── */

    private fun catAJson(c: Categoria) = JSONObject().apply {
        put("id", c.id); put("nombre", c.nombre); put("color", c.color)
    }
    private fun catDeJson(o: JSONObject) = Categoria(o.getString("id"), o.getString("nombre"), o.getString("color"))

    private fun notaAJson(n: Nota) = JSONObject().apply {
        put("id", n.id); put("titulo", n.titulo); put("desc", n.desc); put("fecha", n.fecha)
        put("hora", n.hora); put("catId", n.catId); put("color", n.color)
        put("repeticion", n.repeticion); put("avisos", JSONArray(n.avisos))
        put("done", n.done); put("creada", n.creada)
    }
    private fun notaDeJson(o: JSONObject) = Nota(
        id = o.getString("id"), titulo = o.getString("titulo"),
        desc = o.optString("desc", ""), fecha = o.getString("fecha"),
        hora = o.optString("hora", ""), catId = o.optString("catId", ""),
        color = o.optString("color", ""), repeticion = o.optString("repeticion", ""),
        avisos = o.optJSONArray("avisos")?.let { ja -> (0 until ja.length()).map { ja.optInt(it) } } ?: emptyList(),
        done = o.optBoolean("done", false),
        creada = o.optLong("creada", System.currentTimeMillis()),
    )

    private fun usuarioAJson(u: Usuario) = JSONObject().apply {
        put("id", u.id); put("nombre", u.nombre); put("avatar", u.avatar)
        put("color", u.color); put("tema", u.tema); put("actualizado", u.actualizado)
        put("categorias", JSONArray(u.categorias.map { catAJson(it) }))
        put("notas", JSONArray(u.notas.map { notaAJson(it) }))
    }
    private fun usuarioDeJson(o: JSONObject) = Usuario(
        id = o.getString("id"), nombre = o.getString("nombre"),
        avatar = o.optString("avatar", "🙂"), color = o.optString("color", "#4f6df5"),
        tema = o.optString("tema", "claro"), actualizado = o.optLong("actualizado", 0L),
        categorias = o.optJSONArray("categorias")?.let { ja -> (0 until ja.length()).map { catDeJson(ja.getJSONObject(it)) } } ?: emptyList(),
        notas = o.optJSONArray("notas")?.let { ja -> (0 until ja.length()).map { notaDeJson(ja.getJSONObject(it)) } } ?: emptyList(),
    )

    private fun bajaAJson(b: BajaUsuario) = JSONObject().apply {
        put("id", b.id); put("nombre", b.nombre); put("ts", b.ts)
    }
    private fun bajaDeJson(o: JSONObject) =
        BajaUsuario(o.optString("id"), o.optString("nombre"), o.optLong("ts", 0L))

    private fun aJson(s: AppState): String = JSONObject().apply {
        put("version", s.version); put("currentUserId", s.currentUserId)
        put("users", JSONArray(s.users.map { usuarioAJson(it) }))
        put("eliminados", JSONArray(s.eliminados.map { bajaAJson(it) }))
    }.toString()

    private fun deJson(txt: String): AppState {
        val o = JSONObject(txt)
        val users = o.optJSONArray("users")?.let { ja -> (0 until ja.length()).map { usuarioDeJson(ja.getJSONObject(it)) } } ?: emptyList()
        val eliminados = o.optJSONArray("eliminados")?.let { ja -> (0 until ja.length()).map { bajaDeJson(ja.getJSONObject(it)) } } ?: emptyList()
        return AppState(o.optInt("version", 1), o.optString("currentUserId", ""), users, eliminados)
    }
}

/* ─────────── Sincronización con el servidor web ─────────── */

object Sincronizacion {
    /** Completa la dirección: "192.168.1.17:8177" → "http://192.168.1.17:8177". */
    fun normalizarUrl(entrada: String): String {
        var s = entrada.trim()
        if (s.isNotEmpty() && !s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        return s.trimEnd('/')
    }

    /** POST JSON al servidor; devuelve el cuerpo de la respuesta. */
    fun httpPost(url: String, cuerpo: String): String {
        val conn = try {
            java.net.URL(url).openConnection() as java.net.HttpURLConnection
        } catch (e: java.net.MalformedURLException) {
            throw Exception("dirección no válida (ej.: http://192.168.1.17:8177)")
        }
        conn.requestMethod = "POST"
        conn.connectTimeout = 5000
        conn.readTimeout = 12000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        try {
            conn.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("sin respuesta · ¿el PC y el celular están en la misma red WiFi?")
        } catch (e: java.net.ConnectException) {
            throw Exception("conexión rechazada · ¿está encendido el servidor? (./menu.sh)")
        } catch (e: java.net.UnknownHostException) {
            throw Exception("no se encuentra esa dirección · revisa la IP en ./menu.sh")
        }
        val codigo = conn.responseCode
        if (codigo !in 200..299) throw Exception("el servidor respondió HTTP $codigo")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}

