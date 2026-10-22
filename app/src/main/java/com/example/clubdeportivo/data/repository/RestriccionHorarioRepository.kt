package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.RestriccionHorario
import kotlinx.coroutines.delay

interface RestriccionHorarioRepository {
    suspend fun obtenerHorarioDeArea(area: Area): RestriccionHorario
}

/**
 * Todas las áreas comparten el horario actual de las canchas de fútbol.
 */
class FakeRestriccionHorarioRepository : RestriccionHorarioRepository {

    override suspend fun obtenerHorarioDeArea(area: Area): RestriccionHorario {
        delay(150)
        return RestriccionHorario(
            id = "rh-${area.id}",
            areaId = area.id,
            diaSemana = "TODOS",
            horaInicio = Catalogos.HORA_APERTURA_AREAS,
            horaFin = Catalogos.HORA_CIERRE_AREAS
        )
    }
}
