
package com.example.clubdeportivo.ui.reservas

import com.example.clubdeportivo.util.Textos
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Rol
import kotlinx.coroutines.launch

class ReservasSupervisorViewModel : ViewModel() {

    private val repository = AppContainer.reservaRepository

    private val _reservas = MutableLiveData<List<Reserva>>(emptyList())
    val reservas: LiveData<List<Reserva>> = _reservas

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    init {
        cargarReservas()
    }

    private fun esSupervisor(): Boolean {
        return SesionManager.usuarioActual?.rol == Rol.SUPERVISOR
    }

    fun cargarReservas() {
        if (!esSupervisor()) {
            _error.value = Textos.get("supvm_sin_permiso")
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            try {
                _reservas.value = repository
                    .obtenerReservasVigentes()
                    .sortedWith(
                        compareBy<Reserva> { it.fecha }
                            .thenBy { it.horaInicio }
                    )
            } catch (e: Exception) {
                _error.value = Textos.get("supvm_no_cargo")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun aprobarReserva(reservaId: String) {
        if (!esSupervisor()) return

        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            try {
                val reserva = _reservas.value
                    .orEmpty()
                    .firstOrNull { it.id == reservaId }

                if (reserva?.estado != EstadoReserva.PENDIENTE_APROBACION) {
                    _error.value = Textos.get("supvm_ya_no_pendiente")
                    return@launch
                }

                repository.aprobarReserva(reservaId)
                _mensaje.value = Textos.get("supvm_aprobada")

                _reservas.value = repository
                    .obtenerReservasVigentes()
                    .sortedWith(
                        compareBy<Reserva> { it.fecha }
                            .thenBy { it.horaInicio }
                    )

            } catch (e: Exception) {
                android.util.Log.e(
                    "RESERVAS_SUPERVISOR",
                    "Error al aprobar reserva",
                    e
                )

                _error.value = Textos.get("supvm_error_aprobar", e.message.orEmpty())
            } finally {
                _cargando.value = false
            }
        }
    }

    fun rechazarReserva(reservaId: String, motivo: String) {
        if (!esSupervisor()) return

        if (motivo.isBlank()) {
            _error.value = Textos.get("supvm_motivo_obligatorio")
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            try {
                val reserva = _reservas.value
                    .orEmpty()
                    .firstOrNull { it.id == reservaId }

                if (reserva?.estado != EstadoReserva.PENDIENTE_APROBACION) {
                    _error.value = Textos.get("supvm_ya_no_pendiente")
                    return@launch
                }

                repository.rechazarReserva(
                    reservaId = reservaId,
                    motivo = motivo.trim()
                )

                _mensaje.value = Textos.get("supvm_rechazada")

                _reservas.value = repository
                    .obtenerReservasVigentes()
                    .sortedWith(
                        compareBy<Reserva> { it.fecha }
                            .thenBy { it.horaInicio }
                    )

            } catch (e: Exception) {
                android.util.Log.e(
                    "RESERVAS_SUPERVISOR",
                    "Error al rechazar reserva",
                    e
                )

                _error.value = Textos.get("supvm_error_rechazar", e.message.orEmpty())
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun limpiarError() {
        _error.value = null
    }
}
