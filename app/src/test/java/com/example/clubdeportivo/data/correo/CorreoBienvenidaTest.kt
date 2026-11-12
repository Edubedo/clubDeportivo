package com.example.clubdeportivo.data.correo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorreoBienvenidaTest {
    private val base = "https://clubdeportivoudc.web.app/unirse"

    @Test
    fun `el cuerpo trae remitente destinatario codigo y enlace`() {
        val json = CorreoBienvenida.cuerpoJson("Athletic Club <no-reply@x.com>", "Eduardo Escobedo", "ezescobedo27@gmail.com", "CLB-7K3M9Q", base)
        assertTrue(json.contains("\"from\":\"Athletic Club <no-reply@x.com>\""))
        assertTrue(json.contains("\"to\":[\"ezescobedo27@gmail.com\"]"))
        assertTrue(json.contains("Hola Eduardo"))
        assertTrue(json.contains("$base?codigo=CLB-7K3M9Q"))
        assertTrue(json.contains("\"subject\":\"Tu código de acceso a Athletic Club: CLB-7K3M9Q\""))
    }

    @Test
    fun `un nombre con comillas, saltos o html no rompe el json ni el html`() {
        val json = CorreoBienvenida.cuerpoJson("a <a@b.com>", "<b>\"Ana\"\nRuiz</b>", "a@b.com", "CLB-7K3M9Q", base)
        assertFalse(json.contains('\n'))
        assertFalse(json.contains("<p>Hola <b>"))
        assertTrue(json.contains("&lt;b&gt;"))
    }

    @Test
    fun `el enlace lleva el codigo`() {
        assertEquals("$base?codigo=CLB-7K3M9Q", CorreoBienvenida.enlace(base, "CLB-7K3M9Q"))
    }
}
