package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.data.model.puedeVerDeporte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesTest {

    private fun usuario(rol: Rol, area: String? = null) = Usuario("u", "Nombre", "a@b.c", rol, areaTrabajo = area)

    @Test
    fun `solo existen tres nombres de rol para las personas`() {
        assertEquals("Administrador", Rol.ADMIN.nombreLegible())
        assertEquals("Encargado", Rol.AYUDANTE_AREA.nombreLegible())
        assertEquals("Encargado", Rol.ADMIN_AREA.nombreLegible())
        assertEquals("Miembro", Rol.SOCIO.nombreLegible())
        assertFalse(Rol.entries.any { it.nombreLegible().contains("apoyo", ignoreCase = true) })
    }

    @Test
    fun `el encargado solo ve el deporte de su area`() {
        val encargado = usuario(Rol.AYUDANTE_AREA, "Tenis")
        assertTrue(encargado.rol.esEncargado())
        assertTrue(encargado.puedeVerDeporte("Tenis"))
        assertTrue(encargado.puedeVerDeporte(" tenis "))
        assertFalse(encargado.puedeVerDeporte("Fútbol"))
        assertFalse(encargado.puedeVerDeporte(""))
    }

    @Test
    fun `un encargado sin area no ve nada`() {
        assertFalse(usuario(Rol.AYUDANTE_AREA, null).puedeVerDeporte("Tenis"))
        assertFalse(usuario(Rol.AYUDANTE_AREA, "  ").puedeVerDeporte("Tenis"))
    }

    @Test
    fun `administrador y miembros no se limitan por area`() {
        assertTrue(usuario(Rol.ADMIN).puedeVerDeporte("Fútbol"))
        assertTrue(usuario(Rol.SOCIO).puedeVerDeporte("Fútbol"))
        assertTrue((null as Usuario?).puedeVerDeporte("Fútbol"))
    }
}
