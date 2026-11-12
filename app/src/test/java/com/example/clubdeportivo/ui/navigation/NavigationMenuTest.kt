package com.example.clubdeportivo.ui.navigation

import com.example.clubdeportivo.data.model.Rol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationMenuTest {
    @Test
    fun `perfil no aparece en el menu ni es ruta de notificacion para ningun rol`() {
        Rol.entries.forEach { rol ->
            assertFalse(menuPara(rol).any { it.route == Destinations.PERFIL })
            assertTrue(rutaPermitida(rol, Destinations.PERFIL))
        }
    }

    @Test
    fun `reportes no es pestana pero sigue disponible para administradores`() {
        Rol.entries.forEach { rol ->
            val esAdmin = rol == Rol.ADMIN || rol == Rol.SUPERADMIN
            assertFalse(menuPara(rol).any { it.route == Destinations.REPORTES })
            assertTrue(rutaPermitida(rol, Destinations.REPORTES) == esAdmin)
        }
    }

    @Test
    fun `el encargado llega al inventario desde su menu`() {
        assertTrue(menuPara(Rol.AYUDANTE_AREA).any { it.route == Destinations.INVENTARIO })
        assertTrue(rutaPermitida(Rol.AYUDANTE_AREA, Destinations.INVENTARIO))
        assertFalse(rutaPermitida(Rol.SOCIO, Destinations.INVENTARIO))
    }

    @Test
    fun `el supervisor registra miembros desde membresias sin tenerla como pestana`() {
        assertFalse(menuPara(Rol.SUPERVISOR).any { it.route == Destinations.MEMBRESIA })
        assertTrue(rutaPermitida(Rol.SUPERVISOR, Destinations.MEMBRESIA))
        assertFalse(rutaPermitida(Rol.AYUDANTE_AREA, Destinations.MEMBRESIA))
    }
}
