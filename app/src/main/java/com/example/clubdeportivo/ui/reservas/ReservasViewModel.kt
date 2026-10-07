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

/** Una hora de la cuadrícula ("hora" = 6 es 06:00-07:00) con su estado de ocupación. */
data class HoraUi(
    val estado: ReglasReserva.EstadoHora,
    /** Ya no cumple la anticipación mínima (o ya pasó). */
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
    val personas: Int = 1
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
    val deporte: String
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
            ?: return "No encontramos una membresía a tu nombre. Habla con recepción para poder reservar."
        return when (ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())) {
            EstadoMembresia.ACTIVA -> null
            EstadoMembresia.VENCIDA ->
                "Tu membresía venció el ${Fechas.legible(membresia.fechaVencimiento)}. Renuévala en recepción para volver a reservar."
            EstadoMembresia.SUSPENDIDA ->
                "Tu membresía está suspendida (${membresia.motivoSuspension.ifBlank { "sin motivo registrado" }}). Habla con recepción."
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
                _mensaje.value = "No se pudieron cargar las áreas."
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
            val texto = "Horario · abierto ${apertura.take(5)} – ${cierre.take(5)} · máx. ${Catalogos.MAX_HORAS_POR_RESERVA} horas" +
                if (esVisitanteExterno) " (requiere aprobación)" else ""
            _ui.value = estado.copy(area = area, horarioTexto = texto, desde = null, hasta = null, personas = 1)
            recalcularHoras(apertura, cierre)
        }
    }

    fun elegirFecha(fecha: String) {
        _ui.value = estado.copy(fecha = fecha, desde = null, hasta = null, personas = 1)
        val area = estado.area ?: return
        viewModelScope.launch {
            val (apertura, cierre) = rangoDeApertura(area)
            recalcularHoras(apertura, cierre)
        }
    }

    /** Horario del área; los visitantes externos solo pueden reservar dentro de su ventana. */
    private suspend fun rangoDeApertura(area: Area): Pair<String, String> {
        val horario = restriccionHorarioRepository.obtenerHorarioDeArea(area)
        return if (esVisitanteExterno) {
            maxOf(Catalogos.HORA_INICIO_EXTERNOS, horario.horaInicio) to minOf(Catalogos.HORA_FIN_EXTERNOS, horario.horaFin)
        } else {
            horario.horaInicio to horario.horaFin
        }
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
            _mensaje.value = "No se pudo consultar la disponibilidad."
        }
    }

    fun tocarHora(hora: Int) {
        val actual = estado
        val celda = actual.horas.firstOrNull { it.hora == hora } ?: return
        if (!celda.seleccionable) {
            _mensaje.value = when {
                celda.pasada -> "Esa hora ya pasó o no cumple la anticipación mínima de ${Catalogos.ANTICIPACION_MINIMA_HORAS} horas."
                celda.estado.torneo != null -> "A esa hora el área está reservada para el torneo \"${celda.estado.torneo.nombre}\"."
                else -> "Esa hora ya alcanzó su cupo (${celda.estado.capacidad} personas). Elige otra hora."
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
    }

    fun limpiarSeleccion() {
        _ui.value = estado.copy(desde = null, hasta = null, personas = 1)
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
                val error = validar(area, actual.fecha, horaInicio, horaFin, actual.personas)
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
                        personas = actual.personas
                    )
                    _mensaje.value = if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) {
                        "Reserva enviada: queda pendiente de aprobación."
                    } else {
                        "¡Reserva confirmada! ${Deportes.titulo(area.tipo, area.nombre)}, ${actual.fecha} de $horaInicio a $horaFin."
                    }
                }
                // Siempre se vuelve a leer la ocupación real, haya funcionado o no.
                _ui.value = estado.copy(desde = null, hasta = null, personas = 1)
                val (apertura, cierre) = rangoDeApertura(area)
                recalcularHoras(apertura, cierre)
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = "No se pudo completar la reserva. Inténtalo de nuevo."
            }
            _ui.value = estado.copy(enviando = false)
        }
    }

    /**
     * Reglas del socio y del club + cupo/torneos. Usa datos recién leídos (no los de la pantalla),
     * así un cupo que se llenó mientras se elegía el horario se detecta antes de guardar.
     */
    private suspend fun validar(area: Area, fecha: String, horaInicio: String, horaFin: String, personas: Int): String? {
        val usuario = usuario ?: return "Inicia sesión para reservar."
        motivoDeRestriccion()?.let { return it }

        if (esVisitanteExterno && !area.permiteExternos) return "Esta área no admite reservaciones de visitantes externos."
        if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) return "${area.nombre} está en mantenimiento."
        if (Fechas.horasDesdeAhora(fecha, horaInicio) < Catalogos.ANTICIPACION_MINIMA_HORAS) {
            return "Ese horario ya no cumple la anticipación mínima de ${Catalogos.ANTICIPACION_MINIMA_HORAS} horas."
        }

        if (!usuario.rol.esPersonal()) {
            if (reservaRepository.estaBloqueadoPorInasistencias(usuario.id)) {
                return "No puedes reservar: tienes inasistencias recientes (${Catalogos.INASISTENCIAS_PARA_BLOQUEO} faltas) " +
                    "y estás bloqueado por ${Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS} días."
            }
            if (reservaRepository.contarReservasActivas(usuario.id) >= Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO) {
                return "Ya tienes ${Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO} reservaciones activas. Cancela alguna antes de crear una nueva."
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
        val todas = reservaRepository.obtenerReservasVigentes()
        val visibles = if (usuario.rol.esPersonal()) todas else todas.filter { it.usuarioId == usuario.id }
        _reservas.value = visibles
            .filter { it.fecha >= hoy }
            .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            .map { reserva ->
                val area = areas.firstOrNull { it.id == reserva.areaId }
                ReservaListada(
                    id = reserva.id,
                    titulo = area?.let { Deportes.titulo(it.tipo, it.nombre) } ?: "Área eliminada",
                    emoji = area?.let { Deportes.emojiDe(it.tipo, it.emoji) } ?: "📍",
                    fecha = reserva.fecha,
                    horaInicio = reserva.horaInicio,
                    horaFin = reserva.horaFin,
                    personas = reserva.personas,
                    estado = reserva.estado,
                    deporte = area?.tipo ?: ""
                )
            }
    }

    fun cancelar(reserva: ReservaListada) {
        viewModelScope.launch {
            try {
                val sinPenalizacion = reservaRepository.cancelarReserva(reserva.id)
                _mensaje.value = if (sinPenalizacion) {
                    "Reserva cancelada."
                } else {
                    "Reserva cancelada con menos de ${Catalogos.CANCELACION_SIN_PENALIZACION_HORAS} horas de anticipación."
                }
                estado.area?.let { elegirArea(it) }
                cargarLista(estado.areas)
            } catch (e: Exception) {
                _mensaje.value = "No se pudo cancelar la reserva."
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
