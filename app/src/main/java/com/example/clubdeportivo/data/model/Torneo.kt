package com.example.clubdeportivo.data.model

data class Torneo(
    val id: Int,
    val nombre: String,
    val disciplina: String,
    /** Área que el torneo bloquea por completo durante su horario: no se permiten reservas individuales ahí. */
    val areaId: Int,
    val fechaInicio: String,
    val fechaFin: String,
    val cupoMaximo: Int,
    val inscritos: Int
)

data class InscripcionTorneo(
    val id: Int,
    val torneoId: Int,
    val usuarioId: Int,
    val fechaInscripcion: String
)
