package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.SolicitudRenovacion

data class MembresiaEstadoVisual(
    val membresia: Membresia?,
    val renovacionAprobadaVigente: Boolean
)

fun reconciliarRenovacionAprobada(
    membresia: Membresia?,
    solicitud: SolicitudRenovacion?,
    hoy: String = Fechas.hoy()
): MembresiaEstadoVisual {
    if (
        membresia == null ||
        solicitud?.status != SolicitudRenovacion.APROBADO ||
        solicitud.membresiaId != membresia.id ||
        membresia.estado == EstadoMembresia.SUSPENDIDA
    ) {
        return MembresiaEstadoVisual(membresia, false)
    }

    val fechaAprobacion = solicitud.reviewedDate.takeIf { it.isNotBlank() }
        ?: solicitud.reviewedAt?.let(Fechas::fechaLocal)
        ?: solicitud.createdAt.takeIf { it > 0 }?.let(Fechas::fechaLocal)
        ?: return MembresiaEstadoVisual(membresia, false)
    val fechaVigenciaAprobada = solicitud.renewedUntil.takeIf { it.isNotBlank() }
        ?: membresia.fechaVencimiento.takeIf { it >= fechaAprobacion }
        ?: ReglasMembresia.fechasDeRenovacion(membresia, fechaAprobacion).second
    if (fechaVigenciaAprobada < hoy) return MembresiaEstadoVisual(membresia, false)

    val vencimiento = maxOf(membresia.fechaVencimiento, fechaVigenciaAprobada)
    val actualizada = if (
        membresia.fechaVencimiento != vencimiento ||
        ReglasMembresia.estadoEfectivo(membresia, hoy) != EstadoMembresia.ACTIVA
    ) {
        membresia.copy(
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = if (membresia.fechaVencimiento < fechaAprobacion) fechaAprobacion else membresia.fechaInicio,
            fechaVencimiento = vencimiento
        )
    } else {
        membresia
    }
    return MembresiaEstadoVisual(actualizada, true)
}
