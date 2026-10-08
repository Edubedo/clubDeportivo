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
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.util.ReglasReserva
import kotlinx.coroutines.launch

class TorneosViewModel(
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository
) : ViewModel() {

    private val _torneos = MutableLiveData<List<Torneo>>()
    val torneos: LiveData<List<Torneo>> = _torneos

    /** Torneos en los que la persona que tiene la sesión ya está inscrita. */
    private val _inscritos = MutableLiveData<Set<String>>(emptySet())
    val inscritos: LiveData<Set<String>> = _inscritos

    private val _areas = MutableLiveData<List<Area>>(emptyList())
    val areas: LiveData<List<Area>> = _areas

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _mostrarModalCrear = MutableLiveData(false)
    val mostrarModalCrear: LiveData<Boolean> = _mostrarModalCrear

    /** Motivo por el que no se pudo guardar el torneo (choque con reservas u otro torneo); se muestra dentro del formulario. */
    private val _errorFormulario = MutableLiveData<String?>()
    val errorFormulario: LiveData<String?> = _errorFormulario

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
            SesionManager.usuarioActual?.id?.let { uid ->
                _inscritos.value = runCatching { torneoRepository.torneosInscritos(uid) }.getOrDefault(_inscritos.value.orEmpty())
            }
            _cargando.value = false
        }
    }

    fun inscribirse(torneo: Torneo) {
        val usuarioId = SesionManager.usuarioActual?.id ?: return
        viewModelScope.launch {
            try {
                torneoRepository.inscribirse(torneo.id, usuarioId)
                _mensaje.value = "Te inscribiste a ${torneo.nombre}"
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "No se pudo completar la inscripción."
            }
            cargarTorneos()
        }
    }

    fun abrirModalCrear() {
        _errorFormulario.value = null
        _torneoEnEdicion.value = null
        _mostrarModalCrear.value = true
    }

    fun abrirModalEditar(torneo: Torneo) {
        _errorFormulario.value = null
        _torneoEnEdicion.value = torneo
        _mostrarModalCrear.value = true
    }

    fun cerrarModalCrear() {
        _errorFormulario.value = null
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
            // Un torneo ocupa el área completa: no puede chocar con reservas vigentes ni con otro torneo.
            // Se lee todo de nuevo para validar contra la ocupación real, no contra lo que se ve en pantalla.
            val area = areaRepository.obtenerAreaPorId(areaId)
            if (area == null) {
                _errorFormulario.value = "El área elegida ya no existe."
                return@launch
            }
            val conflicto = ReglasReserva.validarTorneo(
                area = area,
                reservas = reservaRepository.obtenerReservasDeArea(areaId),
                torneos = torneoRepository.obtenerTorneos(),
                torneoIdExcluido = id,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                horaInicio = horaInicio,
                horaFin = horaFin
            )
            if (conflicto != null) {
                _errorFormulario.value = conflicto
                return@launch
            }
            _errorFormulario.value = null

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

    fun eliminarTorneo(torneo: Torneo) {
        viewModelScope.launch {
            try {
                torneoRepository.eliminarTorneo(torneo.id)
                _mensaje.value = "Torneo eliminado: ${torneo.nombre}"
            } catch (e: Exception) {
                _mensaje.value = "No se pudo eliminar el torneo. Inténtalo de nuevo."
            }
            cargarTorneos()
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
