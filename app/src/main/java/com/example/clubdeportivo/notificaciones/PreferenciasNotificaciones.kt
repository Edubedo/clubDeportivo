package com.example.clubdeportivo.notificaciones

import android.content.Context
import com.example.clubdeportivo.util.Fechas

/**
 * Lo que la persona decidió recibir (un interruptor por categoría, todas activas al principio) y lo que ya se le avisó.
 * Todo vive en el teléfono; lo de "ya avisado" va por usuario para que dos cuentas en el mismo teléfono no se mezclen.
 */
object PreferenciasNotificaciones {
    private const val ARCHIVO_AJUSTES = "notificaciones_ajustes"
    private const val DIAS_QUE_SE_RECUERDA = 10

    private fun ajustes(contexto: Context) = contexto.getSharedPreferences(ARCHIVO_AJUSTES, Context.MODE_PRIVATE)

    fun activa(contexto: Context, categoria: CategoriaNotificacion): Boolean =
        runCatching { ajustes(contexto).getBoolean("cat_${categoria.name}", true) }.getOrDefault(true)

    fun activar(contexto: Context, categoria: CategoriaNotificacion, activa: Boolean) {
        runCatching { ajustes(contexto).edit().putBoolean("cat_${categoria.name}", activa).apply() }
    }

    /** Si ya se le pidió el permiso de notificaciones (Android 13+) para no insistir en cada inicio. */
    fun yaPidioPermiso(contexto: Context): Boolean = ajustes(contexto).getBoolean("pidio_permiso", false)

    fun marcarPermisoPedido(contexto: Context) {
        ajustes(contexto).edit().putBoolean("pidio_permiso", true).apply()
    }

    /** Memoria de una persona: qué ya se avisó, qué estado tenían sus reservas y cuál fue el último aviso del club visto. */
    class Registro(contexto: Context, uid: String) {
        private val prefs = contexto.getSharedPreferences("notificaciones_$uid", Context.MODE_PRIVATE)

        fun yaAvisado(clave: String): Boolean = prefs.getStringSet("avisados", emptySet())!!.any { it.substringBeforeLast('|') == clave }

        fun marcarAvisado(claves: List<String>, hoy: String) {
            val dia = Fechas.combinar(hoy, "12:00").time / 86_400_000L
            val vigentes = prefs.getStringSet("avisados", emptySet())!!
                .filter { (it.substringAfterLast('|').toLongOrNull() ?: 0L) >= dia - DIAS_QUE_SE_RECUERDA }
                .toMutableSet()
            claves.forEach { vigentes += "$it|$dia" }
            prefs.edit().putStringSet("avisados", vigentes).apply()
        }

        /** id de reserva -> nombre de su estado, tal como se vio en la revisión anterior. */
        fun estadosDeReservas(): Map<String, String> =
            prefs.getString("estados", "").orEmpty().split(';').filter { '=' in it }
                .associate { it.substringBefore('=') to it.substringAfter('=') }

        fun guardarEstadosDeReservas(estados: Map<String, String>) {
            prefs.edit().putString("estados", estados.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
        }

        /** Marca de tiempo del último aviso del club ya visto; 0 = todavía no se ha revisado nunca. */
        var ultimoAvisoMillis: Long
            get() = prefs.getLong("ultimo_aviso", 0L)
            set(valor) { prefs.edit().putLong("ultimo_aviso", valor).apply() }
    }
}
