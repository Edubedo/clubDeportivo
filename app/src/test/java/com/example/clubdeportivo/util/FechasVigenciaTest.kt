package com.example.clubdeportivo.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FechasVigenciaTest {
    @Test
    fun `formato de vigencia siempre muestra fecha calendario explicita`() {
        val localeAnterior = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("jue. 12 nov", Fechas.fechaVigencia("2026-11-12"))
        } finally {
            Locale.setDefault(localeAnterior)
        }
    }

    @Test
    fun `fecha invalida se conserva para no ocultar el valor original`() {
        assertEquals("fecha-invalida", Fechas.fechaVigencia("fecha-invalida"))
    }
}
