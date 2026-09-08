package com.example.clubdeportivo.data.model

enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }

enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }

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
    val fechaVencimiento: String
)

data class IntegranteFamiliar(
    val id: String,
    val membresiaId: String,
    val nombre: String,
    val parentesco: String
)
