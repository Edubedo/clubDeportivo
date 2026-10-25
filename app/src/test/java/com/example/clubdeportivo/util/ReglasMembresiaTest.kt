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
    // Va primero: los demás campos de la clase pueden usar textos al crearse.
    init { com.example.clubdeportivo.TextosDePrueba.instalar() }


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
    fun `normalizarCodigo acepta los 6 caracteres sin el prefijo`() {
        assertEquals("CLB-7K3M9Q", ReglasMembresia.normalizarCodigo("7k3m9q"))
        assertTrue(ReglasMembresia.esCodigo("7K3M9Q"))
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
    fun `un paquete familiar puede registrarse solo con el titular y respeta su maximo`() {
        // El titular da de alta a su familia después, desde la app.
        assertNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, listOf(persona())))
        assertNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, listOf(persona(), persona("Luis Pérez", parentesco = "Cónyuge"))))
        val tres = listOf(persona(), persona("Luis Pérez"), persona("Eva Ruiz"))
        assertNotNull(ReglasMembresia.validarPersonas(TipoMembresia.FAMILIAR, 2, tres))
    }

    @Test
    fun `el titular solo agrega familiares a un paquete familiar con lugar`() {
        val hijo = persona("Luis Pérez", parentesco = "Hijo")
        assertNull(ReglasMembresia.validarFamiliar(hijo, 1, TipoMembresia.FAMILIAR, 1))
        // Pareja: 2 personas, ya está llena con titular + cónyuge.
        assertNotNull(ReglasMembresia.validarFamiliar(hijo, 2, TipoMembresia.FAMILIAR, 2))
        assertNotNull(ReglasMembresia.validarFamiliar(hijo, 1, TipoMembresia.INDIVIDUAL, null))
        assertNotNull(ReglasMembresia.validarFamiliar(persona("Luis Pérez", parentesco = ""), 1, TipoMembresia.FAMILIAR, 1))
        assertNotNull(ReglasMembresia.validarFamiliar(persona("L", parentesco = "Hijo"), 1, TipoMembresia.FAMILIAR, 1))
    }

    @Test
    fun `el enlace de acceso lleva el codigo y se lee de vuelta`() {
        val enlace = ReglasMembresia.enlaceDeAcceso("clb-7k3m9q")
        assertEquals("clubdeportivo://registro?codigo=CLB-7K3M9Q", enlace)
        assertEquals("CLB-7K3M9Q", ReglasMembresia.codigoDeEnlace(enlace))
        assertEquals("CLB-7K3M9Q", ReglasMembresia.codigoDeEnlace("clubdeportivo://registro?codigo=clb-7k3m9q&x=1"))
    }

    @Test
    fun `enlaces ajenos o con codigos invalidos no se aceptan`() {
        assertNull(ReglasMembresia.codigoDeEnlace(null))
        assertNull(ReglasMembresia.codigoDeEnlace("https://ejemplo.com/registro?codigo=CLB-7K3M9Q"))
        assertNull(ReglasMembresia.codigoDeEnlace("clubdeportivo://registro?codigo=CLB-00000I"))
        assertNull(ReglasMembresia.codigoDeEnlace("clubdeportivo://registro"))
    }

    @Test
    fun `el mensaje de invitacion trae el enlace y el codigo`() {
        val mensaje = ReglasMembresia.mensajeDeInvitacion("Luis Pérez", "Ana Ruiz", "CLB-7K3M9Q")
        assertTrue(mensaje.contains("clubdeportivo://registro?codigo=CLB-7K3M9Q"))
        assertTrue(mensaje.contains("Hola Luis"))
        assertTrue(mensaje.contains("Ana Ruiz"))
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
