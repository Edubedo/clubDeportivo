package com.example.clubdeportivo.data.model

import com.example.clubdeportivo.util.Idioma
import com.example.clubdeportivo.util.Textos
enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }

enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }

/**
 * Razones por las que el personal puede suspender una membresía. Se guardan con la suspensión (siempre en español,
 * como el resto de los datos) y las ve el miembro en su idioma con [etiquetaMotivoSuspension].
 */
enum class MotivoSuspension(internal val clave: String, val pideDetalle: Boolean = false) {
    FALTA_DE_PAGO("missed_payment"),
    INCUMPLIMIENTO_REGLAMENTO("rule_violation"),
    CONDUCTA("inappropriate_conduct"),
    SOLICITUD_DEL_MIEMBRO("at_the_member_s_request"),
    DOCUMENTACION("incomplete_documentation"),
    MOTIVO_MEDICO("medical_reasons"),
    OTRO("other_reason", pideDetalle = true);

    /** Cómo se muestra en el idioma elegido. */
    val etiqueta: String get() = Textos.get(clave)

    /** Texto que se guarda: la etiqueta en español, o "Otro: <detalle>" cuando el personal lo escribe. */
    fun textoGuardado(detalle: String): String {
        val enEspanol = Textos.en(Idioma.ES, clave)
        return if (pideDetalle) prefijoGuardado(enEspanol) + detalle.trim() else enEspanol
    }

    /** "Otro motivo" → "Otro: ". */
    internal fun prefijoGuardado(etiqueta: String): String = "${etiqueta.substringBefore(" ")}: "

    companion object {
        /** Motivo que se guarda al dar de baja a un miembro desde Membresías. */
        const val CLAVE_BAJA_MIEMBRO = "miembro_motivo_baja"
    }
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
