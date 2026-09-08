package com.example.clubdeportivo.data.model

enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, FINALIZADA }

data class Reserva(
    val id: String,
    val usuarioId: String,
    val areaId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: EstadoReserva,
    val esExterno: Boolean = false
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
    val horaLlegada: String
)
