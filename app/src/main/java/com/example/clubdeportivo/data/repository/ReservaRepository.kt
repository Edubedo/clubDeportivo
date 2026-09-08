package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.delay

interface ReservaRepository {
    suspend fun obtenerReservasDeUsuario(usuarioId: String): List<Reserva>
    suspend fun contarReservasActivas(usuarioId: String): Int
    suspend fun crearReserva(
        usuarioId: String,
        areaId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        esExterno: Boolean
    ): Reserva
    suspend fun obtenerMaterialAsignado(reservaId: String): List<MaterialAsignado>
    /** true si se pudo cancelar sin penalización (fue con 4+ horas de anticipación). */
    suspend fun cancelarReserva(reservaId: String): Boolean
    suspend fun registrarNoShow(usuarioId: String)
    suspend fun estaBloqueadoPorInasistencias(usuarioId: String): Boolean
}

/**
 * Simula la asignación automática de material al reservar: cada área
 * tiene una herramienta "por defecto" que se asigna sola a la reserva,
 * tal como lo hará el backend real más adelante. También aplica las
 * reglas de cancelación e inasistencias del club (ver Catalogos).
 */
class FakeReservaRepository : ReservaRepository {

    private var siguienteId = 100
    private val reservas = mutableListOf(
        Reserva("1", "5", "1", "2026-09-10", "18:00", "19:00", EstadoReserva.CONFIRMADA),
        Reserva("2", "5", "3", "2026-09-12", "09:00", "10:00", EstadoReserva.PENDIENTE_APROBACION)
    )
    private val materialPorReserva = mutableMapOf(
        "1" to listOf(MaterialAsignado("1", "1", "10", 2))
    )

    private val herramientaPorArea = mapOf(
        "1" to "10", "2" to "10", "3" to "12", "4" to "12", "5" to "11", "6" to "11", "7" to "15", "8" to "15"
    )

    /** Marcas de tiempo (ms) de inasistencias por usuario, para la regla de 3 strikes. */
    private val noShowsPorUsuario = mutableMapOf<String, MutableList<Long>>()

    override suspend fun obtenerReservasDeUsuario(usuarioId: String): List<Reserva> {
        delay(400)
        return reservas.filter { it.usuarioId == usuarioId }
    }

    override suspend fun contarReservasActivas(usuarioId: String): Int {
        delay(150)
        return reservas.count {
            it.usuarioId == usuarioId &&
                (it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.PENDIENTE_APROBACION)
        }
    }

    override suspend fun crearReserva(
        usuarioId: String,
        areaId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        esExterno: Boolean
    ): Reserva {
        delay(500)

        // Un visitante externo necesita aprobación de un administrador antes de
        // que su reserva sea válida; un socio queda confirmado de inmediato.
        val estadoInicial = if (esExterno) EstadoReserva.PENDIENTE_APROBACION else EstadoReserva.CONFIRMADA

        val nuevaReserva = Reserva(
            id = (siguienteId++).toString(),
            usuarioId = usuarioId,
            areaId = areaId,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            estado = estadoInicial,
            esExterno = esExterno
        )
        reservas.add(nuevaReserva)

        // El material solo se asigna si la reserva queda CONFIRMADA con al
        // menos 1 hora de anticipación (una PENDIENTE_APROBACION todavía no
        // lo necesita: se asignará cuando un administrador la confirme).
        val horasDeAnticipacion = Fechas.horasDesdeAhora(fecha, horaInicio)
        val debeAsignarMaterial = estadoInicial == EstadoReserva.CONFIRMADA &&
            horasDeAnticipacion >= Catalogos.MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS

        if (debeAsignarMaterial) {
            val herramientaId = herramientaPorArea[areaId]
            if (herramientaId != null) {
                materialPorReserva[nuevaReserva.id] = listOf(
                    MaterialAsignado(nuevaReserva.id, nuevaReserva.id, herramientaId, 1)
                )
            }
        }

        return nuevaReserva
    }

    override suspend fun obtenerMaterialAsignado(reservaId: String): List<MaterialAsignado> {
        delay(200)
        return materialPorReserva[reservaId] ?: emptyList()
    }

    override suspend fun cancelarReserva(reservaId: String): Boolean {
        delay(300)
        val indice = reservas.indexOfFirst { it.id == reservaId }
        if (indice == -1) return true

        val reserva = reservas[indice]
        val sinPenalizacion = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio) >=
            Catalogos.CANCELACION_SIN_PENALIZACION_HORAS

        reservas[indice] = reserva.copy(estado = EstadoReserva.CANCELADA)
        return sinPenalizacion
    }

    override suspend fun registrarNoShow(usuarioId: String) {
        val strikes = noShowsPorUsuario.getOrPut(usuarioId) { mutableListOf() }
        strikes.add(System.currentTimeMillis())
    }

    override suspend fun estaBloqueadoPorInasistencias(usuarioId: String): Boolean {
        val strikes = noShowsPorUsuario[usuarioId] ?: return false
        val ventanaMs = Catalogos.VENTANA_INASISTENCIAS_DIAS * 24L * 60 * 60 * 1000
        val recientes = strikes.filter { System.currentTimeMillis() - it <= ventanaMs }
        if (recientes.size < Catalogos.INASISTENCIAS_PARA_BLOQUEO) return false

        val ultimaFalta = recientes.max()
        val bloqueoMs = Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS * 24L * 60 * 60 * 1000
        return System.currentTimeMillis() - ultimaFalta <= bloqueoMs
    }
}
