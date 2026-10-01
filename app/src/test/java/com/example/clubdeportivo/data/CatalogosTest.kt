package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.PlanIndividual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los precios del club deben seguir teniendo sentido entre sí: un paquete siempre debe convenir más que comprar planes sueltos. */
class CatalogosTest {

    private val nino = Catalogos.precioPlanIndividual(PlanIndividual.NINO)
    private val normal = Catalogos.precioPlanIndividual(PlanIndividual.NORMAL)
    private val deluxe = Catalogos.precioPlanIndividual(PlanIndividual.DELUXE)

    @Test
    fun `precios definidos por el club`() {
        assertEquals(1500.0, nino, 0.0)
        assertEquals(1800.0, normal, 0.0)
        assertEquals(3500.0, deluxe, 0.0)
        assertEquals(200.0, Catalogos.PRECIO_VISITA, 0.0)
        assertEquals(7000.0, Catalogos.paqueteFamiliarPorId(1)!!.precioMensual, 0.0)
        assertEquals(2999.0, Catalogos.paqueteFamiliarPorId(2)!!.precioMensual, 0.0)
        assertEquals(2800.0, Catalogos.paqueteFamiliarPorId(3)!!.precioMensual, 0.0)
    }

    @Test
    fun `cada plan individual cuesta mas que el anterior`() {
        assertTrue(nino < normal && normal < deluxe)
    }

    @Test
    fun `el paquete familiar conviene frente a comprar planes sueltos`() {
        val familiar = Catalogos.paqueteFamiliarPorId(1)!!
        assertTrue(familiar.precioMensual < 2 * normal + 3 * nino)
    }

    @Test
    fun `el paquete pareja conviene frente a dos planes normales`() {
        assertTrue(Catalogos.paqueteFamiliarPorId(2)!!.precioMensual < 2 * normal)
    }

    @Test
    fun `el paquete ninos conviene frente a planes de nino sueltos`() {
        val paquete = Catalogos.paqueteFamiliarPorId(3)!!
        assertTrue(paquete.precioMensual < paquete.maxIntegrantes * nino)
    }

    @Test
    fun `solo el deluxe incluye prioridad y extras`() {
        assertTrue(Catalogos.beneficiosPlanIndividual(PlanIndividual.DELUXE).size > 1)
        assertEquals(1, Catalogos.beneficiosPlanIndividual(PlanIndividual.NORMAL).size)
    }
}
