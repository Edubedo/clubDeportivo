package com.example.clubdeportivo.data.model

enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }

enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }

/** Razones por las que el personal puede suspender una membresía. Se guardan con la suspensión y las ve el miembro. */
enum class MotivoSuspension(val etiqueta: String, val pideDetalle: Boolean = false) {
    FALTA_DE_PAGO("Falta de pago"),
    INCUMPLIMIENTO_REGLAMENTO("Incumplimiento del reglamento"),
    CONDUCTA("Conducta inapropiada"),
    SOLICITUD_DEL_MIEMBRO("A solicitud del miembro"),
    DOCUMENTACION("Documentación incompleta"),
    MOTIVO_MEDICO("Motivo médico"),
    OTRO("Otro motivo", pideDetalle = true);

    /** Texto que se guarda: la etiqueta, o "Otro: <detalle>" cuando el personal lo escribe. */
    fun textoGuardado(detalle: String): String =
        if (pideDetalle) "${etiqueta.substringBefore(" ")}: ${detalle.trim()}" else etiqueta
}

/** Planes de membresía individual (sin paquete familiar). */
enum class PlanIndividual { NINO, NORMAL, DELUXE }

data class PaqueteFamiliar(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val maxIntegrantes: Int,
    val precioMensual: Double
)

data class Membresia(
    val id: String,
    val usuarioId: String,
    val tipo: TipoMembresia,
    val plan: PlanIndividual? = null,
    /** Referencia a Catalogos.paquetesFamiliares (dato fijo local, no vive en la base de datos). */
    val paqueteFamiliarId: Int? = null,
    val precio: Double,
    val estado: EstadoMembresia,
    val fechaInicio: String,
    val fechaVencimiento: String,
    /** Por qué está suspendida (vacío si no lo está). */
    val motivoSuspension: String = ""
)

data class IntegranteFamiliar(
    val id: String,
    val membresiaId: String,
    val nombre: String,
    val parentesco: String
)
