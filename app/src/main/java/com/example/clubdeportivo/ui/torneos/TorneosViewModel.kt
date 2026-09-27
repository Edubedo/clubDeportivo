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
        _mostrarModalCrear.value = true
    }

    fun cerrarModalCrear() {
        _mostrarModalCrear.value = false
    }

    fun crearTorneo(nombre: String, disciplina: String, areaId: String, fechaInicio: String, fechaFin: String, cupoMaximo: Int) {
        viewModelScope.launch {
            torneoRepository.crearTorneo(nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo)
            _mensaje.value = "Torneo creado: $nombre"
            _mostrarModalCrear.value = false
            cargarTorneos()
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
