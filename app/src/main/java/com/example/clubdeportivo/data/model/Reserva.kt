package com.example.clubdeportivo.data.model

enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, FINALIZADA }

data class Reserva(
    val id: Int,
    val usuarioId: Int,
    val areaId: Int,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: EstadoReserva,
    val esExterno: Boolean = false
)

data class MaterialAsignado(
    val id: Int,
    val reservaId: Int,
    val herramientaId: Int,
    val cantidad: Int
)

data class Checkin(
    val id: Int,
    val reservaId: Int,
    val horaLlegada: String
)
