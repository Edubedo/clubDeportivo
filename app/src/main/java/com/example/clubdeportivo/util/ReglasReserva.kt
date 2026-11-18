package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.ModificacionReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Torneo
import java.util.Date

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

    /** Reservas vigentes en o antes de [hasta] que aún no tienen asistencia registrada. */
    fun reservasVisiblesProximamente(
        reservas: List<Reserva>,
        asistenciasRegistradas: Set<String>,
        hasta: String
    ): List<Reserva> =
        reservas.filter {
            esVigente(it) &&
                it.fecha <= hasta &&
                it.id !in asistenciasRegistradas
        }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))

    /** Vigentes primero, en orden próximo; las que ya terminaron quedan al final, de más reciente a más antigua. */
    fun ordenarActivasAntesQuePasadas(reservas: List<Reserva>, ahora: Date = Date()): List<Reserva> =
        reservas.sortedWith(Comparator { a, b ->
            val aTerminada = Fechas.horasDesdeAhora(a.fecha, a.horaFin, ahora) <= 0
            val bTerminada = Fechas.horasDesdeAhora(b.fecha, b.horaFin, ahora) <= 0
            when {
                aTerminada != bTerminada -> aTerminada.compareTo(bTerminada)
                aTerminada -> "${b.fecha} ${b.horaInicio}".compareTo("${a.fecha} ${a.horaInicio}")
                else -> "${a.fecha} ${a.horaInicio}".compareTo("${b.fecha} ${b.horaInicio}")
            }
        })

    /** Reservas futuras del socio que todavía cuentan para el límite personal. */
    fun contarReservasActivasFuturas(usuarioId: String, reservas: List<Reserva>, ahora: Date = Date()): Int =
        reservas.count {
            it.usuarioId == usuarioId &&
                esVigente(it) &&
                Fechas.horasDesdeAhora(it.fecha, it.horaFin, ahora) > 0
        }

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
    data class EstadoHora(
        val hora: Int,
        val capacidad: Int,
        val ocupadas: Int,
        val torneo: Torneo?,
        /** El área está en mantenimiento ese día: no se reserva. */
        val mantenimiento: Boolean = false
    ) {
        val libres get() = (capacidad - ocupadas).coerceAtLeast(0)
        val lleno get() = libres == 0
        val disponible get() = torneo == null && !mantenimiento && !lleno
        val pocosLugares get() =
            torneo == null && !mantenimiento && ocupadas > 0 && libres in 1..Catalogos.LUGARES_PARA_ALERTA_CUPO_BAJO
    }

    fun estadoDeHora(area: Area, reservas: List<Reserva>, torneos: List<Torneo>, fecha: String, hora: Int): EstadoHora {
        val inicio = hora * 60
        val fin = inicio + 60
        val torneo = torneos.firstOrNull { torneoOcupa(it, area.id, fecha, inicio, fin) }
        val ocupadas = reservas
            .filter { it.areaId == area.id && it.fecha == fecha && esVigente(it) && reservaSolapa(it, inicio, fin) }
            .sumOf { it.personas }
        return EstadoHora(hora, area.capacidad, ocupadas, torneo, mantenimiento = area.enMantenimientoEl(fecha))
    }

    /** Texto del periodo de mantenimiento de [area] ("del 5 de ... al 9 de ..."), o vacío si no tiene fechas. */
    fun textoMantenimiento(area: Area): String =
        if (area.tienePeriodoDeMantenimiento) {
            Textos.get("maintenance_period", Fechas.legible(area.mantenimientoDesde), Fechas.legible(area.mantenimientoHasta))
        } else {
            ""
        }

    /** Por qué no se puede reservar [area] el [fecha] por mantenimiento (con el periodo si lo tiene), o null si no está en mantenimiento. */
    fun motivoMantenimiento(area: Area, fecha: String): String? {
        if (!area.enMantenimientoEl(fecha)) return null
        val periodo = textoMantenimiento(area)
        return Textos.get("is_under_maintenance", area.nombre) + if (periodo.isBlank()) "" else " $periodo"
    }

    /**
     * La reserva vigente de [usuarioId] que choca con el horario [horaInicio]–[horaFin] de [fecha] (en cualquier área), o
     * null. Una persona no puede estar en dos reservas a la misma hora: para cambiarla se edita la que ya tiene.
     * [excluirId] deja fuera la reserva que se está modificando.
     */
    fun reservaDelMiembroEnConflicto(
        usuarioId: String,
        reservas: List<Reserva>,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        excluirId: String? = null
    ): Reserva? {
        val inicio = aMinutos(horaInicio) ?: return null
        val fin = aMinutos(horaFin) ?: return null
        return reservas.firstOrNull {
            it.usuarioId == usuarioId && it.id != excluirId && it.fecha == fecha && esVigente(it) && reservaSolapa(it, inicio, fin)
        }
    }

    /** Lugares de [areaId] que esa hora están pedidos en solicitudes todavía "en revisión" (ya cuentan dentro del cupo ocupado). */
    fun personasEnRevision(areaId: String, reservas: List<Reserva>, fecha: String, hora: Int): Int =
        reservas
            .filter {
                it.areaId == areaId && it.fecha == fecha && it.estado == EstadoReserva.PENDIENTE_APROBACION &&
                    reservaSolapa(it, hora * 60, hora * 60 + 60)
            }
            .sumOf { it.personas }

    private fun texto(hora: Int) = String.format(java.util.Locale.ROOT, "%02d:00", hora)

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
        val inicio = aMinutos(horaInicio) ?: return Textos.get("choose_a_time")
        val fin = aMinutos(horaFin) ?: return Textos.get("choose_a_time")
        val horas = (inicio / 60) until (fin / 60)
        if (horas.isEmpty()) return Textos.get("choose_a_time")
        if (horas.count() > Catalogos.MAX_HORAS_POR_RESERVA) {
            return Textos.get("maximum_hours_per_booking", Catalogos.MAX_HORAS_POR_RESERVA)
        }
        if (personas < 1) return Textos.get("enter_at_least_1_person")
        motivoMantenimiento(area, fecha)?.let { return it }
        if (personas > area.capacidad) return Textos.get("has_capacity_for_people", area.nombre, area.capacidad)

        for (hora in horas) {
            val estado = estadoDeHora(area, reservas, torneos, fecha, hora)
            estado.torneo?.let { return Textos.get("at_the_area_is_reserved_for", texto(hora), it.nombre) }
            if (estado.lleno) return Textos.get("at_the_area_has_already_reached", texto(hora), area.capacidad)
            if (estado.libres < personas) {
                return Textos.get("at_only_spots_are_left", texto(hora), estado.libres)
            }
        }
        return null
    }

    /**
     * @return el motivo por el que [cambio] NO se puede aplicar a [reserva], o null si es válido. Lo usan quien pide el cambio
     * (con [anticipacionMinimaHoras] = la de cualquier reserva) y quien lo aprueba (con 0: basta que el nuevo horario no
     * haya empezado), así los dos aplican exactamente las mismas reglas.
     *
     * [reservasDelArea] y [reservasDelMiembro] pueden incluir la propia reserva: se descarta, porque se va a mover.
     */
    fun validarModificacion(
        area: Area,
        reserva: Reserva,
        cambio: ModificacionReserva,
        reservasDelArea: List<Reserva>,
        reservasDelMiembro: List<Reserva>,
        torneos: List<Torneo>,
        anticipacionMinimaHoras: Int,
        ahora: Date = Date()
    ): String? {
        val mismoHorario = cambio.fecha == reserva.fecha && cambio.horaInicio == reserva.horaInicio &&
            cambio.horaFin == reserva.horaFin && cambio.personas == reserva.personas
        if (mismoHorario) return Textos.get("change_request_no_changes")
        if (Fechas.horasDesdeAhora(cambio.fecha, cambio.horaInicio, ahora) < anticipacionMinimaHoras ||
            Fechas.horasDesdeAhora(cambio.fecha, cambio.horaInicio, ahora) <= 0
        ) {
            return Textos.get("minimum_booking_advance", anticipacionMinimaHoras.coerceAtLeast(0))
        }

        reservaDelMiembroEnConflicto(reserva.usuarioId, reservasDelMiembro, cambio.fecha, cambio.horaInicio, cambio.horaFin, reserva.id)?.let {
            return Textos.get("you_already_have_booking_at_that_time", Fechas.legible(it.fecha), it.horaInicio, it.horaFin)
        }

        val otras = reservasDelArea.filter { it.id != reserva.id }
        if (area.carriles > 0) {
            val carril = cambio.carril ?: reserva.carril
            if (carril != null) {
                val ini = aMinutos(cambio.horaInicio) ?: return Textos.get("choose_a_time")
                val fin = aMinutos(cambio.horaFin) ?: return Textos.get("choose_a_time")
                val ocupado = otras.any { it.fecha == cambio.fecha && it.carril == carril && esVigente(it) && reservaSolapa(it, ini, fin) }
                if (ocupado) return Textos.get("that_lane_has_just_been_taken")
            }
        }
        return validarReserva(area, otras, torneos, cambio.fecha, cambio.horaInicio, cambio.horaFin, cambio.personas)
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
        val inicio = aMinutos(horaInicio) ?: return Textos.get("enter_the_start_time")
        val fin = aMinutos(horaFin) ?: return Textos.get("enter_the_end_time")
        if (fin <= inicio) return Textos.get("the_end_time_must_be_after")
        if (fechaFin < fechaInicio) return Textos.get("the_end_date_can_t_be")
        if (area.enMantenimientoEntre(fechaInicio, fechaFin)) {
            return Textos.get("is_under_maintenance", area.nombre) + textoMantenimiento(area).let { if (it.isBlank()) "" else " $it" }
        }

        val otrosTorneos = torneos.filter { it.id != torneoIdExcluido }
        for (dia in Fechas.diasEntre(fechaInicio, fechaFin)) {
            otrosTorneos.firstOrNull { torneoOcupa(it, area.id, dia, inicio, fin) }?.let {
                return Textos.get("already_has_the_tournament_on_at", area.nombre, it.nombre, dia)
            }
            reservas.firstOrNull {
                it.areaId == area.id && it.fecha == dia && esVigente(it) && reservaSolapa(it, inicio, fin)
            }?.let {
                return Textos.get("has_a_booking_on_from_to", area.nombre, dia, it.horaInicio, it.horaFin)
            }
        }
        return null
    }
}
