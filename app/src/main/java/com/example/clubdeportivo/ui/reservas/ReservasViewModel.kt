package com.example.clubdeportivo.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.GestorListaEspera
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.EntradaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import com.example.clubdeportivo.data.repository.ListaEsperaRepository
import com.example.clubdeportivo.util.ReglasListaEspera
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaMotivoSuspension
import com.example.clubdeportivo.data.model.etiquetaArea

/** Una hora de la cuadrícula ("hora" = 6 es 06:00-07:00) con su estado de ocupación. */
data class HoraUi(
    val estado: ReglasReserva.EstadoHora,
    /** El horario ya comenzó o terminó. */
    val pasada: Boolean,
    /** La persona está en la lista de espera de esta hora. */
    val enEspera: Boolean = false
) {
    val hora get() = estado.hora
    val seleccionable get() = estado.disponible && !pasada
    /** Llena por reservas (no por un torneo) y todavía no empieza: se puede pedir lugar en la lista de espera. */
    val esperable get() = !pasada && estado.torneo == null && estado.lleno
}

/** Una entrada de la lista de espera de la persona, lista para mostrar. */
data class EsperaListada(
    val entrada: EntradaEspera,
    val titulo: String,
    val emoji: String,
    /** Lugar en la fila (1 = la siguiente); null si ya no está en ella. */
    val posicion: Int?,
    /** "HH:mm" hasta la que se le guarda el lugar liberado; null si todavía no le toca. */
    val limite: String?
)

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
    val usuarioNombre: String,
    val areaId: String = ""
)

class ReservasViewModel(
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository,
    private val restriccionHorarioRepository: RestriccionHorarioRepository = AppContainer.restriccionHorarioRepository,
    private val membresiaRepository: MembresiaRepository = AppContainer.membresiaRepository,
    private val listaEsperaRepository: ListaEsperaRepository = AppContainer.listaEsperaRepository
) : ViewModel() {

    private val _ui = MutableLiveData(ReservaEspacioUi())
    val ui: LiveData<ReservaEspacioUi> = _ui

    private val _reservas = MutableLiveData<List<ReservaListada>>(emptyList())
    val reservas: LiveData<List<ReservaListada>> = _reservas

    private val _esperas = MutableLiveData<List<EsperaListada>>(emptyList())
    val esperas: LiveData<List<EsperaListada>> = _esperas

    /** Hora llena que la persona tocó: se le ofrece entrar a la lista de espera. */
    private val _ofertaEspera = MutableLiveData<Int?>(null)
    val ofertaEspera: LiveData<Int?> = _ofertaEspera

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    /** Por qué la persona no puede reservar (membresía suspendida, vencida o inexistente); null si puede. */
    private val _restriccion = MutableLiveData<String?>()
    val restriccion: LiveData<String?> = _restriccion

    private val estado get() = _ui.value ?: ReservaEspacioUi()
    private val usuario get() = SesionManager.usuarioActual
    val esVisitanteExterno get() = usuario?.rol == Rol.VISITANTE_EXTERNO

    /** Socios y visitantes hacen fila en las horas llenas; el personal reserva sin lista de espera. */
    private val usaListaEspera get() = usuario?.rol?.esCliente() == true

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
            ?: return Textos.get("we_couldn_t_find_a_membership")
        return when (ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())) {
            EstadoMembresia.ACTIVA -> null
            EstadoMembresia.VENCIDA ->
                Textos.get("your_membership_expired_on_renew_it", Fechas.legible(membresia.fechaVencimiento))
            EstadoMembresia.SUSPENDIDA ->
                Textos.get("manual_membresia_suspendida", etiquetaMotivoSuspension(membresia.motivoSuspension).ifBlank { Textos.get("no_reason_recorded") })
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
                _mensaje.value = Textos.get("the_areas_could_not_be_loaded")
            }
        }
    }

    fun elegirDeporte(deporte: String) {
        _ui.value = estado.copy(
            deporte = deporte, area = null, horas = emptyList(), errorDisponibilidad = false, desde = null, hasta = null, personas = 1
        )
    }

    fun elegirArea(area: Area) {
        viewModelScope.launch { abrirArea(area) }
    }

    private suspend fun abrirArea(area: Area) {
        val (apertura, cierre) = rangoDeApertura(area)
        val texto = Textos.get("hours_open_max_hours", apertura.take(5), cierre.take(5), Catalogos.MAX_HORAS_POR_RESERVA) +
            if (esVisitanteExterno) Textos.get("requires_approval") else ""
        _ui.value = estado.copy(area = area, horarioTexto = texto, desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
        recalcularHoras(apertura, cierre)
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
            val esperas = filaDelDia(area.id, fecha)
            val ahora = Date()
            val primera = ReglasReserva.aMinutos(apertura)?.div(60) ?: return
            val ultima = (ReglasReserva.aMinutos(cierre)?.div(60) ?: return) - 1
            val horas = (primera..ultima).map { hora ->
                val base = ReglasReserva.estadoDeHora(area, reservas, torneos, fecha, hora)
                // Los lugares liberados que están apartados para quien va antes en la fila se ven ocupados.
                val fila = ReglasListaEspera.fila(esperas, area.id, fecha, hora, ahora)
                val apartados = usuario?.let { ReglasListaEspera.apartadosPara(it.id, fila, base.libres) } ?: 0
                HoraUi(
                    estado = base.copy(ocupadas = base.ocupadas + apartados),
                    pasada = Fechas.horasDesdeAhora(fecha, "%02d:00".format(hora)) < Catalogos.ANTICIPACION_MINIMA_HORAS,
                    enEspera = usuario?.let { ReglasListaEspera.posicion(it.id, fila) } != null
                )
            }
            _ui.value = estado.copy(horas = horas, errorDisponibilidad = false)
        } catch (e: Exception) {
            _ui.value = estado.copy(horas = emptyList(), errorDisponibilidad = true)
            _mensaje.value = Textos.get("the_availability_could_not_be_checked")
        }
    }

    /**
     * Lista de espera del área ese día (de todas las personas), después de apartar los lugares que se hayan liberado.
     * Si no se puede leer (sin red o sin permiso), se reserva como si no hubiera fila.
     */
    private suspend fun filaDelDia(areaId: String, fecha: String): List<EntradaEspera> {
        if (!usaListaEspera) return emptyList()
        runCatching { GestorListaEspera.promover(areaId, fecha) }
        return runCatching { listaEsperaRepository.obtenerDeArea(areaId, fecha) }.getOrDefault(emptyList())
    }

    fun tocarHora(hora: Int) {
        val actual = estado
        val celda = actual.horas.firstOrNull { it.hora == hora } ?: return
        if (!celda.seleccionable && usaListaEspera && celda.esperable) {
            when {
                _restriccion.value != null -> _mensaje.value = _restriccion.value
                celda.enEspera -> _mensaje.value = Textos.get("espera_ya_estas")
                else -> _ofertaEspera.value = hora
            }
            return
        }
        if (!celda.seleccionable) {
            _mensaje.value = when {
                celda.pasada -> Textos.get("that_hour_has_already_started")
                celda.estado.torneo != null -> Textos.get("at_that_time_the_area_is", celda.estado.torneo.nombre)
                else -> Textos.get("that_hour_has_reached_its_capacity", celda.estado.capacidad)
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
            _mensaje.value = Textos.get("the_lane_availability_could_not_be")
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
                    // Quien estaba en la lista de espera de esas horas ya consiguió su lugar: sale de la fila.
                    if (usaListaEspera) {
                        runCatching { listaEsperaRepository.marcarAtendidas(usuario.id, area.id, actual.fecha, desde..hasta) }
                    }
                    _mensaje.value = if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) {
                        Textos.get("request_sent_your_booking_is_under")
                    } else {
                        Textos.get("booking_confirmed_from_to", Deportes.titulo(area.tipo, area.nombre), actual.fecha, horaInicio, horaFin)
                    }
                }
                // Siempre se vuelve a leer la ocupación real, haya funcionado o no.
                _ui.value = estado.copy(desde = null, hasta = null, personas = 1, carril = null, carrilesDisponibles = emptyList())
                val (apertura, cierre) = rangoDeApertura(area)
                recalcularHoras(apertura, cierre)
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_booking_could_not_be_completed")
            }
            _ui.value = estado.copy(enviando = false)
        }
    }

    /**
     * Reglas del socio y del club + cupo/torneos. Usa datos recién leídos (no los de la pantalla),
     * así un cupo que se llenó mientras se elegía el horario se detecta antes de guardar.
     */
    private suspend fun validar(area: Area, fecha: String, horaInicio: String, horaFin: String, personas: Int, carril: Int?): String? {
        val usuario = usuario ?: return Textos.get("sign_in_to_book")
        motivoDeRestriccion()?.let { return it }
        if (area.carriles > 0) {
            if (carril == null || carril !in 1..area.carriles) return Textos.get("select_an_available_lane_2")
            val carrilOcupado = reservaRepository.obtenerReservasDeArea(area.id).any {
                it.fecha == fecha &&
                    it.carril == carril &&
                    it.horaInicio < horaFin &&
                    it.horaFin > horaInicio
            }
            if (carrilOcupado) return Textos.get("that_lane_has_just_been_taken")
        }

        if (esVisitanteExterno && !area.permiteExternos) return Textos.get("this_area_doesn_t_accept_bookings")
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return Textos.get("is_under_maintenance", etiquetaArea(area.nombre))
        if (Fechas.horasDesdeAhora(fecha, horaInicio) < Catalogos.ANTICIPACION_MINIMA_HORAS) {
            return Textos.get("that_time_has_already_started")
        }

        if (!usuario.rol.esPersonal()) {
            if (reservaRepository.estaBloqueadoPorInasistencias(usuario.id)) {
                return Textos.get("you_can_t_book_you_have", Catalogos.INASISTENCIAS_PARA_BLOQUEO) +
                    Textos.get("and_you_are_blocked_for_days", Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS)
            }
            if (reservaRepository.contarReservasActivas(usuario.id) >= Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO) {
                return Textos.get("you_have_reached_the_maximum_limit", Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO)
            }
        }

        val reservas = reservaRepository.obtenerReservasDeArea(area.id)
        val torneos = torneoRepository.obtenerTorneos()
        ReglasReserva.validarReserva(
            area = area,
            reservas = reservas,
            torneos = torneos,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            personas = personas
        )?.let { return it }

        // Un lugar que se liberó y está apartado para alguien de la lista de espera no se lo puede ganar otra persona.
        if (usaListaEspera) {
            val esperas = filaDelDia(area.id, fecha)
            val ahora = Date()
            val desde = (ReglasReserva.aMinutos(horaInicio) ?: 0) / 60
            val hasta = (ReglasReserva.aMinutos(horaFin) ?: 0) / 60
            for (hora in desde until hasta) {
                val base = ReglasReserva.estadoDeHora(area, reservas, torneos, fecha, hora)
                val apartados = ReglasListaEspera.apartadosPara(usuario.id, ReglasListaEspera.fila(esperas, area.id, fecha, hora, ahora), base.libres)
                if (apartados > 0 && base.libres - apartados < personas) {
                    return Textos.get("espera_lugar_apartado", ReglasListaEspera.textoHora(hora))
                }
            }
        }
        return null
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
                    titulo = area?.let { Deportes.titulo(it.tipo, it.nombre) } ?: Textos.get("area_deleted_2"),
                    emoji = area?.let { Deportes.emojiDe(it.tipo, it.emoji) } ?: "📍",
                    fecha = reserva.fecha,
                    horaInicio = reserva.horaInicio,
                    horaFin = reserva.horaFin,
                    personas = reserva.personas,
                    estado = reserva.estado,
                    deporte = area?.tipo ?: reserva.deporte,
                    carril = reserva.carril,
                    usuarioNombre = reserva.usuarioNombre,
                    areaId = reserva.areaId
                )
            }
        cargarEsperas()
    }

    /** Vuelve a leer la lista de espera de la persona (al regresar a la pantalla, por si ya le tocó). */
    fun refrescarEsperas() {
        if (!usaListaEspera) return
        viewModelScope.launch {
            cargarEsperas()
            // Si tiene abierta el área de alguna de sus esperas, la cuadrícula también cambia.
            estado.area?.let { area -> rangoDeApertura(area).let { (apertura, cierre) -> recalcularHoras(apertura, cierre) } }
        }
    }

    private suspend fun cargarEsperas() {
        val usuario = usuario ?: return
        if (!usaListaEspera) return
        try {
            val ahora = Date()
            var mias = listaEsperaRepository.obtenerDeUsuario(usuario.id).filter { ReglasListaEspera.vigente(it, ahora) }
            val dias = mias.map { it.areaId to it.fecha }.distinct()
            // Antes de mostrarlas se apartan los lugares que se hayan liberado: así se entera aunque no haya llegado la notificación.
            dias.forEach { (areaId, fecha) -> runCatching { GestorListaEspera.promover(areaId, fecha, ahora) } }
            mias = listaEsperaRepository.obtenerDeUsuario(usuario.id).filter { ReglasListaEspera.vigente(it, ahora) }
            val filas = dias.associateWith { (areaId, fecha) ->
                runCatching { listaEsperaRepository.obtenerDeArea(areaId, fecha) }.getOrDefault(emptyList())
            }
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            _esperas.value = mias.sortedWith(compareBy({ it.fecha }, { it.hora })).map { entrada ->
                val area = estado.areas.firstOrNull { it.id == entrada.areaId }
                val fila = ReglasListaEspera.fila(filas[entrada.areaId to entrada.fecha].orEmpty(), entrada.areaId, entrada.fecha, entrada.hora, ahora)
                EsperaListada(
                    entrada = entrada,
                    titulo = area?.let { Deportes.titulo(it.tipo, it.nombre) } ?: Deportes.titulo(entrada.deporte, entrada.areaNombre),
                    emoji = area?.let { Deportes.emojiDe(it.tipo, it.emoji) } ?: Deportes.emojiDe(entrada.deporte),
                    posicion = ReglasListaEspera.posicion(usuario.id, fila),
                    limite = if (entrada.estado == EstadoEspera.AVISADO) {
                        ReglasListaEspera.limiteMillis(entrada)?.let { formatoHora.format(Date(it)) }
                    } else {
                        null
                    }
                )
            }
        } catch (e: Exception) {
            // Sin red o sin permiso: se deja la lista que ya se tenía.
        }
    }

    fun unirseAEspera(personas: Int) {
        val hora = _ofertaEspera.value ?: return
        _ofertaEspera.value = null
        val area = estado.area ?: return
        val usuario = usuario ?: return
        val fecha = estado.fecha
        viewModelScope.launch {
            try {
                listaEsperaRepository.unirse(
                    EntradaEspera(
                        id = ReglasListaEspera.idDe(area.id, fecha, hora, usuario.id),
                        usuarioId = usuario.id,
                        usuarioNombre = usuario.nombre,
                        areaId = area.id,
                        areaNombre = area.nombre,
                        deporte = area.tipo,
                        fecha = fecha,
                        hora = hora,
                        personas = personas.coerceIn(1, area.capacidad.coerceAtLeast(1)),
                        estado = EstadoEspera.ESPERANDO,
                        creadoMillis = System.currentTimeMillis()
                    )
                )
                val fila = ReglasListaEspera.fila(listaEsperaRepository.obtenerDeArea(area.id, fecha), area.id, fecha, hora, Date())
                _mensaje.value = Textos.get("espera_unido", ReglasListaEspera.posicion(usuario.id, fila) ?: fila.size)
                val (apertura, cierre) = rangoDeApertura(area)
                recalcularHoras(apertura, cierre)
                cargarEsperas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("espera_error")
            }
        }
    }

    fun cerrarOfertaEspera() {
        _ofertaEspera.value = null
    }

    fun salirDeEspera(espera: EsperaListada) {
        viewModelScope.launch {
            try {
                listaEsperaRepository.salir(espera.entrada.id)
                // Si tenía un lugar apartado, ahora le toca al siguiente.
                runCatching { GestorListaEspera.promover(espera.entrada.areaId, espera.entrada.fecha) }
                _mensaje.value = Textos.get("espera_salir_ok")
                cargarEsperas()
                estado.area?.let { area -> rangoDeApertura(area).let { (apertura, cierre) -> recalcularHoras(apertura, cierre) } }
            } catch (e: Exception) {
                _mensaje.value = Textos.get("espera_error")
            }
        }
    }

    /** Lleva a la persona directo a la hora que se le liberó, ya seleccionada, para que solo confirme. */
    fun reservarDesdeEspera(espera: EsperaListada) {
        val area = estado.areas.firstOrNull { it.id == espera.entrada.areaId } ?: run {
            _mensaje.value = Textos.get("area_deleted_2")
            return
        }
        _ui.value = estado.copy(deporte = area.tipo, fecha = espera.entrada.fecha)
        viewModelScope.launch {
            abrirArea(area)
            tocarHora(espera.entrada.hora)
        }
    }

    fun cancelar(reserva: ReservaListada) {
        viewModelScope.launch {
            try {
                val sinPenalizacion = reservaRepository.cancelarReserva(reserva.id)
                _mensaje.value = if (sinPenalizacion) {
                    Textos.get("booking_cancelled")
                } else {
                    Textos.get("booking_cancelled_less_than_hours_in", Catalogos.CANCELACION_SIN_PENALIZACION_HORAS)
                }
                // El lugar que se liberó se aparta para el primero de la lista de espera de esas horas.
                if (reserva.areaId.isNotBlank()) runCatching { GestorListaEspera.promover(reserva.areaId, reserva.fecha) }
                estado.area?.let { elegirArea(it) }
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_booking_could_not_be_cancelled")
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
