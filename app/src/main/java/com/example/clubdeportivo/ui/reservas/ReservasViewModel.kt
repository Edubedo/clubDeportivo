package com.example.clubdeportivo.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.esCliente
import com.example.clubdeportivo.data.model.esPersonal
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.RestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.ReglasReserva
import kotlinx.coroutines.launch
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

/** Una hora de la cuadrícula ("hora" = 6 es 06:00-07:00) con su estado de ocupación. */
data class HoraUi(
    val estado: ReglasReserva.EstadoHora,
    /** El horario ya comenzó o terminó. */
    val pasada: Boolean
) {
    val hora get() = estado.hora
    val seleccionable get() = estado.disponible && !pasada
}

data class ReservaEspacioUi(
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val areas: List<Area> = emptyList(),
    val deporte: String? = null,
    val area: Area? = null,
    val fecha: String = Fechas.hoy(),
    val horarioTexto: String = "",
    val horas: List<HoraUi> = emptyList(),
    /** No se pudo leer la ocupación (sin conexión o sin permiso): no es lo mismo que "no hay horarios". */
    val errorDisponibilidad: Boolean = false,
    /** Rango elegido, horas de inicio de la primera y la última (ambas incluidas). */
    val desde: Int? = null,
    val hasta: Int? = null,
    val personas: Int = 1,
    val carril: Int? = null,
    val carrilesDisponibles: List<Int> = emptyList()
) {
    val horasElegidas get() = if (desde != null && hasta != null) hasta - desde + 1 else 0

    /** Lugares libres que aún quedan en TODAS las horas elegidas: tope de personas para esta reserva. */
    val maxPersonas: Int
        get() {
            val d = desde ?: return 0
            val h = hasta ?: return 0
            return horas.filter { it.hora in d..h }.minOfOrNull { it.estado.libres } ?: 0
        }
}

data class ReservaListada(
    val id: String,
    val titulo: String,
    val emoji: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val personas: Int,
    val estado: EstadoReserva,
    val deporte: String,
    val carril: Int?,
    val usuarioNombre: String
)

class ReservasViewModel(
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository,
    private val restriccionHorarioRepository: RestriccionHorarioRepository = AppContainer.restriccionHorarioRepository,
    private val membresiaRepository: MembresiaRepository = AppContainer.membresiaRepository
) : ViewModel() {

    private val _ui = MutableLiveData(ReservaEspacioUi())
    val ui: LiveData<ReservaEspacioUi> = _ui

    private val _reservas = MutableLiveData<List<ReservaListada>>(emptyList())
    val reservas: LiveData<List<ReservaListada>> = _reservas

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    /** Por qué la persona no puede reservar (membresía suspendida, vencida o inexistente); null si puede. */
    private val _restriccion = MutableLiveData<String?>()
    val restriccion: LiveData<String?> = _restriccion

    private val estado get() = _ui.value ?: ReservaEspacioUi()
    private val usuario get() = SesionManager.usuarioActual
    val esVisitanteExterno get() = usuario?.rol == Rol.VISITANTE_EXTERNO

    /** Días que se pueden elegir (hoy + [Catalogos.ANTICIPACION_MAXIMA_DIAS]). */
    val fechas = Fechas.proximosDias(Catalogos.ANTICIPACION_MAXIMA_DIAS + 1)

    init {
        cargar()
        viewModelScope.launch { _restriccion.value = runCatching { motivoDeRestriccion() }.getOrNull() }
    }

    /** Solo socios y visitantes con una membresía activa y vigente pueden reservar; el personal siempre puede. */
    private suspend fun motivoDeRestriccion(): String? {
        val actual = usuario ?: return null
        if (!actual.rol.esCliente()) return null
        val membresia = membresiaRepository.obtenerMembresia(actual.id)
            ?: return Textos.get(R.string.we_couldn_t_find_a_membership)
        return when (ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())) {
            EstadoMembresia.ACTIVA -> null
            EstadoMembresia.VENCIDA ->
                Textos.get(R.string.your_membership_expired_on_renew_it, Fechas.legible(membresia.fechaVencimiento))
            EstadoMembresia.SUSPENDIDA ->
                Textos.get(R.string.manual_membresia_suspendida, membresia.motivoSuspension.ifBlank { Textos.get(R.string.no_reason_recorded) })
        }
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                val areas = areaRepository.obtenerAreas()
                _ui.value = estado.copy(cargando = false, areas = areas)
                estado.area?.let { actual -> areas.firstOrNull { it.id == actual.id }?.let { elegirArea(it) } }
                cargarLista(areas)
            } catch (e: Exception) {
                _ui.value = estado.copy(cargando = false)
                _mensaje.value = Textos.get(R.string.the_areas_could_not_be_loaded)
            }
        }
    }

    fun elegirDeporte(deporte: String) {
        _ui.value = estado.copy(
            deporte = deporte, area = null, horas = emptyList(), errorDisponibilidad = false, desde = null, hasta = null, personas = 1
        )
    }

    fun elegirArea(area: Area) {
        viewModelScope.launch {
            val (apertura, cierre) = rangoDeApertura(area)
            val texto = Textos.get(R.string.hours_open_max_hours, apertura.take(5), cierre.take(5), Catalogos.MAX_HORAS_POR_RESERVA) +
                if (esVisitanteExterno) Textos.get(R.string.requires_approval) else ""
            _ui.value = estado.copy(area = area, horarioTexto = texto, desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
            recalcularHoras(apertura, cierre)
        }
    }

    fun elegirFecha(fecha: String) {
        _ui.value = estado.copy(fecha = fecha, desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
        val area = estado.area ?: return
        viewModelScope.launch {
            val (apertura, cierre) = rangoDeApertura(area)
            recalcularHoras(apertura, cierre)
        }
    }

    /** Todas las áreas y tipos de usuario comparten el horario publicado por el área. */
    private suspend fun rangoDeApertura(area: Area): Pair<String, String> {
        val horario = restriccionHorarioRepository.obtenerHorarioDeArea(area)
        return horario.horaInicio to horario.horaFin
    }

    /** Vuelve a leer reservas y torneos y arma la cuadrícula de horas del área y día elegidos. */
    private suspend fun recalcularHoras(apertura: String, cierre: String) {
        val area = estado.area ?: return
        val fecha = estado.fecha
        try {
            val reservas = reservaRepository.obtenerReservasDeArea(area.id)
            val torneos = torneoRepository.obtenerTorneos()
            val primera = ReglasReserva.aMinutos(apertura)?.div(60) ?: return
            val ultima = (ReglasReserva.aMinutos(cierre)?.div(60) ?: return) - 1
            val horas = (primera..ultima).map { hora ->
                HoraUi(
                    estado = ReglasReserva.estadoDeHora(area, reservas, torneos, fecha, hora),
                    pasada = Fechas.horasDesdeAhora(fecha, "%02d:00".format(hora)) < Catalogos.ANTICIPACION_MINIMA_HORAS
                )
            }
            _ui.value = estado.copy(horas = horas, errorDisponibilidad = false)
        } catch (e: Exception) {
            _ui.value = estado.copy(horas = emptyList(), errorDisponibilidad = true)
            _mensaje.value = Textos.get(R.string.the_availability_could_not_be_checked)
        }
    }

    fun tocarHora(hora: Int) {
        val actual = estado
        val celda = actual.horas.firstOrNull { it.hora == hora } ?: return
        if (!celda.seleccionable) {
            _mensaje.value = when {
                celda.pasada -> Textos.get(R.string.that_hour_has_already_started)
                celda.estado.torneo != null -> Textos.get(R.string.at_that_time_the_area_is, celda.estado.torneo.nombre)
                else -> Textos.get(R.string.that_hour_has_reached_its_capacity, celda.estado.capacidad)
            }
            return
        }
        val d = actual.desde
        val a = actual.hasta
        val max = Catalogos.MAX_HORAS_POR_RESERVA
        val (nuevoDesde, nuevoHasta) = when {
            d == null || a == null -> hora to hora
            hora == d && d == a -> null to null
            hora == a + 1 && hora - d + 1 <= max -> d to hora
            hora == d - 1 && a - hora + 1 <= max -> hora to a
            else -> hora to hora
        }
        val nuevo = actual.copy(desde = nuevoDesde, hasta = nuevoHasta)
        _ui.value = nuevo.copy(personas = nuevo.personas.coerceIn(1, nuevo.maxPersonas.coerceAtLeast(1)))
        if (nuevoDesde == null || nuevoHasta == null) {
            _ui.value = estado.copy(carril = null, carrilesDisponibles = emptyList())
        } else {
            viewModelScope.launch { cargarCarrilesDisponibles() }
        }
    }

    /** Descarta la reserva a medias (deporte, cancha, horas...) y vuelve al primer paso. */
    fun reiniciarProceso() {
        _ui.value = estado.copy(
            deporte = null, area = null, horas = emptyList(), errorDisponibilidad = false,
            desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList()
        )
    }

    fun limpiarSeleccion() {
        _ui.value = estado.copy(desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
    }

    fun elegirCarril(carril: Int) {
        if (carril !in estado.carrilesDisponibles) return
        _ui.value = estado.copy(carril = carril)
    }

    private suspend fun cargarCarrilesDisponibles() {
        val actual = estado
        val area = actual.area ?: return
        val desde = actual.desde ?: return
        val hasta = actual.hasta ?: return
        if (area.carriles <= 0) {
            _ui.value = actual.copy(carrilesDisponibles = emptyList())
            return
        }
        try {
            val ocupadas = reservaRepository.obtenerReservasDeArea(area.id)
                .filter {
                    it.fecha == actual.fecha &&
                        it.carril != null &&
                        it.horaInicio < "%02d:00".format(hasta + 1) &&
                        it.horaFin > "%02d:00".format(desde)
                }
                .mapNotNull { it.carril }
                .toSet()
            val disponibles = (1..area.carriles).filterNot { it in ocupadas }
            _ui.value = actual.copy(
                carrilesDisponibles = disponibles,
                carril = actual.carril?.takeIf { it in disponibles },
                errorDisponibilidad = false
            )
        } catch (e: Exception) {
            _ui.value = actual.copy(errorDisponibilidad = true)
            _mensaje.value = Textos.get(R.string.the_lane_availability_could_not_be)
        }
    }

    fun cambiarPersonas(delta: Int) {
        val actual = estado
        _ui.value = actual.copy(personas = (actual.personas + delta).coerceIn(1, actual.maxPersonas.coerceAtLeast(1)))
    }

    fun confirmar() {
        val actual = estado
        val area = actual.area ?: return
        val desde = actual.desde ?: return
        val hasta = actual.hasta ?: return
        val usuario = usuario ?: return
        val horaInicio = "%02d:00".format(desde)
        val horaFin = "%02d:00".format(hasta + 1)

        viewModelScope.launch {
            _ui.value = estado.copy(enviando = true)
            try {
                val error = validar(area, actual.fecha, horaInicio, horaFin, actual.personas, actual.carril)
                if (error != null) {
                    _mensaje.value = error
                } else {
                    val reserva = reservaRepository.crearReserva(
                        usuarioId = usuario.id,
                        areaId = area.id,
                        fecha = actual.fecha,
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        esExterno = esVisitanteExterno,
                        personas = actual.personas,
                        carril = actual.carril,
                        // Miembros y visitantes quedan en revisión; el personal reserva sin pedir permiso.
                        requiereAprobacion = usuario.rol.esCliente()
                    )
                    _mensaje.value = if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) {
                        Textos.get(R.string.request_sent_your_booking_is_under)
                    } else {
                        Textos.get(R.string.booking_confirmed_from_to, Deportes.titulo(area.tipo, area.nombre), actual.fecha, horaInicio, horaFin)
                    }
                }
                // Siempre se vuelve a leer la ocupación real, haya funcionado o no.
                _ui.value = estado.copy(desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
                val (apertura, cierre) = rangoDeApertura(area)
                recalcularHoras(apertura, cierre)
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = Textos.get(R.string.the_booking_could_not_be_completed)
            }
            _ui.value = estado.copy(enviando = false)
        }
    }

    /**
     * Reglas del socio y del club + cupo/torneos. Usa datos recién leídos (no los de la pantalla),
     * así un cupo que se llenó mientras se elegía el horario se detecta antes de guardar.
     */
    private suspend fun validar(area: Area, fecha: String, horaInicio: String, horaFin: String, personas: Int, carril: Int?): String? {
        val usuario = usuario ?: return Textos.get(R.string.sign_in_to_book)
        motivoDeRestriccion()?.let { return it }
        if (area.carriles > 0) {
            if (carril == null || carril !in 1..area.carriles) return Textos.get(R.string.select_an_available_lane_2)
            val carrilOcupado = reservaRepository.obtenerReservasDeArea(area.id).any {
                it.fecha == fecha &&
                    it.carril == carril &&
                    it.horaInicio < horaFin &&
                    it.horaFin > horaInicio
            }
            if (carrilOcupado) return Textos.get(R.string.that_lane_has_just_been_taken)
        }

        if (esVisitanteExterno && !area.permiteExternos) return Textos.get(R.string.this_area_doesn_t_accept_bookings)
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return Textos.get(R.string.is_under_maintenance, area.nombre)
        if (Fechas.horasDesdeAhora(fecha, horaInicio) < Catalogos.ANTICIPACION_MINIMA_HORAS) {
            return Textos.get(R.string.that_time_has_already_started)
        }

        if (!usuario.rol.esPersonal()) {
            if (reservaRepository.estaBloqueadoPorInasistencias(usuario.id)) {
                return Textos.get(R.string.you_can_t_book_you_have, Catalogos.INASISTENCIAS_PARA_BLOQUEO) +
                    Textos.get(R.string.and_you_are_blocked_for_days, Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS)
            }
            if (reservaRepository.contarReservasActivas(usuario.id) >= Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO) {
                return Textos.get(R.string.you_have_reached_the_maximum_limit, Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO)
            }
        }

        return ReglasReserva.validarReserva(
            area = area,
            reservas = reservaRepository.obtenerReservasDeArea(area.id),
            torneos = torneoRepository.obtenerTorneos(),
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            personas = personas
        )
    }

    private suspend fun cargarLista(areas: List<Area>) {
        val usuario = usuario ?: return
        val hoy = Fechas.hoy()
        // Quien reserva también ve las suyas rechazadas (para saber qué pasó); el personal, las vigentes de todos.
        val visibles = if (usuario.rol.esPersonal()) {
            reservaRepository.obtenerReservasVigentes()
        } else {
            reservaRepository.obtenerReservasDeUsuario(usuario.id)
                .filter { ReglasReserva.esVigente(it) || it.estado == EstadoReserva.RECHAZADA }
        }
        _reservas.value = visibles
            .filter { it.fecha >= hoy }
            .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            .map { reserva ->
                val area = areas.firstOrNull { it.id == reserva.areaId }
                ReservaListada(
                    id = reserva.id,
                    titulo = area?.let { Deportes.titulo(it.tipo, it.nombre) } ?: Textos.get(R.string.area_deleted_2),
                    emoji = area?.let { Deportes.emojiDe(it.tipo, it.emoji) } ?: "📍",
                    fecha = reserva.fecha,
                    horaInicio = reserva.horaInicio,
                    horaFin = reserva.horaFin,
                    personas = reserva.personas,
                    estado = reserva.estado,
                    deporte = area?.tipo ?: reserva.deporte,
                    carril = reserva.carril,
                    usuarioNombre = reserva.usuarioNombre
                )
            }
    }

    fun cancelar(reserva: ReservaListada) {
        viewModelScope.launch {
            try {
                val sinPenalizacion = reservaRepository.cancelarReserva(reserva.id)
                _mensaje.value = if (sinPenalizacion) {
                    Textos.get(R.string.booking_cancelled)
                } else {
                    Textos.get(R.string.booking_cancelled_less_than_hours_in, Catalogos.CANCELACION_SIN_PENALIZACION_HORAS)
                }
                estado.area?.let { elegirArea(it) }
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = Textos.get(R.string.the_booking_could_not_be_cancelled)
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
