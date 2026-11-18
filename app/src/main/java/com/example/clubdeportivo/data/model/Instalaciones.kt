package com.example.clubdeportivo.data.model

enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Area(
    val id: String,
    val nombre: String,
    val tipo: String,
    val capacidad: Int,
    val disponibilidad: DisponibilidadArea,
    val permiteExternos: Boolean = false,
    /** Emoji propio del deporte cuando se dio de alta uno nuevo; vacío = usar el de [Deportes]. */
    val emoji: String = "",
    val carriles: Int = 0,

    // Información que se mostrará al abrir el detalle del área
    val descripcion: String = "",
    val entrenadores: List<String> = emptyList(),
    val imagenUrl: String = "",

    /**
     * Periodo de mantenimiento ("yyyy-MM-dd", ambos incluidos) cuando [disponibilidad] es MANTENIMIENTO. Solo ese periodo
     * queda bloqueado para reservar; fuera de él el área se reserva normal. Vacío = sin fecha de término (todo bloqueado).
     */
    val mantenimientoDesde: String = "",
    val mantenimientoHasta: String = "",
    val mantenimientoMotivo: String = ""
) {
    /** true si el área está en mantenimiento ese [fecha] ("yyyy-MM-dd"). */
    fun enMantenimientoEl(fecha: String): Boolean {
        if (disponibilidad != DisponibilidadArea.MANTENIMIENTO) return false
        if (mantenimientoDesde.isBlank() || mantenimientoHasta.isBlank()) return true
        return fecha in mantenimientoDesde..mantenimientoHasta
    }

    /** true si el mantenimiento toca algún día entre [desde] y [hasta] ("yyyy-MM-dd", inclusive). */
    fun enMantenimientoEntre(desde: String, hasta: String): Boolean {
        if (disponibilidad != DisponibilidadArea.MANTENIMIENTO) return false
        if (mantenimientoDesde.isBlank() || mantenimientoHasta.isBlank()) return true
        return desde <= mantenimientoHasta && hasta >= mantenimientoDesde
    }

    val tienePeriodoDeMantenimiento get() = mantenimientoDesde.isNotBlank() && mantenimientoHasta.isNotBlank()
}

data class Herramienta(
    val id: String,
    val nombre: String,
    val areaId: String,
    val cantidadTotal: Int,
    val cantidadDisponible: Int
)

data class RestriccionHorario(
    val id: String,
    val areaId: String,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String
)
