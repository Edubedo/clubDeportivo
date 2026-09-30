package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Torneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasReservaTest {

    private val cancha = Area("a1", "Cancha tenis 1", "Tenis", 4, DisponibilidadArea.DISPONIBLE)
    private val fecha = "2026-10-10"

    private fun reserva(
        hora: Int,
        horas: Int = 1,
        personas: Int = 1,
        estado: EstadoReserva = EstadoReserva.CONFIRMADA,
        areaId: String = cancha.id,
        dia: String = fecha
    ) = Reserva(
        id = "r$hora-$personas-$areaId-$dia",
        usuarioId = "u",
        areaId = areaId,
        fecha = dia,
        horaInicio = "%02d:00".format(hora),
        horaFin = "%02d:00".format(hora + horas),
        estado = estado,
        personas = personas
    )

    private fun torneo(
        areaId: String = cancha.id,
        inicio: String = fecha,
        fin: String = fecha,
        horaInicio: String = "10:00",
        horaFin: String = "12:00",
        id: String = "t1"
    ) = Torneo(id, "Copa", "Tenis", areaId, inicio, fin, 8, 0, horaInicio, horaFin)

    @Test
    fun `una reserva en un area vacia es valida`() {
        assertNull(ReglasReserva.validarReserva(cancha, emptyList(), emptyList(), fecha, "09:00", "10:00", 1))
    }

    @Test
    fun `cuando el cupo se llena ya no se puede reservar esa hora`() {
        val reservas = listOf(reserva(9, personas = 3), reserva(9, personas = 1))

        val error = ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "10:00", 1)

        assertNotNull(error)
        assertTrue(error!!.contains("cupo límite"))
    }

    @Test
    fun `si quedan menos lugares que personas pedidas se rechaza`() {
        val reservas = listOf(reserva(9, personas = 3))

        assertNull(ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "10:00", 1))
        assertNotNull(ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "10:00", 2))
    }

    @Test
    fun `basta una hora llena dentro del rango para rechazar toda la reserva`() {
        val reservas = listOf(reserva(10, personas = 4))

        val error = ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "12:00", 1)

        assertNotNull(error)
        assertTrue(error!!.contains("10:00"))
    }

    @Test
    fun `las reservas canceladas o de otra fecha o area no ocupan cupo`() {
        val reservas = listOf(
            reserva(9, personas = 4, estado = EstadoReserva.CANCELADA),
            reserva(9, personas = 4, dia = "2026-10-11"),
            reserva(9, personas = 4, areaId = "otra")
        )

        assertNull(ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "10:00", 4))
    }

    @Test
    fun `las reservas pendientes de aprobacion si ocupan cupo`() {
        val reservas = listOf(reserva(9, personas = 4, estado = EstadoReserva.PENDIENTE_APROBACION))

        assertNotNull(ReglasReserva.validarReserva(cancha, reservas, emptyList(), fecha, "09:00", "10:00", 1))
    }

    @Test
    fun `un torneo bloquea las reservas en sus horas`() {
        val torneos = listOf(torneo())

        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "10:00", "11:00", 1))
        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "11:00", "12:00", 1))
        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "09:00", "11:00", 1))
    }

    @Test
    fun `fuera del horario del torneo si se puede reservar`() {
        val torneos = listOf(torneo())

        assertNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "09:00", "10:00", 1))
        assertNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "12:00", "13:00", 1))
    }

    @Test
    fun `el torneo solo bloquea su area y sus fechas`() {
        val torneos = listOf(torneo(inicio = "2026-10-10", fin = "2026-10-12"))
        val otraArea = cancha.copy(id = "a2")

        assertNull(ReglasReserva.validarReserva(otraArea, emptyList(), torneos, fecha, "10:00", "11:00", 1))
        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, "2026-10-12", "10:00", "11:00", 1))
        assertNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, "2026-10-13", "10:00", "11:00", 1))
    }

    @Test
    fun `un torneo sin horas registradas bloquea el dia completo`() {
        val torneos = listOf(torneo(horaInicio = "", horaFin = ""))

        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), torneos, fecha, "07:00", "08:00", 1))
    }

    @Test
    fun `no se permiten mas horas que el maximo ni areas en mantenimiento`() {
        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), emptyList(), fecha, "06:00", "11:00", 1))
        val enMantenimiento = cancha.copy(disponibilidad = DisponibilidadArea.MANTENIMIENTO)
        assertNotNull(ReglasReserva.validarReserva(enMantenimiento, emptyList(), emptyList(), fecha, "06:00", "07:00", 1))
    }

    @Test
    fun `no se puede reservar para mas personas que la capacidad`() {
        assertNotNull(ReglasReserva.validarReserva(cancha, emptyList(), emptyList(), fecha, "06:00", "07:00", 5))
    }

    @Test
    fun `estadoDeHora cuenta lugares libres`() {
        val estado = ReglasReserva.estadoDeHora(cancha, listOf(reserva(9, personas = 3)), emptyList(), fecha, 9)

        assertEquals(1, estado.libres)
        assertTrue(estado.disponible)
        assertFalse(ReglasReserva.estadoDeHora(cancha, listOf(reserva(9, personas = 4)), emptyList(), fecha, 9).disponible)
    }

    @Test
    fun `un torneo no puede crearse sobre una reserva existente`() {
        val reservas = listOf(reserva(11))

        val error = ReglasReserva.validarTorneo(cancha, reservas, emptyList(), null, fecha, fecha, "10:00", "12:00")

        assertNotNull(error)
        assertTrue(error!!.contains("reserva"))
    }

    @Test
    fun `un torneo si puede crearse si las reservas no chocan con su horario`() {
        val reservas = listOf(reserva(13), reserva(11, estado = EstadoReserva.CANCELADA))

        assertNull(ReglasReserva.validarTorneo(cancha, reservas, emptyList(), null, fecha, fecha, "10:00", "12:00"))
    }

    @Test
    fun `un torneo revisa todos los dias de su rango`() {
        val reservas = listOf(reserva(11, dia = "2026-10-12"))

        val error = ReglasReserva.validarTorneo(cancha, reservas, emptyList(), null, "2026-10-10", "2026-10-14", "10:00", "12:00")

        assertNotNull(error)
        assertTrue(error!!.contains("2026-10-12"))
    }

    @Test
    fun `dos torneos no pueden compartir area y horario pero editar el propio si`() {
        val existente = torneo()

        assertNotNull(ReglasReserva.validarTorneo(cancha, emptyList(), listOf(existente), null, fecha, fecha, "11:00", "13:00"))
        assertNull(ReglasReserva.validarTorneo(cancha, emptyList(), listOf(existente), "t1", fecha, fecha, "11:00", "13:00"))
        assertNull(ReglasReserva.validarTorneo(cancha, emptyList(), listOf(existente), null, fecha, fecha, "12:00", "14:00"))
    }

    @Test
    fun `un torneo con horas o fechas invertidas es invalido`() {
        assertNotNull(ReglasReserva.validarTorneo(cancha, emptyList(), emptyList(), null, fecha, fecha, "12:00", "10:00"))
        assertNotNull(ReglasReserva.validarTorneo(cancha, emptyList(), emptyList(), null, "2026-10-12", "2026-10-10", "10:00", "12:00"))
    }
}
