package com.jose.calendario

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Motor de avisos: mientras la app está abierta se revisa cada 30 s si
 * alguna nota debe avisar (5/15/30 min, 1/2 horas o 1 día antes).
 * Lanza una notificación del sistema y encola una ventana central que
 * el usuario debe aceptar. Los avisos ya disparados no se repiten.
 */
object MotorAvisos {

    data class AvisoPendiente(val titulo: String, val detalle: String, val desc: String)

    val cola = mutableStateListOf<AvisoPendiente>()
    private val disparados = mutableSetOf<String>()
    private const val CANAL = "recordatorios"

    fun iniciar(contexto: Context) {
        val nm = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        disparados.addAll(
            contexto.getSharedPreferences("calendario_avisos", Context.MODE_PRIVATE)
                .getStringSet("disparados", emptySet()) ?: emptySet()
        )
    }

    fun revisar(contexto: Context) {
        val ahora = LocalDateTime.now()
        val hoy = LocalDate.now()
        var cambio = false
        for (n in Store.usuario.notas) {
            if (n.hora.isEmpty() || n.avisos.isEmpty()) continue
            for (f in listOf(hoy, hoy.plusDays(1))) {   // hoy y mañana (para el aviso de "1 día antes")
                if (!Repeticiones.ocurreEn(n, f)) continue
                val partes = n.hora.split(":")
                val h = partes.getOrNull(0)?.toIntOrNull() ?: continue
                val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                val inicio = f.atTime(h, m)
                for (av in n.avisos) {
                    val t = inicio.minusMinutes(av.toLong())
                    val clave = "${n.id}|$f|$av"
                    if (disparados.contains(clave)) continue
                    val demora = Duration.between(t, ahora).seconds
                    if (demora in 0..89) {
                        disparados.add(clave)
                        cambio = true
                        disparar(contexto, n, av)
                    }
                }
            }
        }
        if (cambio) {
            val manana = hoy.plusDays(1)
            val vigentes = disparados.filter { it.contains("|$hoy|") || it.contains("|$manana|") }.toSet()
            disparados.clear()
            disparados.addAll(vigentes)
            contexto.getSharedPreferences("calendario_avisos", Context.MODE_PRIVATE)
                .edit().putStringSet("disparados", vigentes).apply()
        }
    }

    private fun disparar(contexto: Context, n: Nota, av: Int) {
        val detalle = "${AvisosDef.etiqueta(av)} — comienza a las ${fmtHora(n.hora)}"
        cola.add(AvisoPendiente(n.titulo, detalle, n.desc))
        try {
            val notif = Notification.Builder(contexto, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("⏰ ${n.titulo}")
                .setContentText(detalle)
                .setAutoCancel(true)
                .build()
            (contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(n.id.hashCode() + av, notif)
        } catch (e: Exception) { /* sin permiso o notificaciones bloqueadas */ }
    }

    fun aceptar() {
        if (cola.isNotEmpty()) cola.removeAt(0)
    }
}
