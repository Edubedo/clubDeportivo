package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoEspera
import com.example.clubdeportivo.util.ReglasListaEspera
import com.example.clubdeportivo.util.ReglasReserva
import java.util.Date

/**
 * Aparta los lugares que se liberaron para los primeros de la fila. No hay servidor que lo haga, así que lo llama la app
 * cada vez que algo pudo liberar lugar o alguien mira la fila: al cancelar o rechazar una reserva, al ver el horario de
 * un área y en la revisión periódica de quien está esperando. Es idempotente: si nada cambió, no escribe nada.
 */
object GestorListaEspera {

    suspend fun promover(areaId: String, fecha: String, ahora: Date = Date()) {
        val repositorio = AppContainer.listaEsperaRepository
        val entradas = repositorio.obtenerDeArea(areaId, fecha)
        val horasEsperando = entradas.filter { it.estado == EstadoEspera.ESPERANDO }.map { it.hora }.distinct()
        if (horasEsperando.isEmpty()) return

        val area = AppContainer.areaRepository.obtenerAreaPorId(areaId) ?: return
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return
        val reservas = AppContainer.reservaRepository.obtenerReservasDeArea(areaId)
        val torneos = AppContainer.torneoRepository.obtenerTorneos()

        val avisar = horasEsperando.flatMap { hora ->
            val estado = ReglasReserva.estadoDeHora(area, reservas, torneos, fecha, hora)
            if (estado.torneo != null) emptyList()
            else ReglasListaEspera.porAvisar(ReglasListaEspera.fila(entradas, areaId, fecha, hora, ahora), estado.libres)
        }
        repositorio.marcarAvisadas(avisar.map { it.id }, ahora.time)
    }
}
