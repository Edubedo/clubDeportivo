package com.example.clubdeportivo.util

import com.example.clubdeportivo.TextosDePrueba
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReconciliacionRenovacionTest {
    @Before
    fun instalarTextos() {
        TextosDePrueba.instalar()
    }

    @Test
    fun `aprobacion vigente reconcilia membresia vencida para tarjeta y reservas`() {
        val result = reconciliarRenovacionAprobada(
            membresia(EstadoMembresia.ACTIVA, "2026-11-11"),
            solicitud(reviewedDate = "2026-11-12"),
            hoy = "2026-11-12"
        )

        assertTrue(result.renovacionAprobadaVigente)
        assertEquals(EstadoMembresia.ACTIVA, result.membresia?.estado)
        assertEquals("2026-12-12", result.membresia?.fechaVencimiento)
    }

    @Test
    fun `no suma otro mes si Firestore ya guardo la vigencia renovada`() {
        val result = reconciliarRenovacionAprobada(
            membresia(EstadoMembresia.ACTIVA, "2026-12-12"),
            solicitud(reviewedDate = "2026-11-12"),
            hoy = "2026-11-12"
        )

        assertTrue(result.renovacionAprobadaVigente)
        assertEquals("2026-12-12", result.membresia?.fechaVencimiento)
    }

    @Test
    fun `la fecha de vigencia persistida en la solicitud es la autoritativa`() {
        val result = reconciliarRenovacionAprobada(
            membresia(EstadoMembresia.ACTIVA, "2026-11-11"),
            solicitud(reviewedDate = "2026-11-12", renewedUntil = "2026-12-20"),
            hoy = "2026-11-12"
        )

        assertTrue(result.renovacionAprobadaVigente)
        assertEquals("2026-12-20", result.membresia?.fechaVencimiento)
    }

    @Test
    fun `aprobacion vencida no impide una renovacion nueva`() {
        val result = reconciliarRenovacionAprobada(
            membresia(EstadoMembresia.ACTIVA, "2026-10-01"),
            solicitud(reviewedDate = "2026-09-01"),
            hoy = "2026-11-12"
        )

        assertFalse(result.renovacionAprobadaVigente)
        assertEquals(
            EstadoMembresia.VENCIDA,
            result.membresia?.let { ReglasMembresia.estadoEfectivo(it, "2026-11-12") }
        )
    }

    @Test
    fun `aprobacion de otra membresia no altera la tarjeta`() {
        val membership = membresia(EstadoMembresia.ACTIVA, "2026-10-01")
        val result = reconciliarRenovacionAprobada(
            membership,
            solicitud(membresiaId = "otra", reviewedDate = "2026-11-12"),
            hoy = "2026-11-12"
        )

        assertFalse(result.renovacionAprobadaVigente)
        assertEquals(membership, result.membresia)
    }

    private fun membresia(estado: EstadoMembresia, vencimiento: String) = Membresia(
        id = "m1",
        usuarioId = "u1",
        tipo = TipoMembresia.INDIVIDUAL,
        plan = PlanIndividual.NORMAL,
        precio = 1800.0,
        estado = estado,
        fechaInicio = "2026-10-11",
        fechaVencimiento = vencimiento
    )

    private fun solicitud(
        membresiaId: String = "m1",
        reviewedDate: String,
        renewedUntil: String = ""
    ) = SolicitudRenovacion(
        id = "r1",
        socioId = "u1",
        socioNombre = "Socio",
        membresiaId = membresiaId,
        planId = "INDIVIDUAL_NORMAL",
        monto = 1800.0,
        concepto = "RENOVACION",
        comprobanteUrl = "",
        status = SolicitudRenovacion.APROBADO,
        createdAt = 1L,
        reviewedDate = reviewedDate,
        renewedUntil = renewedUntil
    )
}
