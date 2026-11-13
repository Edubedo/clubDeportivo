package com.example.clubdeportivo.data.model

data class SolicitudRenovacion(
    val id: String,
    val socioId: String,
    val socioNombre: String,
    val membresiaId: String,
    val planId: String,
    val monto: Double,
    val concepto: String,
    val comprobanteUrl: String,
    val status: String,
    val createdAt: Long,
    val motivoRechazo: String = "",
    val reviewedDate: String = "",
    val reviewedAt: Long? = null,
    val renewedUntil: String = ""
) {
    companion object {
        const val PENDIENTE = "PENDIENTE"
        const val APROBADO = "APROBADO"
        const val RECHAZADO = "RECHAZADO"
    }
}
