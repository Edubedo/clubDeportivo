package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.MotivoSuspension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FechasYMotivosTest {
    // Va primero: los demás campos de la clase pueden usar textos al crearse.
    init { com.example.clubdeportivo.TextosDePrueba.instalar() }


    @Test
    fun `hoy y manana se muestran con palabras`() {
        assertEquals("Hoy", Fechas.legible(Fechas.hoy()))
        assertEquals("Mañana", Fechas.legible(Fechas.sumarDias(1)))
    }

    @Test
    fun `una fecha lejana se muestra con dia y mes`() {
        val texto = Fechas.legible("2031-03-05")
        assertTrue(texto, texto.endsWith("5 mar 2031"))
    }

    @Test
    fun `una fecha mal escrita se devuelve tal cual`() {
        assertEquals("pronto", Fechas.legible("pronto"))
    }

    @Test
    fun `nombreMes entiende el formato de mes del dashboard`() {
        assertEquals("octubre", Fechas.nombreMes("2026-10"))
        assertEquals("ene", Fechas.nombreMes("2026-01", corto = true))
        assertEquals("raro", Fechas.nombreMes("raro"))
    }

    @Test
    fun `permite reservar horarios futuros del mismo dia aunque falte menos de dos horas`() {
        val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .parse("2026-10-21 16:50")!!

        assertTrue(Fechas.horasDesdeAhora("2026-10-21", "17:00", ahora) > 0)
        assertTrue(Fechas.horasDesdeAhora("2026-10-21", "18:00", ahora) > 0)
        assertTrue(Fechas.horasDesdeAhora("2026-10-21", "16:00", ahora) < 0)
    }

    @Test
    fun `los motivos fijos se guardan con su etiqueta y otro con su detalle`() {
        assertEquals("Falta de pago", MotivoSuspension.FALTA_DE_PAGO.textoGuardado("ignorado"))
        assertEquals("Otro: Cambió de ciudad", MotivoSuspension.OTRO.textoGuardado("  Cambió de ciudad "))
    }
}
