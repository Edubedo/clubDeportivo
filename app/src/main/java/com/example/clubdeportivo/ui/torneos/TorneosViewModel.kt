package com.example.clubdeportivo.ui.torneos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.data.repository.TorneoRepository
import kotlinx.coroutines.launch

class TorneosViewModel(
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository
) : ViewModel() {

    private val _torneos = MutableLiveData<List<Torneo>>()
    val torneos: LiveData<List<Torneo>> = _torneos

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    init {
        cargarTorneos()
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

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
