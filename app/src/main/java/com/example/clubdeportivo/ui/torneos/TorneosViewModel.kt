package com.example.clubdeportivo.ui.torneos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import kotlinx.coroutines.launch

class TorneosViewModel(
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository
) : ViewModel() {

    private val _torneos = MutableLiveData<List<Torneo>>()
    val torneos: LiveData<List<Torneo>> = _torneos

    private val _areas = MutableLiveData<List<Area>>(emptyList())
    val areas: LiveData<List<Area>> = _areas

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _mostrarModalCrear = MutableLiveData(false)
    val mostrarModalCrear: LiveData<Boolean> = _mostrarModalCrear

    private val _torneoEnEdicion = MutableLiveData<Torneo?>(null)
    val torneoEnEdicion: LiveData<Torneo?> = _torneoEnEdicion

    init {
        cargarTorneos()
        viewModelScope.launch { _areas.value = areaRepository.obtenerAreas() }
    }

    fun cargarTorneos() {
        viewModelScope.launch {
            _cargando.value = true
            _torneos.value = torneoRepository.obtenerTorneos()
            _cargando.value = false
        }
    }

    fun inscribirse(torneo: Torneo) {
        val usuarioId = SesionManager.usuarioActual?.id ?: return
        viewModelScope.launch {
            torneoRepository.inscribirse(torneo.id, usuarioId)
            _mensaje.value = "Te inscribiste a ${torneo.nombre}"
            cargarTorneos()
        }
    }

    fun abrirModalCrear() {
        _torneoEnEdicion.value = null
        _mostrarModalCrear.value = true
    }

    fun abrirModalEditar(torneo: Torneo) {
        _torneoEnEdicion.value = torneo
        _mostrarModalCrear.value = true
    }

    fun cerrarModalCrear() {
        _mostrarModalCrear.value = false
        _torneoEnEdicion.value = null
    }

    fun guardarTorneo(
        id: String?,
        nombre: String,
        disciplina: String,
        areaId: String,
        fechaInicio: String,
        fechaFin: String,
        cupoMaximo: Int,
        horaInicio: String,
        horaFin: String
    ) {
        viewModelScope.launch {
            if (id == null) {
                torneoRepository.crearTorneo(nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, horaInicio, horaFin)
                _mensaje.value = "Torneo creado: $nombre"
            } else {
                torneoRepository.actualizarTorneo(id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, horaInicio, horaFin)
                _mensaje.value = "Torneo actualizado: $nombre"
            }
            _mostrarModalCrear.value = false
            _torneoEnEdicion.value = null
            cargarTorneos()
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
