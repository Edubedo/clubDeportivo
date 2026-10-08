package com.example.clubdeportivo

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.ui.admin.home.OperacionHoy
import com.example.clubdeportivo.ui.admin.home.PanelAdmin
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ResumenAdmin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * No comprueba nada: dibuja pantallas con datos de ejemplo y guarda capturas en la carpeta de archivos de la app,
 * para poder revisar el diseño sin iniciar sesión ni tocar Firebase.
 */
@RunWith(AndroidJUnit4::class)
class CapturasPantallasTest {

    @get:Rule
    val regla = createComposeRule()

    private fun guardar(nombre: String) {
        val imagen = regla.onRoot().captureToImage().asAndroidBitmap()
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        File(contexto.filesDir, "$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun membresia(id: String, nombre: String, precio: Double, vence: String) = MembresiaDetalle(
        Membresia(id, "u$id", TipoMembresia.INDIVIDUAL, PlanIndividual.DELUXE, null, precio, EstadoMembresia.ACTIVA, Fechas.hoy(), vence),
        listOf(MiembroClub("CLB-AB2CD3", nombre, "", "", id, MiembroClub.PARENTESCO_TITULAR, "u$id"))
    )

    @Test
    fun dashboardDelAdministrador() {
        val mes = Fechas.hoy().take(7)
        val resumen = ResumenAdmin(
            mes = mes,
            ingresosMes = 38400.0,
            ingresosMesAnterior = 31250.0,
            cobrosMes = 19,
            ingresosPorMes = listOf("2026-05" to 18000.0, "2026-06" to 22500.0, "2026-07" to 27900.0, "2026-08" to 25100.0, "2026-09" to 31250.0, mes to 38400.0),
            ingresosPorMetodo = listOf(MetodoPago.TARJETA to 21000.0, MetodoPago.EFECTIVO to 13400.0, MetodoPago.TRANSFERENCIA to 4000.0),
            activas = 34,
            personasActivas = 71,
            vencidas = 3,
            suspendidas = 2,
            altasMes = 6,
            porVencer = listOf(
                membresia("1", "Mariana López Hernández", 3500.0, Fechas.hoy()),
                membresia("2", "Carlos Ibarra", 1800.0, Fechas.sumarDias(2)),
                membresia("3", "Familia Ortega Ruiz", 7000.0, Fechas.sumarDias(5))
            )
        )
        val reservas = listOf(
            Reserva("r1", "u1", "1", Fechas.hoy(), "18:00", "19:00", EstadoReserva.CONFIRMADA, usuarioNombre = "Ana García", areaNombre = "Cancha de fútbol 1", deporte = "Fútbol"),
            Reserva("r2", "u2", "7", Fechas.sumarDias(1), "09:00", "11:00", EstadoReserva.PENDIENTE_APROBACION, usuarioNombre = "Luis Pérez", areaNombre = "Cancha de tenis 1", deporte = "Tenis")
        )

        regla.setContent {
            ClubDeportivoTheme {
                Box(Modifier.fillMaxSize().background(FondoApp)) {
                    PanelAdmin(
                        resumen = resumen,
                        operacion = OperacionHoy(12, 2, 8, 1, 3, 5, 2),
                        proximasReservas = reservas,
                        cargando = false,
                        errorFinanzas = false,
                        onActualizar = {}
                    )
                }
            }
        }
        regla.waitForIdle()
        guardar("dash0")
        repeat(4) { indice ->
            regla.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.15f, durationMillis = 400) }
            regla.waitForIdle()
            guardar("dash${indice + 1}")
        }
    }
}
