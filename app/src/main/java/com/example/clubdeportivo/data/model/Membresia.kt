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
    val id: Int,
    val usuarioId: Int,
    val tipo: TipoMembresia,
    val plan: PlanIndividual? = null,
    val paqueteFamiliarId: Int? = null,
    val precio: Double,
    val estado: EstadoMembresia,
    val fechaInicio: String,
    val fechaVencimiento: String
)

data class IntegranteFamiliar(
    val id: Int,
    val membresiaId: Int,
    val nombre: String,
    val parentesco: String
)
