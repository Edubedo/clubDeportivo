package com.example.clubdeportivo.data.model

data class Torneo(
    val id: String,
    val nombre: String,
    val disciplina: String,
    /** Área que el torneo bloquea por completo durante su horario: no se permiten reservas individuales ahí. */
    val areaId: String,
    val fechaInicio: String,
    val fechaFin: String,
    val cupoMaximo: Int,
    val inscritos: Int
)

data class InscripcionTorneo(
    val id: String,
    val torneoId: String,
    val usuarioId: String,
    val fechaInscripcion: String
)
