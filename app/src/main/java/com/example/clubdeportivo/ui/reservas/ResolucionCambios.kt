package com.example.clubdeportivo.ui.reservas

import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.GestorListaEspera
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.util.ReglasReserva
import com.example.clubdeportivo.util.Textos

/**
 * Lo que hace el administrador, el supervisor o el encargado del área con un cambio de día u horario que pidió un miembro:
 * aprobarlo (solo si el nuevo horario sigue libre) o rechazarlo. Lo comparten las pantallas de quienes validan.
 */
object ResolucionCambios {

    /** @return null si el cambio se aplicó; si no, el motivo por el que no se pudo (la reserva se queda como estaba). */
    suspend fun aprobar(reserva: Reserva): String? {
        val cambio = reserva.modificacion ?: return Textos.get("cambio_reserva_ya_resuelto")
        val reservas = AppContainer.reservaRepository
        val area = AppContainer.areaRepository.obtenerAreaPorId(reserva.areaId) ?: return Textos.get("area_deleted_2")
        // Se vuelve a revisar con datos recién leídos: el horario pudo llenarse desde que el miembro lo pidió.
        ReglasReserva.validarModificacion(
            area = area,
            reserva = reserva,
            cambio = cambio,
            reservasDelArea = reservas.obtenerReservasDeArea(area.id),
            reservasDelMiembro = reservas.obtenerReservasDeUsuario(reserva.usuarioId),
            torneos = AppContainer.torneoRepository.obtenerTorneos(),
            // Quien aprueba solo necesita que el nuevo horario no haya empezado.
            anticipacionMinimaHoras = 0
        )?.let { return it }
        reservas.aprobarModificacion(reserva.id, cambio)
        // El horario que dejó libre se ofrece a la lista de espera.
        runCatching { GestorListaEspera.promover(reserva.areaId, reserva.fecha) }
        return null
    }

    suspend fun rechazar(reserva: Reserva, motivo: String = "") {
        AppContainer.reservaRepository.rechazarModificacion(reserva.id, motivo)
    }
}
