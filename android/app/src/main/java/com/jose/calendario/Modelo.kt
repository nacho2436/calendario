package com.jose.calendario

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class Categoria(val id: String, val nombre: String, val color: String)

data class Nota(
    val id: String,
    val titulo: String,
    val desc: String = "",
    val fecha: String,                 // yyyy-MM-dd
    val hora: String = "",             // HH:mm (opcional)
    val catId: String = "",
    val color: String = "",            // "" → usa el color de la categoría
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
)

data class AppState(val version: Int = 1, val currentUserId: String, val users: List<Usuario>)

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
        prefs.edit().putString("estado", aJson(app)).apply()
    }

    private fun validar(s: AppState): AppState {
        if (s.users.isEmpty()) return estadoInicial()
        return if (s.users.any { it.id == s.currentUserId }) s
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
        val users = app.users.filter { it.id != id }
        val actual = if (app.currentUserId == id) users.first().id else app.currentUserId
        app = AppState(app.version, actual, users)
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

    /* ─────────── serialización JSON (org.json) ─────────── */

    private fun catAJson(c: Categoria) = JSONObject().apply {
        put("id", c.id); put("nombre", c.nombre); put("color", c.color)
    }
    private fun catDeJson(o: JSONObject) = Categoria(o.getString("id"), o.getString("nombre"), o.getString("color"))

    private fun notaAJson(n: Nota) = JSONObject().apply {
        put("id", n.id); put("titulo", n.titulo); put("desc", n.desc); put("fecha", n.fecha)
        put("hora", n.hora); put("catId", n.catId); put("color", n.color)
        put("done", n.done); put("creada", n.creada)
    }
    private fun notaDeJson(o: JSONObject) = Nota(
        id = o.getString("id"), titulo = o.getString("titulo"),
        desc = o.optString("desc", ""), fecha = o.getString("fecha"),
        hora = o.optString("hora", ""), catId = o.optString("catId", ""),
        color = o.optString("color", ""), done = o.optBoolean("done", false),
        creada = o.optLong("creada", System.currentTimeMillis()),
    )

    private fun usuarioAJson(u: Usuario) = JSONObject().apply {
        put("id", u.id); put("nombre", u.nombre); put("avatar", u.avatar)
        put("color", u.color); put("tema", u.tema)
        put("categorias", JSONArray(u.categorias.map { catAJson(it) }))
        put("notas", JSONArray(u.notas.map { notaAJson(it) }))
    }
    private fun usuarioDeJson(o: JSONObject) = Usuario(
        id = o.getString("id"), nombre = o.getString("nombre"),
        avatar = o.optString("avatar", "🙂"), color = o.optString("color", "#4f6df5"),
        tema = o.optString("tema", "claro"),
        categorias = o.optJSONArray("categorias")?.let { ja -> (0 until ja.length()).map { catDeJson(ja.getJSONObject(it)) } } ?: emptyList(),
        notas = o.optJSONArray("notas")?.let { ja -> (0 until ja.length()).map { notaDeJson(ja.getJSONObject(it)) } } ?: emptyList(),
    )

    private fun aJson(s: AppState): String = JSONObject().apply {
        put("version", s.version); put("currentUserId", s.currentUserId)
        put("users", JSONArray(s.users.map { usuarioAJson(it) }))
    }.toString()

    private fun deJson(txt: String): AppState {
        val o = JSONObject(txt)
        val users = o.optJSONArray("users")?.let { ja -> (0 until ja.length()).map { usuarioDeJson(ja.getJSONObject(it)) } } ?: emptyList()
        return AppState(o.optInt("version", 1), o.optString("currentUserId", ""), users)
    }
}
