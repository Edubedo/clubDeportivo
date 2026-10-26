package com.example.clubdeportivo.notificaciones

import androidx.core.app.NotificationManagerCompat
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esEncargado

/** Los tres tipos de persona que usan la app; cada uno recibe un conjunto distinto de recordatorios. */
enum class TipoUsuario {
    CLIENTE,
    ENCARGADO,
    ADMINISTRADOR;

    companion object {
        fun de(rol: Rol): TipoUsuario = when {
            rol.esAdministrador() -> ADMINISTRADOR
            rol.esEncargado() -> ENCARGADO
            else -> CLIENTE
        }
    }
}

/**
 * Cada categoría es un canal de notificaciones de Android (la persona puede silenciar una sola desde los ajustes del
 * sistema) y un interruptor en Perfil → Notificaciones. [tipos] limita quién la recibe.
 */
enum class CategoriaNotificacion(
    val nombre: String,
    val descripcion: String,
    val importancia: Int,
    val tipos: Set<TipoUsuario>
) {
    // ---- Miembros y visitantes
    RESERVA_PROXIMA("notif_cat_reserva_proxima", "notif_cat_reserva_proxima_desc", NotificationManagerCompat.IMPORTANCE_HIGH, setOf(TipoUsuario.CLIENTE)),
    RESERVA_RESPUESTA("notif_cat_reserva_respuesta", "notif_cat_reserva_respuesta_desc", NotificationManagerCompat.IMPORTANCE_HIGH, setOf(TipoUsuario.CLIENTE)),
    MEMBRESIA("notif_cat_membresia", "notif_cat_membresia_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, setOf(TipoUsuario.CLIENTE)),
    LISTA_ESPERA("notif_cat_espera", "notif_cat_espera_desc", NotificationManagerCompat.IMPORTANCE_HIGH, setOf(TipoUsuario.CLIENTE)),

    // ---- Encargados y administradores
    SOLICITUDES("notif_cat_solicitudes", "notif_cat_solicitudes_desc", NotificationManagerCompat.IMPORTANCE_HIGH, setOf(TipoUsuario.ENCARGADO, TipoUsuario.ADMINISTRADOR)),
    ASISTENCIA("notif_cat_asistencia", "notif_cat_asistencia_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, setOf(TipoUsuario.ENCARGADO)),
    STOCK("notif_cat_stock", "notif_cat_stock_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, setOf(TipoUsuario.ENCARGADO, TipoUsuario.ADMINISTRADOR)),
    VENCIMIENTOS("notif_cat_vencimientos", "notif_cat_vencimientos_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, setOf(TipoUsuario.ADMINISTRADOR)),

    // ---- Todos
    TORNEO("notif_cat_torneo", "notif_cat_torneo_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, TipoUsuario.entries.toSet()),
    AVISOS("notif_cat_avisos", "notif_cat_avisos_desc", NotificationManagerCompat.IMPORTANCE_DEFAULT, TipoUsuario.entries.toSet());

    val canalId: String get() = "club_${name.lowercase()}"

    companion object {
        fun de(tipo: TipoUsuario): List<CategoriaNotificacion> = entries.filter { tipo in it.tipos }
    }
}

/** Un recordatorio listo para mostrar. [claves] identifican lo que lo originó, para no repetirlo en la siguiente revisión. */
data class Recordatorio(
    val claves: List<String>,
    val categoria: CategoriaNotificacion,
    val titulo: String,
    val mensaje: String,
    /** Pantalla que abre al tocarlo (una ruta de `Destinations`); null = solo abre la app. */
    val ruta: String? = null
) {
    constructor(clave: String, categoria: CategoriaNotificacion, titulo: String, mensaje: String, ruta: String? = null) :
        this(listOf(clave), categoria, titulo, mensaje, ruta)
}
