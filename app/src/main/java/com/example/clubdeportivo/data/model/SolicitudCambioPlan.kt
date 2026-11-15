package com.example.clubdeportivo.data.model

/**
 * Petición de un miembro para pasar a otro plan individual (de más nivel o de menos). La resuelve un administrador
 * o un supervisor. Vive en la colección `solicitudes_cambio_plan`.
 *
 * Los planes se guardan por nombre ([PlanIndividual.name]) y los precios al momento de pedirlo, para que la
 * solicitud siga diciendo lo mismo aunque el administrador cambie los precios después.
 */
data class SolicitudCambioPlan(
    val id: String,
    val socioId: String,
    val socioNombre: String,
    val membresiaId: String,
    val planActual: PlanIndividual,
    val planNuevo: PlanIndividual,
    val precioActual: Double,
    val precioNuevo: Double,
    val status: String,
    val createdAt: Long,
    val motivoRechazo: String = "",
    val revisadoPor: String = "",
    val revisadoPorNombre: String = "",
    val revisadaEn: Long? = null
) {
    val esMejora: Boolean get() = planNuevo.ordinal > planActual.ordinal

    companion object {
        const val PENDIENTE = "PENDIENTE"
        const val APROBADO = "APROBADO"
        const val RECHAZADO = "RECHAZADO"
    }
}
