package com.example.clubdeportivo.data.model

/**
 * PENDIENTE_APROBACION es lo que el miembro ve como "En revisión": el encargado del área la aprueba (CONFIRMADA) o la
 * rechaza (RECHAZADA). CANCELADA es la que canceló quien reservó.
 */
enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, RECHAZADA, FINALIZADA }

data class Reserva(
    val id: String,
    val usuarioId: String,
    val areaId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: EstadoReserva,
    val esExterno: Boolean = false,
    val personas: Int = 1,


    // Datos para mostrar la reserva
    val usuarioNombre: String = "",
    val areaNombre: String = "",
    val deporte: String = "",
    val carril: Int? = null,

    /** Cambio de fecha/horario que el miembro pidió y que espera la validación del administrador (la reserva sigue igual mientras tanto). */
    val modificacion: ModificacionReserva? = null,
    /** Se canceló porque el área entró en mantenimiento; el miembro puede reagendarla. */
    val canceladaPorMantenimiento: Boolean = false,
    val mantenimientoDesde: String = "",
    val mantenimientoHasta: String = "",
    /** Ya se reagendó (el miembro hizo una reserva nueva): deja de mostrarse el aviso. */
    val reagendada: Boolean = false
)

/** Lo que el miembro quiere cambiar de su reserva; el administrador o encargado lo aprueba o lo rechaza. */
data class ModificacionReserva(
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val personas: Int = 1,
    val carril: Int? = null
)

data class MaterialAsignado(
    val id: String,
    val reservaId: String,
    val herramientaId: String,
    val cantidad: Int
)

data class Checkin(
    val id: String,
    val reservaId: String,
    val asistencia: String,
    val horaLlegada: String = ""
)