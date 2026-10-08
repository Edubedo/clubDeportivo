package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.PagoClub
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumenAdminTest {

    private val hoy = Fechas.hoy()
    private val mesActual = hoy.take(7)

    private fun membresia(
        id: String,
        precio: Double,
        estado: EstadoMembresia = EstadoMembresia.ACTIVA,
        vence: String = Fechas.sumarMeses(hoy, 1),
        tipo: TipoMembresia = TipoMembresia.INDIVIDUAL
    ) = MembresiaDetalle(
        Membresia(id, "u$id", tipo, PlanIndividual.NORMAL, null, precio, estado, hoy, vence),
        emptyList()
    )

    private fun pago(monto: Double, fecha: String, concepto: ConceptoPago = ConceptoPago.MENSUALIDAD, metodo: MetodoPago? = MetodoPago.EFECTIVO) =
        PagoClub("p", "m", "Alguien", concepto, monto, metodo, fecha)

    @Test
    fun `los ultimos meses terminan en el mes actual y van en orden`() {
        val meses = CalculoResumenAdmin.ultimosMeses("2026-02-15", 4)
        assertEquals(listOf("2025-11", "2025-12", "2026-01", "2026-02"), meses)
    }

    @Test
    fun `suma los ingresos del mes y los compara con el anterior`() {
        val mesAnterior = CalculoResumenAdmin.ultimosMeses(hoy).let { it[it.size - 2] }
        val pagos = listOf(
            pago(1000.0, "$mesActual-01"),
            pago(500.0, "$mesActual-02", ConceptoPago.ALTA, MetodoPago.TARJETA),
            pago(1000.0, "$mesAnterior-10")
        )

        val resumen = CalculoResumenAdmin.calcular(emptyList(), pagos, hoy)

        assertEquals(1500.0, resumen.ingresosMes, 0.001)
        assertEquals(1000.0, resumen.ingresosMesAnterior, 0.001)
        assertEquals(2, resumen.cobrosMes)
        assertEquals(1, resumen.altasMes)
        assertEquals(50.0, resumen.variacionContraMesAnterior!!, 0.001)
    }

    @Test
    fun `sin ingresos el mes anterior no hay variacion que mostrar`() {
        val resumen = CalculoResumenAdmin.calcular(emptyList(), listOf(pago(300.0, "$mesActual-01")), hoy)
        assertNull(resumen.variacionContraMesAnterior)
    }

    @Test
    fun `las activas no cuentan visitas, suspendidas ni vencidas por fecha`() {
        val membresias = listOf(
            membresia("1", 1800.0),
            membresia("2", 3500.0),
            membresia("3", 1500.0, EstadoMembresia.SUSPENDIDA),
            membresia("4", 200.0, tipo = TipoMembresia.VISITA, vence = hoy),
            membresia("5", 1800.0, vence = Fechas.sumarDias(-3)) // vencida por fecha
        )

        val resumen = CalculoResumenAdmin.calcular(membresias, emptyList(), hoy)

        assertEquals(2, resumen.activas)
        assertEquals(1, resumen.suspendidas)
        assertEquals(1, resumen.vencidas)
    }

    @Test
    fun `por vencer lista solo las activas que vencen en la semana, la mas proxima primero`() {
        val membresias = listOf(
            membresia("lejos", 1800.0, vence = Fechas.sumarDias(30)),
            membresia("manana", 1800.0, vence = Fechas.sumarDias(1)),
            membresia("hoy", 1800.0, vence = hoy),
            membresia("suspendida", 1800.0, EstadoMembresia.SUSPENDIDA, vence = Fechas.sumarDias(2))
        )

        val porVencer = CalculoResumenAdmin.calcular(membresias, emptyList(), hoy).porVencer

        assertEquals(listOf("hoy", "manana"), porVencer.map { it.membresia.id })
    }
}
