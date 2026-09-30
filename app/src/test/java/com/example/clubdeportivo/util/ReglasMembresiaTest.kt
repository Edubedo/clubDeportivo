package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasMembresiaTest {

    private fun membresia(
        estado: EstadoMembresia = EstadoMembresia.ACTIVA,
        inicio: String = "2026-09-01",
        vence: String = "2026-10-01",
        tipo: TipoMembresia = TipoMembresia.INDIVIDUAL
    ) = Membresia("m1", "u1", tipo, PlanIndividual.NORMAL, null, 1800.0, estado, inicio, vence)

    private fun persona(nombre: String = "Ana López", correo: String = "", telefono: String = "", parentesco: String = "Titular") =
        PersonaForm(null, nombre, telefono, correo, parentesco)

    @Test
    fun `los codigos generados tienen el formato correcto y son validos`() {
        repeat(200) {
            val codigo = ReglasMembresia.generarCodigo()
            assertTrue(codigo, ReglasMembresia.esCodigo(codigo))
            assertEquals(10, codigo.length)
        }
    }

    @Test
    fun `los codigos no se repiten en una muestra grande`() {
        val codigos = (1..5000).map { ReglasMembresia.generarCodigo() }
        assertEquals(codigos.size, codigos.toSet().size)
    }

    @Test
    fun `los codigos evitan caracteres que se confunden`() {
        val usados = (1..500).flatMap { ReglasMembresia.generarCodigo().removePrefix("CLB-").toList() }.toSet()
        assertTrue(usados.none { it in "01OIL" })
    }

    @Test
    fun `normalizarCodigo acepta minusculas espacios y guion opcional`() {
        assertEquals("CLB-7K3M9Q", ReglasMembresia.normalizarCodigo("clb-7k3m9q"))
        assertEquals("CLB-7K3M9Q", ReglasMembresia.normalizarCodigo(" CLB 7K3M9Q "))
        assertEquals("CLB-7K3M9Q", ReglasMembresia.normalizarCodigo("clb7k3m9q"))
    }

    @Test
    fun `un correo o un texto cualquiera no es un codigo`() {
        assertFalse(ReglasMembresia.esCodigo("admin@clubdeportivo.com"))
        assertFalse(ReglasMembresia.esCodigo("CLB-123"))
        assertFalse(ReglasMembresia.esCodigo("CLB-0OIL11"))
    }

    @Test
    fun `cada codigo tiene su propio correo interno en minusculas`() {
        assertEquals("clb-7k3m9q@miembros.clubdeportivo.app", ReglasMembresia.emailDeCodigo("CLB-7K3M9Q"))
    }

    @Test
    fun `una membresia activa vencida cuenta como vencida`() {
        assertEquals(EstadoMembresia.VENCIDA, ReglasMembresia.estadoEfectivo(membresia(vence = "2026-09-29"), "2026-09-30"))
        assertEquals(EstadoMembresia.ACTIVA, ReglasMembresia.estadoEfectivo(membresia(vence = "2026-09-30"), "2026-09-30"))
    }

    @Test
    fun `una membresia suspendida sigue suspendida aunque no haya vencido`() {
        assertEquals(EstadoMembresia.SUSPENDIDA, ReglasMembresia.estadoEfectivo(membresia(estado = EstadoMembresia.SUSPENDIDA), "2026-09-15"))
    }

    @Test
    fun `renovar una membresia vigente suma un mes desde su vencimiento`() {
        val (inicio, vence) = ReglasMembresia.fechasDeRenovacion(membresia(inicio = "2026-09-01", vence = "2026-10-01"), "2026-09-20")
        assertEquals("2026-09-01", inicio)
        assertEquals("2026-11-01", vence)
    }

    @Test
    fun `renovar una membresia vencida suma un mes desde hoy`() {
        val (inicio, vence) = ReglasMembresia.fechasDeRenovacion(membresia(vence = "2026-08-01"), "2026-09-20")
        assertEquals("2026-09-20", inicio)
        assertEquals("2026-10-20", vence)
    }

    @Test
    fun `sumar un mes respeta el fin de mes`() {
        assertEquals("2026-02-28", Fechas.sumarMeses("2026-01-31", 1))
    }

    @Test
    fun `la visita vence el mismo dia y las membresias al mes`() {
        assertEquals("2026-09-30", ReglasMembresia.vencimientoInicial(TipoMembresia.VISITA, "2026-09-30"))
        assertEquals("2026-10-30", ReglasMembresia.vencimientoInicial(TipoMembresia.INDIVIDUAL, "2026-09-30"))
    }

    @Test
    fun `los paquetes limitan cuantas personas cubren`() {
        assertEquals(5, ReglasMembresia.maxPersonas(TipoMembresia.FAMILIAR, 1))
        assertEquals(2, ReglasMembresia.maxPersonas(TipoMembresia.FAMILIAR, 2))
        assertEquals(1, ReglasMembresia.maxPersonas(TipoMembresia.INDIVIDUAL, null))
    }

    @Test
    fun `un registro individual valido pasa`() {
        assertNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona())))
    }

    @Test
    fun `el nombre telefono y correo se validan`() {
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona(nombre = "A"))))
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona(telefono = "123"))))
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona(correo = "sin-arroba"))))
        assertNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona(telefono = "312 000 0000", correo = "a@b.co"))))
    }

    @Test
    fun `un paquete familiar necesita al menos dos personas y respeta su maximo`() {
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, listOf(persona())))
        assertNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, listOf(persona(), persona("Luis Pérez", parentesco = "Cónyuge"))))
        val tres = listOf(persona(), persona("Luis Pérez"), persona("Eva Ruiz"))
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, tres))
    }

    @Test
    fun `un individual no admite mas de una persona`() {
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.INDIVIDUAL, null, listOf(persona(), persona("Luis Pérez"))))
    }

    @Test
    fun `la visita entra como visitante y el resto como socio`() {
        assertEquals(Rol.VISITANTE_EXTERNO, ReglasMembresia.rolDeAcceso(TipoMembresia.VISITA))
        assertEquals(Rol.SOCIO, ReglasMembresia.rolDeAcceso(TipoMembresia.FAMILIAR))
    }

    @Test
    fun `los precios editados tienen prioridad sobre los base`() {
        assertEquals(1800.0, ClavesPrecio.de(ClavesPrecio.individual(PlanIndividual.NORMAL), emptyMap()), 0.0)
        assertEquals(2000.0, ClavesPrecio.de(ClavesPrecio.individual(PlanIndividual.NORMAL), mapOf("INDIVIDUAL_NORMAL" to 2000.0)), 0.0)
        assertEquals(200.0, ClavesPrecio.de(ClavesPrecio.VISITA, emptyMap()), 0.0)
        assertEquals(2999.0, ClavesPrecio.de(ClavesPrecio.familiar(2), emptyMap()), 0.0)
    }

    @Test
    fun `el precio de una membresia sale de su tipo y plan`() {
        assertEquals(3500.0, ClavesPrecio.deMembresia(TipoMembresia.INDIVIDUAL, PlanIndividual.DELUXE, null, emptyMap()), 0.0)
        assertEquals(7000.0, ClavesPrecio.deMembresia(TipoMembresia.FAMILIAR, null, 1, emptyMap()), 0.0)
        assertEquals(250.0, ClavesPrecio.deMembresia(TipoMembresia.VISITA, null, null, mapOf("VISITA" to 250.0)), 0.0)
    }
}
