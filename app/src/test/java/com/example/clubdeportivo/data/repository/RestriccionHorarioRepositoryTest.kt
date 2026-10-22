package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RestriccionHorarioRepositoryTest {

    @Test
    fun `todas las areas usan el horario de futbol`() = runBlocking {
        val repository = FakeRestriccionHorarioRepository()
        val areas = listOf(
            Area("futbol", "Cancha de fútbol", "Fútbol", 22, DisponibilidadArea.DISPONIBLE),
            Area("alberca", "Alberca", "Natación", 12, DisponibilidadArea.DISPONIBLE),
            Area("personalizada", "Cancha de handball", "Handball", 14, DisponibilidadArea.DISPONIBLE)
        )

        areas.forEach { area ->
            val horario = repository.obtenerHorarioDeArea(area)
            assertEquals("06:00", horario.horaInicio)
            assertEquals("22:00", horario.horaFin)
        }
    }
}
