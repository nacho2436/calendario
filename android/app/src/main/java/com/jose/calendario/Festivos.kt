package com.jose.calendario

import java.time.LocalDate

/**
 * Festivos de Colombia según la Ley 51 de 1983 ("Ley Emiliani"):
 * los festivos distintos de Año Nuevo, 1.º de mayo, 20 de julio,
 * 7 de agosto, 8 y 25 de diciembre, Jueves y Viernes Santo se
 * trasladan al lunes siguiente.
 */
object Festivos {

    /** Domingo de Pascua (algoritmo gregoriano anónimo). */
    private fun pascua(anio: Int): LocalDate {
        val a = anio % 19; val b = anio / 100; val c = anio % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val mes = (h + l - 7 * m + 114) / 31
        val dia = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(anio, mes, dia)
    }

    private fun lunesSiguiente(d: LocalDate): LocalDate {
        val iso = d.dayOfWeek.value            // lunes = 1 … domingo = 7
        return d.plusDays(((8 - iso) % 7).toLong())
    }

    private val cache = mutableMapOf<Int, Map<String, String>>()

    fun delAnio(anio: Int): Map<String, String> = cache.getOrPut(anio) {
        val p = pascua(anio)
        val suma = { n: Long -> p.plusDays(n) }
        val k = { d: LocalDate -> d.toString() }   // yyyy-MM-dd
        mapOf(
            "%04d-01-01".format(anio) to "Año Nuevo",
            k(lunesSiguiente(LocalDate.of(anio, 1, 6))) to "Reyes Magos",
            k(lunesSiguiente(LocalDate.of(anio, 3, 19))) to "Día de San José",
            k(suma(-3)) to "Jueves Santo",
            k(suma(-2)) to "Viernes Santo",
            "%04d-05-01".format(anio) to "Día del Trabajo",
            k(lunesSiguiente(suma(39))) to "Ascensión de Jesús",
            k(lunesSiguiente(suma(60))) to "Corpus Christi",
            k(lunesSiguiente(suma(68))) to "Sagrado Corazón de Jesús",
            k(lunesSiguiente(LocalDate.of(anio, 6, 29))) to "San Pedro y San Pablo",
            "%04d-07-20".format(anio) to "Día de la Independencia",
            "%04d-08-07".format(anio) to "Batalla de Boyacá",
            k(lunesSiguiente(LocalDate.of(anio, 8, 15))) to "Asunción de la Virgen",
            k(lunesSiguiente(LocalDate.of(anio, 10, 12))) to "Día de la Raza",
            k(lunesSiguiente(LocalDate.of(anio, 11, 1))) to "Todos los Santos",
            k(lunesSiguiente(LocalDate.of(anio, 11, 11))) to "Independencia de Cartagena",
            "%04d-12-08".format(anio) to "Inmaculada Concepción",
            "%04d-12-25".format(anio) to "Navidad",
        )
    }

    /** Nombre del festivo para una fecha ISO (yyyy-MM-dd), o null si no es festivo. */
    fun nombre(fechaISO: String): String? {
        val anio = fechaISO.take(4).toIntOrNull() ?: return null
        for (a in intArrayOf(anio - 1, anio, anio + 1)) {
            delAnio(a)[fechaISO]?.let { return it }
        }
        return null
    }
}
