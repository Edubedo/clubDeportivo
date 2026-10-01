package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Torneo

/**
 * Todas las reglas de ocupación de un área en un solo lugar (sin Firebase ni Android, para poder
 * probarlas): el cupo por hora y los torneos que bloquean el área. Las usan tanto la reserva de
 * espacio como la creación de torneos, así ambos lados aplican exactamente las mismas reglas.
 *
 *  - Cada reserva vigente ocupa [Reserva.personas] lugares del cupo ([Area.capacidad]) en cada hora que abarca.
 *  - Una hora está llena cuando los lugares ocupados alcanzan la capacidad: ya no se puede reservar.
 *  - Un torneo ocupa el área completa durante su horario (todos los días entre fechaInicio y fechaFin):
 *    en esas horas no entra ninguna reserva. Un torneo sin horas registradas bloquea el día completo.
 *  - Un torneo tampoco puede crearse encima de reservas vigentes ni de otro torneo en esa área.
 */
object ReglasReserva {

    private const val MINUTOS_POR_DIA = 24 * 60

    fun esVigente(reserva: Reserva) =
        reserva.estado == EstadoReserva.CONFIRMADA || reserva.estado == EstadoReserva.PENDIENTE_APROBACION

    /** "HH:mm" a minutos desde medianoche; null si el texto no es una hora válida. */
    fun aMinutos(hora: String): Int? {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return h * 60 + m
    }

    private fun solapa(iniA: Int, finA: Int, iniB: Int, finB: Int) = iniA < finB && iniB < finA

    private fun tramoDelTorneo(torneo: Torneo): Pair<Int, Int> {
        val inicio = aMinutos(torneo.horaInicio)
        val fin = aMinutos(torneo.horaFin)
        return if (inicio == null || fin == null || fin <= inicio) 0 to MINUTOS_POR_DIA else inicio to fin
    }

    /** true si [torneo] ocupa [areaId] ese [fecha] en algún momento del tramo [inicioMin, finMin). */
    fun torneoOcupa(torneo: Torneo, areaId: String, fecha: String, inicioMin: Int, finMin: Int): Boolean {
        if (torneo.areaId != areaId || fecha < torneo.fechaInicio || fecha > torneo.fechaFin) return false
        val (torneoInicio, torneoFin) = tramoDelTorneo(torneo)
        return solapa(inicioMin, finMin, torneoInicio, torneoFin)
    }

    private fun reservaSolapa(reserva: Reserva, inicioMin: Int, finMin: Int): Boolean {
        val inicio = aMinutos(reserva.horaInicio) ?: return false
        val fin = aMinutos(reserva.horaFin) ?: return false
        return solapa(inicioMin, finMin, inicio, fin)
    }

    /** Cómo está una hora concreta ("hora" = 6 significa 06:00 a 07:00) de un área en una fecha. */
    data class EstadoHora(val hora: Int, val capacidad: Int, val ocupadas: Int, val torneo: Torneo?) {
        val libres get() = (capacidad - ocupadas).coerceAtLeast(0)
        val lleno get() = libres == 0
        val disponible get() = torneo == null && !lleno
    }

    fun estadoDeHora(area: Area, reservas: List<Reserva>, torneos: List<Torneo>, fecha: String, hora: Int): EstadoHora {
        val inicio = hora * 60
        val fin = inicio + 60
        val torneo = torneos.firstOrNull { torneoOcupa(it, area.id, fecha, inicio, fin) }
        val ocupadas = reservas
            .filter { it.areaId == area.id && it.fecha == fecha && esVigente(it) && reservaSolapa(it, inicio, fin) }
            .sumOf { it.personas }
        return EstadoHora(hora, area.capacidad, ocupadas, torneo)
    }

    private fun texto(hora: Int) = "%02d:00".format(hora)

    /**
     * @return el motivo por el que NO se puede reservar [area] el [fecha] de [horaInicio] a [horaFin]
     * para [personas], o null si la reserva es válida.
     */
    fun validarReserva(
        area: Area,
        reservas: List<Reserva>,
        torneos: List<Torneo>,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        personas: Int
    ): String? {
        val inicio = aMinutos(horaInicio) ?: return "Elige un horario."
        val fin = aMinutos(horaFin) ?: return "Elige un horario."
        val horas = (inicio / 60) until (fin / 60)
        if (horas.isEmpty()) return "Elige un horario."
        if (horas.count() > Catalogos.MAX_HORAS_POR_RESERVA) {
            return "Máximo ${Catalogos.MAX_HORAS_POR_RESERVA} horas por reserva."
        }
        if (personas < 1) return "Indica al menos 1 persona."
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return "${area.nombre} está en mantenimiento."
        if (personas > area.capacidad) return "${area.nombre} tiene capacidad para ${area.capacidad} personas."

        for (hora in horas) {
            val estado = estadoDeHora(area, reservas, torneos, fecha, hora)
            estado.torneo?.let { return "A las ${texto(hora)} el área está reservada para el torneo \"${it.nombre}\"." }
            if (estado.lleno) return "A las ${texto(hora)} el área ya alcanzó su cupo límite (${area.capacidad})."
            if (estado.libres < personas) {
                return "A las ${texto(hora)} solo quedan ${estado.libres} lugares disponibles."
            }
        }
        return null
    }

    /**
     * @return el motivo por el que el torneo NO puede ocupar [area] en ese rango de fechas y horas
     * (al editar, [torneoIdExcluido] evita chocar contra el propio torneo), o null si es válido.
     */
    fun validarTorneo(
        area: Area,
        reservas: List<Reserva>,
        torneos: List<Torneo>,
        torneoIdExcluido: String?,
        fechaInicio: String,
        fechaFin: String,
        horaInicio: String,
        horaFin: String
    ): String? {
        val inicio = aMinutos(horaInicio) ?: return "Indica la hora de inicio."
        val fin = aMinutos(horaFin) ?: return "Indica la hora de fin."
        if (fin <= inicio) return "La hora de fin debe ser posterior a la de inicio."
        if (fechaFin < fechaInicio) return "La fecha de fin no puede ser anterior a la de inicio."
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return "${area.nombre} está en mantenimiento."

        val otrosTorneos = torneos.filter { it.id != torneoIdExcluido }
        for (dia in Fechas.diasEntre(fechaInicio, fechaFin)) {
            otrosTorneos.firstOrNull { torneoOcupa(it, area.id, dia, inicio, fin) }?.let {
                return "${area.nombre} ya tiene el torneo \"${it.nombre}\" el $dia en ese horario."
            }
            reservas.firstOrNull {
                it.areaId == area.id && it.fecha == dia && esVigente(it) && reservaSolapa(it, inicio, fin)
            }?.let {
                return "${area.nombre} tiene una reserva el $dia de ${it.horaInicio} a ${it.horaFin}: cancélala o elige otro horario."
            }
        }
        return null
    }
}
