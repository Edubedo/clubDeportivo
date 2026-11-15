package com.example.clubdeportivo.util

import com.example.clubdeportivo.TextosDePrueba
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.SolicitudCambioPlan
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasCambioPlanTest {
    init { TextosDePrueba.instalar() }

    private val hoy = "2026-11-15"

    private fun membresia(
        plan: PlanIndividual? = PlanIndividual.NORMAL,
        tipo: TipoMembresia = TipoMembresia.INDIVIDUAL,
        estado: EstadoMembresia = EstadoMembresia.ACTIVA,
        vence: String = "2026-12-01"
    ) = Membresia(
        id = "m1", usuarioId = "u1", tipo = tipo, plan = plan, precio = 1800.0, estado = estado,
        fechaInicio = "2026-11-01", fechaVencimiento = vence
    )

    private fun solicitud(
        actual: PlanIndividual = PlanIndividual.NORMAL,
        nuevo: PlanIndividual = PlanIndividual.DELUXE,
        status: String = SolicitudCambioPlan.PENDIENTE
    ) = SolicitudCambioPlan(
        id = "s1", socioId = "u1", socioNombre = "Socio", membresiaId = "m1", planActual = actual, planNuevo = nuevo,
        precioActual = 1800.0, precioNuevo = 3500.0, status = status, createdAt = 1L
    )

    private fun puede(m: Membresia?, nuevo: PlanIndividual, cambio: Boolean = false, renov: Boolean = false) =
        ReglasCambioPlan.motivoNoPuedePedir(m, hoy, nuevo, cambio, renov)

    @Test
    fun losPlanesDisponiblesExcluyenElActual() {
        assertEquals(listOf(PlanIndividual.NINO, PlanIndividual.DELUXE), ReglasCambioPlan.planesDisponibles(PlanIndividual.NORMAL))
        assertEquals(listOf(PlanIndividual.NORMAL, PlanIndividual.DELUXE), ReglasCambioPlan.planesDisponibles(PlanIndividual.NINO))
        assertEquals(listOf(PlanIndividual.NINO, PlanIndividual.NORMAL), ReglasCambioPlan.planesDisponibles(PlanIndividual.DELUXE))
    }

    @Test
    fun mejoraYReduccionSegunElNivel() {
        assertTrue(ReglasCambioPlan.esMejora(PlanIndividual.NINO, PlanIndividual.NORMAL))
        assertTrue(ReglasCambioPlan.esMejora(PlanIndividual.NORMAL, PlanIndividual.DELUXE))
        assertTrue(ReglasCambioPlan.esMejora(PlanIndividual.NINO, PlanIndividual.DELUXE))
        assertFalse(ReglasCambioPlan.esMejora(PlanIndividual.DELUXE, PlanIndividual.NORMAL))
        assertFalse(ReglasCambioPlan.esMejora(PlanIndividual.NORMAL, PlanIndividual.NINO))
        assertTrue(solicitud(PlanIndividual.NORMAL, PlanIndividual.DELUXE).esMejora)
        assertFalse(solicitud(PlanIndividual.DELUXE, PlanIndividual.NINO).esMejora)
    }

    @Test
    fun laDiferenciaEsPositivaSiSube() {
        assertEquals(1700.0, ReglasCambioPlan.diferencia(1800.0, 3500.0), 0.001)
        assertEquals(-300.0, ReglasCambioPlan.diferencia(1800.0, 1500.0), 0.001)
    }

    @Test
    fun unMiembroActivoPuedePedirUnPlanDistinto() {
        PlanIndividual.entries.forEach { actual ->
            ReglasCambioPlan.planesDisponibles(actual).forEach { nuevo ->
                assertNull("$actual -> $nuevo", puede(membresia(actual), nuevo))
            }
        }
    }

    @Test
    fun noPuedePedirElMismoPlan() {
        assertNotNull(puede(membresia(PlanIndividual.NORMAL), PlanIndividual.NORMAL))
    }

    @Test
    fun soloLasMembresiasIndividualesCambianDesdeLaApp() {
        assertNotNull(puede(membresia(plan = null, tipo = TipoMembresia.FAMILIAR), PlanIndividual.DELUXE))
        assertNotNull(puede(membresia(plan = null, tipo = TipoMembresia.VISITA), PlanIndividual.DELUXE))
        assertNotNull(puede(null, PlanIndividual.DELUXE))
    }

    @Test
    fun conLaMembresiaSuspendidaOVencidaNoSePuedePedir() {
        assertNotNull(puede(membresia(estado = EstadoMembresia.SUSPENDIDA), PlanIndividual.DELUXE))
        assertNotNull(puede(membresia(vence = "2026-11-14"), PlanIndividual.DELUXE))
        // El día que vence todavía está vigente.
        assertNull(puede(membresia(vence = "2026-11-15"), PlanIndividual.DELUXE))
    }

    @Test
    fun noPuedePedirConOtraSolicitudOUnaRenovacionEnRevision() {
        assertNotNull(puede(membresia(), PlanIndividual.DELUXE, cambio = true))
        assertNotNull(puede(membresia(), PlanIndividual.DELUXE, renov = true))
    }

    @Test
    fun seApruebaUnaSolicitudPendienteConLaMembresiaIgual() {
        assertNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), membresia(), hoy, false))
        // Un miembro cuya membresía venció entre tanto aún se puede cambiar de plan: lo renovará con el plan nuevo.
        assertNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), membresia(vence = "2026-11-10"), hoy, false))
    }

    @Test
    fun noSeApruebaUnaSolicitudYaAtendida() {
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(status = SolicitudCambioPlan.APROBADO), membresia(), hoy, false))
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(status = SolicitudCambioPlan.RECHAZADO), membresia(), hoy, false))
    }

    @Test
    fun noSeApruebaSiLaMembresiaYaCambioDePlan() {
        // El miembro pidió desde NORMAL pero el personal ya lo pasó a DELUXE por otro lado.
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(actual = PlanIndividual.NORMAL), membresia(PlanIndividual.DELUXE), hoy, false))
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), membresia(plan = null, tipo = TipoMembresia.FAMILIAR), hoy, false))
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), null, hoy, false))
    }

    @Test
    fun noSeApruebaSuspendidaNiConRenovacionPendiente() {
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), membresia(estado = EstadoMembresia.SUSPENDIDA), hoy, false))
        assertNotNull(ReglasCambioPlan.motivoNoSePuedeAprobar(solicitud(), membresia(), hoy, true))
    }
}
