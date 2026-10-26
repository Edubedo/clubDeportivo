package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EntradaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ReglasListaEsperaTest {
    init { com.example.clubdeportivo.TextosDePrueba.instalar() }

    private val fecha = "2030-01-10"
    private val ahora: Date = Fechas.combinar(fecha, "08:00")
    private val prioridadMs = Catalogos.MINUTOS_PRIORIDAD_LISTA_ESPERA * 60_000L

    private fun entrada(
        usuario: String,
        llegada: Long,
        personas: Int = 1,
        estado: EstadoEspera = EstadoEspera.ESPERANDO,
        avisado: Long? = null,
        hora: Int = 10
    ) = EntradaEspera(
        id = ReglasListaEspera.idDe("a1", fecha, hora, usuario), usuarioId = usuario, usuarioNombre = usuario,
        areaId = "a1", areaNombre = "Cancha 1", deporte = "Tenis", fecha = fecha, hora = hora,
        personas = personas, estado = estado, creadoMillis = llegada, avisadoMillis = avisado
    )

    private fun fila(vararg entradas: EntradaEspera) = ReglasListaEspera.fila(entradas.toList(), "a1", fecha, 10, ahora)

    @Test
    fun laFilaVaEnOrdenDeLlegadaYSoloConLasVigentes() {
        val f = fila(
            entrada("c", 300),
            entrada("a", 100),
            entrada("x", 50, estado = EstadoEspera.CANCELADA),
            entrada("y", 60, estado = EstadoEspera.ATENDIDA),
            entrada("b", 200),
            entrada("otraHora", 10, hora = 11)
        )
        assertEquals(listOf("a", "b", "c"), f.map { it.usuarioId })
        assertEquals(2, ReglasListaEspera.posicion("b", f))
        assertNull(ReglasListaEspera.posicion("x", f))
    }

    @Test
    fun elAvisoCaducaDespuesDelTiempoDePrioridad() {
        val reciente = entrada("a", 100, estado = EstadoEspera.AVISADO, avisado = ahora.time - prioridadMs + 60_000)
        val vencida = entrada("b", 200, estado = EstadoEspera.AVISADO, avisado = ahora.time - prioridadMs - 1)
        assertTrue(ReglasListaEspera.vigente(reciente, ahora))
        assertFalse(ReglasListaEspera.vigente(vencida, ahora))
        assertEquals(listOf("a"), fila(reciente, vencida).map { it.usuarioId })
    }

    @Test
    fun unaHoraQueYaEmpezoNoTieneFila() {
        val tarde = Fechas.combinar(fecha, "10:30")
        assertFalse(ReglasListaEspera.vigente(entrada("a", 100), tarde))
    }

    @Test
    fun seAvisaALosPrimerosMientrasAlcancenLosLugares() {
        val f = fila(entrada("a", 100, personas = 2), entrada("b", 200, personas = 1), entrada("c", 300, personas = 1))
        assertEquals(emptyList<String>(), ReglasListaEspera.porAvisar(f, 0).map { it.usuarioId })
        assertEquals(emptyList<String>(), ReglasListaEspera.porAvisar(f, 1).map { it.usuarioId }) // a necesita 2: nadie se le adelanta
        assertEquals(listOf("a"), ReglasListaEspera.porAvisar(f, 2).map { it.usuarioId })
        assertEquals(listOf("a", "b"), ReglasListaEspera.porAvisar(f, 3).map { it.usuarioId })
    }

    @Test
    fun noSeVuelveAAvisarAQuienYaTieneSuLugar() {
        val f = fila(entrada("a", 100, estado = EstadoEspera.AVISADO, avisado = ahora.time), entrada("b", 200))
        assertEquals(emptyList<String>(), ReglasListaEspera.porAvisar(f, 1).map { it.usuarioId })
        assertEquals(listOf("b"), ReglasListaEspera.porAvisar(f, 2).map { it.usuarioId })
    }

    @Test
    fun losLugaresLiberadosQuedanApartadosParaQuienVaAntes() {
        val f = fila(entrada("a", 100, personas = 2), entrada("b", 200, personas = 1))
        // Alguien de fuera de la fila no puede tomar lo que está apartado.
        assertEquals(2, ReglasListaEspera.apartadosPara("fuera", f, 2))
        assertEquals(3, ReglasListaEspera.apartadosPara("fuera", f, 5))
        // El primero de la fila tiene todo disponible; el segundo, lo que deje el primero.
        assertEquals(0, ReglasListaEspera.apartadosPara("a", f, 2))
        assertEquals(2, ReglasListaEspera.apartadosPara("b", f, 2))
        assertEquals(0, ReglasListaEspera.apartadosPara("fuera", emptyList(), 4))
    }
}
