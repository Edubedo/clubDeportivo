package com.example.clubdeportivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasContrasenaTest {
    // Va primero: los demás campos de la clase pueden usar textos al crearse.
    init { com.example.clubdeportivo.TextosDePrueba.instalar() }


    @Test
    fun contrasenaCompletaEsValida() {
        assertTrue(ReglasContrasena.esValida("Club#2026"))
        assertNull(ReglasContrasena.primerError("Club#2026"))
    }

    @Test
    fun rechazaMenosDeOchoCaracteres() {
        assertFalse(ReglasContrasena.esValida("Ab#1xyz"))
        assertEquals("La contraseña debe tener al menos 8 caracteres.", ReglasContrasena.primerError("Ab#1xyz"))
    }

    @Test
    fun exigeMayusculaMinusculaNumeroYEspecial() {
        assertNotNull(ReglasContrasena.primerError("club#2026"))
        assertNotNull(ReglasContrasena.primerError("CLUB#2026"))
        assertNotNull(ReglasContrasena.primerError("Club#Atleta"))
        assertNotNull(ReglasContrasena.primerError("Club2026x"))
    }

    @Test
    fun rechazaEspacios() {
        assertFalse(ReglasContrasena.esValida("Club #2026"))
    }

    @Test
    fun vaciaNoEsValidaYTodosLosRequisitosFallan() {
        assertFalse(ReglasContrasena.esValida(""))
        assertTrue(ReglasContrasena.requisitos("").none { it.cumple })
    }

    @Test
    fun cambiarContrasenaPideLaActualUnaNuevaValidaDistintaYConfirmada() {
        assertNotNull(ReglasContrasena.errorAlCambiar("", "Club#2026", "Club#2026"))
        assertNotNull(ReglasContrasena.errorAlCambiar("Vieja#2025", "corta", "corta"))
        assertEquals(
            "La contraseña nueva debe ser distinta de la actual.",
            ReglasContrasena.errorAlCambiar("Club#2026", "Club#2026", "Club#2026")
        )
        assertEquals(
            "Las contraseñas nuevas no coinciden.",
            ReglasContrasena.errorAlCambiar("Vieja#2025", "Club#2026", "Club#2027")
        )
        assertNull(ReglasContrasena.errorAlCambiar("Vieja#2025", "Club#2026", "Club#2026"))
    }
}
