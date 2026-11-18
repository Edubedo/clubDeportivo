package com.example.clubdeportivo.ui.areas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.launch
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaArea

/** Gestión de áreas (canchas, albercas, gimnasio...): alta, edición y baja sobre la colección real de áreas. */
class AreasViewModel(
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository
) : ViewModel() {

    private val _areas = MutableLiveData<List<Area>>()
    val areas: LiveData<List<Area>> = _areas

    /** Cuántas reservas vigentes tiene hoy cada área (por id de área). */
    private val _reservasHoy = MutableLiveData<Map<String, Int>>(emptyMap())
    val reservasHoy: LiveData<Map<String, Int>> = _reservasHoy

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    /** Reservas vigentes que caen en el periodo de mantenimiento que se está eligiendo (null mientras se calcula). */
    private val _afectadas = MutableLiveData<Int?>(null)
    val afectadas: LiveData<Int?> = _afectadas

    private var calculoAfectadas: kotlinx.coroutines.Job? = null

    init {
        cargarAreas()
    }

    fun cargarAreas() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                // Un encargado solo ve las áreas de su deporte.
                val usuario = SesionManager.usuarioActual
                _areas.value = areaRepository.obtenerAreas().filter { usuario.puedeVerDeporte(it.tipo) }
                val hoy = Fechas.hoy()
                _reservasHoy.value = reservaRepository.obtenerReservasVigentes()
                    .filter { it.fecha == hoy && it.estado == EstadoReserva.CONFIRMADA }
                    .groupingBy { it.areaId }
                    .eachCount()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_areas_could_not_be_loaded")
            }
            _cargando.value = false
        }
    }

    /** true si ya hay otra área del mismo deporte con ese nombre. */
    fun nombreRepetido(nombre: String, tipo: String, excluirId: String?): Boolean =
        _areas.value.orEmpty().any {
            it.id != excluirId &&
                it.tipo.equals(tipo.trim(), ignoreCase = true) &&
                it.nombre.trim().equals(nombre.trim(), ignoreCase = true)
        }

    fun guardarArea(
        existente: Area?,
        nombre: String,
        tipo: String,
        capacidad: Int,
        emoji: String,
        disponibilidad: DisponibilidadArea = DisponibilidadArea.DISPONIBLE,
        carriles: Int = 0,
        imagenUrl: String = ""
    ) {
        if (nombreRepetido(nombre, tipo, existente?.id)) {
            _mensaje.value = Textos.get("already_exists_in", nombre.trim(), tipo)
            return
        }
        viewModelScope.launch {
            try {
                if (existente == null) {
                    val creada = areaRepository.crearArea(
                        nombre.trim(),
                        tipo.trim(),
                        capacidad,
                        emoji.trim(),
                        carriles,
                        imagenUrl
                    )
                    if (disponibilidad != creada.disponibilidad) {
                        areaRepository.actualizarArea(creada.copy(disponibilidad = disponibilidad))
                    }
                    _mensaje.value = Textos.get("area_added", nombre.trim())
                } else {
                    val hoy = Fechas.hoy()
                    val excedida = reservaRepository.obtenerReservasDeArea(existente.id)
                        .any { it.fecha >= hoy && it.personas > capacidad }
                    if (excedida) {
                        _mensaje.value = Textos.get("there_are_future_bookings_for_more", capacidad)
                        return@launch
                    }
                    areaRepository.actualizarArea(
                        existente.copy(
                            nombre = nombre.trim(),
                            tipo = tipo.trim(),
                            capacidad = capacidad,
                            emoji = emoji.trim(),
                            disponibilidad = disponibilidad,
                            carriles = carriles,
                            imagenUrl = imagenUrl
                        )
                    )
                    _mensaje.value = Textos.get("area_updated", nombre.trim())
                }
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_area_could_not_be_saved")
            }
        }
    }

    /** Reservas vigentes de [areaId] que todavía no terminan y caen entre [desde] y [hasta]: las que se cancelarían. */
    private suspend fun reservasEnPeriodo(areaId: String, desde: String, hasta: String): List<Reserva> =
        reservaRepository.obtenerReservasDeArea(areaId).filter {
            it.fecha in desde..hasta && Fechas.horasDesdeAhora(it.fecha, it.horaFin) > 0
        }

    /** Cuenta cuántas reservas se cancelarían con ese periodo, para avisarlo antes de confirmar. */
    fun calcularAfectadas(areaId: String, desde: String, hasta: String) {
        calculoAfectadas?.cancel()
        _afectadas.value = null
        calculoAfectadas = viewModelScope.launch {
            _afectadas.value = try {
                reservasEnPeriodo(areaId, desde, hasta).size
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                0
            }
        }
    }

    fun limpiarAfectadas() {
        calculoAfectadas?.cancel()
        _afectadas.value = null
    }

    /**
     * Pone el área en mantenimiento solo entre [desde] y [hasta]: bloquea esos días para reservar y cancela (sin penalización)
     * las reservas que ya había en ellos. A cada persona se le avisa en la app y por correo (lo hace el servidor al ver la
     * cancelación) y puede reagendar desde "Mis reservas".
     */
    fun ponerEnMantenimiento(areaId: String, desde: String, hasta: String, motivo: String) {
        if (hasta < desde) {
            _mensaje.value = Textos.get("maintenance_end_before_start")
            return
        }
        viewModelScope.launch {
            try {
                // Siempre sobre la versión guardada: el formulario pudo haber cambiado otros datos justo antes.
                val area = areaRepository.obtenerAreaPorId(areaId) ?: run {
                    _mensaje.value = Textos.get("the_area_status_could_not_be")
                    return@launch
                }
                val afectadas = reservasEnPeriodo(area.id, desde, hasta)
                areaRepository.actualizarArea(
                    area.copy(
                        disponibilidad = DisponibilidadArea.MANTENIMIENTO,
                        mantenimientoDesde = desde,
                        mantenimientoHasta = hasta,
                        mantenimientoMotivo = motivo
                    )
                )
                if (afectadas.isNotEmpty()) {
                    reservaRepository.cancelarPorMantenimiento(afectadas, desde, hasta, motivo)
                }
                _mensaje.value = Textos.get(
                    "maintenance_scheduled", etiquetaArea(area.nombre), Fechas.legibleConDia(desde), Fechas.legibleConDia(hasta), afectadas.size
                )
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_area_status_could_not_be")
            }
        }
    }

    /** Cambia solo el estatus del área (acción rápida del menú de la tarjeta). */
    fun cambiarDisponibilidad(area: Area, nueva: DisponibilidadArea) {
        if (area.disponibilidad == nueva) return
        viewModelScope.launch {
            try {
                areaRepository.actualizarArea(area.copy(disponibilidad = nueva))
                _mensaje.value = if (nueva == DisponibilidadArea.MANTENIMIENTO) {
                    Textos.get("is_now_under_maintenance_it_no", etiquetaArea(area.nombre))
                } else {
                    Textos.get("is_available_again", etiquetaArea(area.nombre))
                }
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_area_status_could_not_be")
            }
        }
    }

    fun eliminarArea(area: Area) {
        viewModelScope.launch {
            try {
                val hoy = Fechas.hoy()
                val conReservas = reservaRepository.obtenerReservasDeArea(area.id).any { it.fecha >= hoy }
                val conTorneo = torneoRepository.obtenerTorneos().any { it.areaId == area.id && it.fechaFin >= hoy }
                if (conReservas || conTorneo) {
                    _mensaje.value = Textos.get("can_t_be_deleted_it_has", etiquetaArea(area.nombre))
                    return@launch
                }
                areaRepository.eliminarArea(area.id)
                _mensaje.value = Textos.get("area_deleted", etiquetaArea(area.nombre))
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_area_could_not_be_deleted")
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
