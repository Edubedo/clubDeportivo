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
        carriles: Int = 0
    ) {
        if (nombreRepetido(nombre, tipo, existente?.id)) {
            _mensaje.value = Textos.get("already_exists_in", nombre.trim(), tipo)
            return
        }
        viewModelScope.launch {
            try {
                if (existente == null) {
                    val creada = areaRepository.crearArea(nombre.trim(), tipo.trim(), capacidad, emoji.trim(), carriles)
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
                            carriles = carriles
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
