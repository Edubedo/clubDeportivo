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
    val deporte: String = ""
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