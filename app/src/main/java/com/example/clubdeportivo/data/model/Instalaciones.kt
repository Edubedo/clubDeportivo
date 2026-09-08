package com.example.clubdeportivo.data.model

enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Area(
    val id: Int,
    val nombre: String,
    val tipo: String,
    val capacidad: Int,
    val disponibilidad: DisponibilidadArea,
    val permiteExternos: Boolean = false
)

data class Herramienta(
    val id: Int,
    val nombre: String,
    val areaId: Int,
    val cantidadTotal: Int,
    val cantidadDisponible: Int
)

data class RestriccionHorario(
    val id: Int,
    val areaId: Int,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String
)
