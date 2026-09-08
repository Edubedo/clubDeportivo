package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.RestriccionHorario
import kotlinx.coroutines.delay

interface RestriccionHorarioRepository {
    suspend fun obtenerHorarioDeArea(area: Area): RestriccionHorario
}

/**
 * Horario de operación por tipo de área. Todas las áreas de un mismo tipo
 * comparten horario (no se maneja aún horario distinto por día de la semana).
 *
 *   Alberca: 6:00 a.m. - 9:00 p.m.
 *   Canchas (fútbol, básquet, tenis): 6:00 a.m. - 10:00 p.m.
 *   Gimnasio: 5:00 a.m. - 10:00 p.m.
 *   Salón de usos múltiples (clases grupales): 7:00 a.m. - 9:00 p.m.
 */
class FakeRestriccionHorarioRepository : RestriccionHorarioRepository {

    private val horarioPorTipo = mapOf(
        "Natación" to ("06:00" to "21:00"),
        "Fútbol" to ("06:00" to "22:00"),
        "Básquetbol" to ("06:00" to "22:00"),
        "Tenis" to ("06:00" to "22:00"),
        "Gimnasio" to ("05:00" to "22:00"),
        "Clases grupales" to ("07:00" to "21:00")
    )

    override suspend fun obtenerHorarioDeArea(area: Area): RestriccionHorario {
        delay(150)
        val (inicio, fin) = horarioPorTipo[area.tipo] ?: ("08:00" to "20:00")
        return RestriccionHorario(
            id = "rh-${area.id}",
            areaId = area.id,
            diaSemana = "TODOS",
            horaInicio = inicio,
            horaFin = fin
        )
    }
}
