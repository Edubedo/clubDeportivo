package com.example.clubdeportivo.data.model

enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

data class Pago(
    val id: Int,
    val usuarioId: Int,
    val concepto: String,
    val monto: Double,
    val estado: EstadoPago,
    val fecha: String
)

data class Notificacion(
    val id: Int,
    val usuarioId: Int,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val fecha: String
)
