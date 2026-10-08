package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasReserva
import kotlinx.coroutines.delay

interface ReservaRepository {
    suspend fun obtenerReservasDeUsuario(usuarioId: String): List<Reserva>
    suspend fun contarReservasActivas(usuarioId: String): Int

    suspend fun registrarAsistencia(reservaId: String, usuarioId: String, asistencia: String)

    suspend fun obtenerAsistencia(reservaId: String): String?

    suspend fun crearReserva(
        usuarioId: String,
        areaId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        esExterno: Boolean,
        personas: Int = 1,
        /** true = la reserva queda "en revisión" hasta que el encargado del área la apruebe (miembros y visitantes). */
        requiereAprobacion: Boolean = true
    ): Reserva
    /** Reservas vigentes (confirmadas o pendientes) de un área, de cualquier usuario: base del control de cupo. */
    suspend fun obtenerReservasDeArea(areaId: String): List<Reserva>
    /** Todas las reservas vigentes del club (confirmadas o pendientes). */
    suspend fun obtenerReservasPorDeporte(deporte: String): List<Reserva>
    suspend fun obtenerReservasVigentes(): List<Reserva>
    suspend fun obtenerMaterialAsignado(reservaId: String): List<MaterialAsignado>
    /** true si se pudo cancelar sin penalización (fue con 4+ horas de anticipación). */
    suspend fun cancelarReserva(reservaId: String): Boolean
    suspend fun registrarNoShow(usuarioId: String)

    /** Confirma una reserva que estaba pendiente de aprobación (visitantes externos) y le aparta su material. */
    suspend fun aprobarReserva(reservaId: String)

    /** Rechaza una reserva pendiente de aprobación: queda rechazada y libera su lugar. */
    suspend fun rechazarReserva(reservaId: String)
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

    private val asistencias = mutableMapOf<String, String>()

    override suspend fun registrarAsistencia(
        reservaId: String,
        usuarioId: String,
        asistencia: String
    ) {
        asistencias[reservaId] = asistencia
    }

    override suspend fun obtenerAsistencia(
        reservaId: String
    ): String? {
        return asistencias[reservaId]
    }

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
        esExterno: Boolean,
        personas: Int,
        requiereAprobacion: Boolean
    ): Reserva {
        delay(500)

        // Miembros y visitantes quedan en revisión hasta que el encargado del área apruebe; el personal, confirmado.
        val estadoInicial = if (requiereAprobacion) EstadoReserva.PENDIENTE_APROBACION else EstadoReserva.CONFIRMADA

        val nuevaReserva = Reserva(
            id = (siguienteId++).toString(),
            usuarioId = usuarioId,
            areaId = areaId,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            estado = estadoInicial,
            esExterno = esExterno,
            personas = personas
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

    override suspend fun obtenerReservasDeArea(areaId: String): List<Reserva> =
        reservas.filter { it.areaId == areaId && ReglasReserva.esVigente(it) }

    override suspend fun obtenerReservasPorDeporte(deporte: String): List<Reserva> {
        // En el repositorio falso no tenemos guardado el deporte directamente
        // en Reserva, así que relacionamos los IDs de área con su deporte.
        val areasDelDeporte = when (deporte.trim().lowercase()) {
            "tenis" -> listOf("7", "8")
            "fútbol", "futbol" -> listOf("1", "2")
            "básquetbol", "basquetbol", "baloncesto" -> listOf("3", "4")
            "natación", "natacion" -> listOf("5", "6")
            else -> emptyList()
        }

        return reservas.filter {
            it.areaId in areasDelDeporte && ReglasReserva.esVigente(it)
        }
    }

    override suspend fun obtenerReservasVigentes(): List<Reserva> =
        reservas.filter { ReglasReserva.esVigente(it) }

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

    override suspend fun aprobarReserva(reservaId: String) {
        reservas.replaceAll { if (it.id == reservaId) it.copy(estado = EstadoReserva.CONFIRMADA) else it }
    }

    override suspend fun rechazarReserva(reservaId: String) {
        reservas.replaceAll { if (it.id == reservaId) it.copy(estado = EstadoReserva.RECHAZADA) else it }
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
