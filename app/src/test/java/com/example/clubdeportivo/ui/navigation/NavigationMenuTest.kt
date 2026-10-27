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
}
