package com.example.clubdeportivo.notificaciones

import com.example.clubdeportivo.TextosDePrueba
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.util.Fechas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasRecordatoriosTest {
    init { TextosDePrueba.instalar() }

    private val hoy = "2026-10-25"
    private fun a(hora: String) = Fechas.combinar(hoy, hora)
    private val todoNuevo: (String) -> Boolean = { true }

    private fun reserva(id: String = "r1", inicio: String = "11:00", estado: EstadoReserva = EstadoReserva.CONFIRMADA, fecha: String = hoy, nombre: String = "Ana") =
        Reserva(id, "u1", "a1", fecha, inicio, "12:00", estado, usuarioNombre = nombre, areaNombre = "Cancha 1", deporte = "Tenis")

    private fun membresia(vence: String, estado: EstadoMembresia = EstadoMembresia.ACTIVA, motivo: String = "") =
        Membresia("m1", "u1", TipoMembresia.INDIVIDUAL, null, null, 1500.0, estado, "2026-09-01", vence, motivo)

    // ---- Tipos de usuario y categorías

    @Test
    fun `cada tipo de usuario recibe solo sus categorias`() {
        val cliente = CategoriaNotificacion.de(TipoUsuario.CLIENTE)
        val encargado = CategoriaNotificacion.de(TipoUsuario.ENCARGADO)
        val admin = CategoriaNotificacion.de(TipoUsuario.ADMINISTRADOR)
        assertTrue(CategoriaNotificacion.RESERVA_PROXIMA in cliente && CategoriaNotificacion.MEMBRESIA in cliente)
        assertFalse(CategoriaNotificacion.SOLICITUDES in cliente || CategoriaNotificacion.STOCK in cliente)
        assertTrue(CategoriaNotificacion.ASISTENCIA in encargado && CategoriaNotificacion.SOLICITUDES in encargado)
        assertFalse(CategoriaNotificacion.VENCIMIENTOS in encargado || CategoriaNotificacion.MEMBRESIA in encargado)
        assertTrue(CategoriaNotificacion.VENCIMIENTOS in admin && CategoriaNotificacion.SOLICITUDES in admin)
        assertFalse(CategoriaNotificacion.ASISTENCIA in admin || CategoriaNotificacion.RESERVA_PROXIMA in admin)
        listOf(cliente, encargado, admin).forEach { assertTrue(CategoriaNotificacion.AVISOS in it && CategoriaNotificacion.TORNEO in it) }
    }

    @Test
    fun `el rol decide el tipo de usuario`() {
        assertEquals(TipoUsuario.CLIENTE, TipoUsuario.de(Rol.SOCIO))
        assertEquals(TipoUsuario.CLIENTE, TipoUsuario.de(Rol.VISITANTE_EXTERNO))
        assertEquals(TipoUsuario.ENCARGADO, TipoUsuario.de(Rol.AYUDANTE_AREA))
        assertEquals(TipoUsuario.ENCARGADO, TipoUsuario.de(Rol.ADMIN_AREA))
        assertEquals(TipoUsuario.ADMINISTRADOR, TipoUsuario.de(Rol.ADMIN))
        assertEquals(TipoUsuario.ADMINISTRADOR, TipoUsuario.de(Rol.SUPERADMIN))
    }

    // ---- Horas de silencio

    @Test
    fun `de noche no se molesta`() {
        assertTrue(ReglasRecordatorios.enSilencio(a("22:00")))
        assertTrue(ReglasRecordatorios.enSilencio(a("03:30")))
        assertFalse(ReglasRecordatorios.enSilencio(a("05:00")))
        assertFalse(ReglasRecordatorios.enSilencio(a("21:59")))
    }

    // ---- Reservas

    @Test
    fun `una reserva se recuerda 2 horas y 1 hora antes`() {
        val dosHoras = ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "15:00")), a("13:00"), todoNuevo)
        assertEquals(1, dosHoras.size)
        assertEquals(CategoriaNotificacion.RESERVA_PROXIMA, dosHoras.first().categoria)
        assertEquals(listOf("reserva:r1:120"), dosHoras.first().claves)
        assertTrue(dosHoras.first().mensaje.contains("2 h"))

        val unaHora = ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "15:00")), a("14:00"), todoNuevo)
        assertEquals(listOf("reserva:r1:60"), unaHora.first().claves)
        assertTrue(unaHora.first().mensaje.contains("1 h"))

        // Si la revisión cae entre los dos avisos, sale el de 2 h con el tiempo real que falta.
        val entre = ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "15:00")), a("13:35"), todoNuevo)
        assertEquals(listOf("reserva:r1:120"), entre.first().claves)
        assertTrue(entre.first().mensaje.contains("1 h 25 min"))

        val pocoAntes = ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "11:00")), a("10:20"), todoNuevo)
        assertTrue(pocoAntes.first().mensaje.contains("40 min"))
    }

    @Test
    fun `no se recuerda una reserva lejana, que ya empieza, no confirmada o ya avisada`() {
        assertTrue(ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "13:00")), a("10:00"), todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "09:00")), a("10:00"), todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "15:00")), a("14:55"), todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.reservasProximas(listOf(reserva(estado = EstadoReserva.PENDIENTE_APROBACION)), a("10:30"), todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.reservasProximas(listOf(reserva(inicio = "11:00")), a("10:30")) { false }.isEmpty())
    }

    @Test
    fun `se programan alarmas a las 2 horas y a la hora antes de la reserva`() {
        val inicio = a("15:00").time
        assertEquals(
            listOf(inicio - 120 * 60_000L, inicio - 60 * 60_000L),
            ReglasRecordatorios.momentosDeAviso(reserva(inicio = "15:00"), a("12:00"))
        )
        assertEquals(listOf(inicio - 60 * 60_000L), ReglasRecordatorios.momentosDeAviso(reserva(inicio = "15:00"), a("13:30")))
        assertTrue(ReglasRecordatorios.momentosDeAviso(reserva(estado = EstadoReserva.CANCELADA), a("08:00")).isEmpty())
    }

    @Test
    fun `se avisa cuando una solicitud en revision se aprueba o se rechaza`() {
        val previos = mapOf("r1" to "PENDIENTE_APROBACION", "r2" to "PENDIENTE_APROBACION", "r3" to "CONFIRMADA")
        val reservas = listOf(
            reserva("r1", estado = EstadoReserva.CONFIRMADA),
            reserva("r2", estado = EstadoReserva.RECHAZADA),
            reserva("r3", estado = EstadoReserva.CONFIRMADA),
            reserva("r4", estado = EstadoReserva.CONFIRMADA)
        )
        val r = ReglasRecordatorios.respuestasDeReserva(reservas, previos)
        assertEquals(listOf("respuesta:r1:ok", "respuesta:r2:no"), r.map { it.claves.first() })
    }

    // ---- Membresía

    @Test
    fun `la membresia avisa a 3 dias, manana y hoy`() {
        assertEquals(1, ReglasRecordatorios.membresia(membresia("2026-10-28"), hoy, todoNuevo).size)
        val manana = ReglasRecordatorios.membresia(membresia("2026-10-26"), hoy, todoNuevo).single()
        assertTrue(manana.mensaje.contains("mañana"))
        val vence = ReglasRecordatorios.membresia(membresia(hoy), hoy, todoNuevo).single()
        assertEquals("Tu membresía vence hoy", vence.titulo)
    }

    @Test
    fun `la membresia lejana, ausente o ya avisada no avisa`() {
        assertTrue(ReglasRecordatorios.membresia(membresia("2026-11-30"), hoy, todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.membresia(null, hoy, todoNuevo).isEmpty())
        assertTrue(ReglasRecordatorios.membresia(membresia("2026-10-27"), hoy) { false }.isEmpty())
    }

    @Test
    fun `una membresia suspendida avisa con su motivo`() {
        val r = ReglasRecordatorios.membresia(membresia("2026-12-01", EstadoMembresia.SUSPENDIDA, "Falta de pago"), hoy, todoNuevo).single()
        assertTrue(r.mensaje.contains("Falta de pago"))
    }

    // ---- Torneos y avisos

    @Test
    fun `solo se avisa de los torneos que empiezan manana`() {
        val torneos = listOf(
            Torneo("t1", "Copa Otoño", "Tenis", "a1", "2026-10-26", "2026-10-27", 16, 3, "09:00", "14:00"),
            Torneo("t2", "Liga", "Tenis", "a1", "2026-10-30", "2026-11-02", 16, 3)
        )
        val cliente = ReglasRecordatorios.torneosDeManana(torneos, hoy, comoCliente = true, nuevo = todoNuevo).single()
        assertTrue(cliente.titulo.contains("Copa Otoño") && cliente.mensaje.contains("09:00"))
        val personal = ReglasRecordatorios.torneosDeManana(torneos, hoy, comoCliente = false, nuevo = todoNuevo).single()
        assertTrue(personal.mensaje.contains("3 de 16"))
    }

    @Test
    fun `solo se avisa de avisos nuevos que no escribio la propia persona`() {
        val avisos = listOf(
            Notificacion("n1", "Viejo", "", DestinatarioNotificacion.TODOS, autorId = "x", fechaMillis = 100),
            Notificacion("n2", "Nuevo", "", DestinatarioNotificacion.TODOS, autorId = "x", fechaMillis = 300),
            Notificacion("n3", "Mio", "", DestinatarioNotificacion.TODOS, autorId = "yo", fechaMillis = 400)
        )
        val r = ReglasRecordatorios.avisos(avisos, desdeMillis = 200, miId = "yo", nuevo = todoNuevo)
        assertEquals(listOf("aviso:n2"), r.map { it.claves.first() })
    }

    // ---- Encargados y administradores

    @Test
    fun `las solicitudes en revision se resumen en una sola notificacion y no se repiten`() {
        val pendientes = listOf(reserva("r1", estado = EstadoReserva.PENDIENTE_APROBACION), reserva("r2", estado = EstadoReserva.PENDIENTE_APROBACION), reserva("r3"))
        val r = ReglasRecordatorios.solicitudes(pendientes, hoy, todoNuevo)!!
        assertEquals(2, r.claves.size)
        assertTrue(r.mensaje.contains("2 solicitudes"))
        assertNull(ReglasRecordatorios.solicitudes(pendientes, hoy) { false })
        assertTrue(ReglasRecordatorios.solicitudes(listOf(pendientes[0]), hoy, todoNuevo)!!.mensaje.contains("Ana"))
    }

    @Test
    fun `la asistencia se recuerda para reservas que empezaron hace poco y no tienen registro`() {
        val reservas = listOf(reserva("r1", inicio = "10:00"), reserva("r2", inicio = "10:30"), reserva("r3", inicio = "06:00"), reserva("r4", inicio = "15:00"))
        val r = ReglasRecordatorios.asistencias(reservas, conAsistencia = setOf("r2"), ahora = a("11:00"), nuevo = todoNuevo)!!
        assertEquals(listOf("asistencia:r1"), r.claves)
        assertNull(ReglasRecordatorios.asistencias(reservas, setOf("r1", "r2"), a("11:00"), todoNuevo))
    }

    @Test
    fun `el stock bajo es un resumen diario que no sale antes de las 8 ni se repite`() {
        val articulos = listOf(
            ArticuloInventario("1", "Raquetas", "Tenis", 2, "🎾", 5),
            ArticuloInventario("2", "Pelotas", "Tenis", 50, "🎾", 10),
            ArticuloInventario("3", "Conos", "Fútbol", 0, "📦", 0)
        )
        assertNull(ReglasRecordatorios.stockBajo(articulos, hoy, a("07:30"), todoNuevo))
        val r = ReglasRecordatorios.stockBajo(articulos, hoy, a("09:00"), todoNuevo)!!
        assertTrue(r.mensaje.contains("Raquetas"))
        assertNull(ReglasRecordatorios.stockBajo(articulos, hoy, a("09:00")) { false })
    }

    @Test
    fun `el administrador recibe un resumen de membresias por vencer`() {
        val ms = listOf(membresia("2026-10-27"), membresia(hoy), membresia("2026-12-30"), membresia("2026-10-28", EstadoMembresia.SUSPENDIDA))
        assertNull(ReglasRecordatorios.vencimientos(ms, hoy, a("07:00"), todoNuevo))
        val r = ReglasRecordatorios.vencimientos(ms, hoy, a("09:00"), todoNuevo)!!
        assertTrue(r.mensaje.contains("2 membresías") && r.mensaje.contains("1 hoy"))
        assertNotNull(r)
    }
}
