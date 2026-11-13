package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.TextosDePrueba
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FirebaseMembresiaRepositoryTest {
    @Before
    fun instalarTextos() {
        TextosDePrueba.instalar()
    }

    @Test
    fun `prefiere la membresia activa frente a una suspendida vinculada a la misma cuenta`() {
        val suspendida = membresia(
            id = "suspendida",
            estado = EstadoMembresia.SUSPENDIDA,
            fechaInicio = "2026-10-01"
        )
        val renovada = membresia(
            id = "renovada",
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = "2026-09-01",
            fechaVencimiento = "2026-12-01"
        )

        assertEquals(renovada, seleccionarMembresia(listOf(suspendida, renovada), "2026-11-12"))
    }

    @Test
    fun `si no hay membresias activas prefiere la mas reciente`() {
        val anterior = membresia(
            id = "anterior",
            estado = EstadoMembresia.SUSPENDIDA,
            fechaInicio = "2026-08-01"
        )
        val reciente = membresia(
            id = "reciente",
            estado = EstadoMembresia.SUSPENDIDA,
            fechaInicio = "2026-10-01"
        )

        assertEquals(reciente, seleccionarMembresia(listOf(anterior, reciente), "2026-11-12"))
    }

    @Test
    fun `devuelve null cuando no hay membresias`() {
        assertNull(seleccionarMembresia(emptyList(), "2026-11-12"))
    }

    private fun membresia(
        id: String,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String = "2027-01-01"
    ) = Membresia(
        id = id,
        usuarioId = "usuario",
        tipo = TipoMembresia.INDIVIDUAL,
        plan = PlanIndividual.NORMAL,
        precio = 1800.0,
        estado = estado,
        fechaInicio = fechaInicio,
        fechaVencimiento = fechaVencimiento
    )
}
