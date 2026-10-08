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
                _mensaje.value = "No se pudieron cargar las áreas."
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
        disponibilidad: DisponibilidadArea = DisponibilidadArea.DISPONIBLE
    ) {
        if (nombreRepetido(nombre, tipo, existente?.id)) {
            _mensaje.value = "Ya existe \"${nombre.trim()}\" en $tipo."
            return
        }
        viewModelScope.launch {
            try {
                if (existente == null) {
                    val creada = areaRepository.crearArea(nombre.trim(), tipo.trim(), capacidad, emoji.trim())
                    if (disponibilidad != creada.disponibilidad) {
                        areaRepository.actualizarArea(creada.copy(disponibilidad = disponibilidad))
                    }
                    _mensaje.value = "Área agregada: ${nombre.trim()}"
                } else {
                    val hoy = Fechas.hoy()
                    val excedida = reservaRepository.obtenerReservasDeArea(existente.id)
                        .any { it.fecha >= hoy && it.personas > capacidad }
                    if (excedida) {
                        _mensaje.value = "Hay reservas futuras para más personas que la nueva capacidad ($capacidad)."
                        return@launch
                    }
                    areaRepository.actualizarArea(
                        existente.copy(
                            nombre = nombre.trim(),
                            tipo = tipo.trim(),
                            capacidad = capacidad,
                            emoji = emoji.trim(),
                            disponibilidad = disponibilidad
                        )
                    )
                    _mensaje.value = "Área actualizada: ${nombre.trim()}"
                }
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo guardar el área."
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
                    "${area.nombre} quedó en mantenimiento: ya no admite reservas nuevas. Las que ya existen se conservan."
                } else {
                    "${area.nombre} está disponible otra vez."
                }
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo cambiar el estatus del área."
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
                    _mensaje.value = "No se puede eliminar ${area.nombre}: tiene reservas o torneos pendientes."
                    return@launch
                }
                areaRepository.eliminarArea(area.id)
                _mensaje.value = "Área eliminada: ${area.nombre}"
                cargarAreas()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo eliminar el área."
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
