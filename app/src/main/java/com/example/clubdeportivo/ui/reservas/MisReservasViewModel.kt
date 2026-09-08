package com.example.clubdeportivo.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import kotlinx.coroutines.launch

data class ReservaConNombreArea(
    val reservaId: Int,
    val nombreArea: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: EstadoReserva
)

class MisReservasViewModel(
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository
) : ViewModel() {

    private val _reservas = MutableLiveData<List<ReservaConNombreArea>>()
    val reservas: LiveData<List<ReservaConNombreArea>> = _reservas

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensajeCancelacion = MutableLiveData<String?>()
    val mensajeCancelacion: LiveData<String?> = _mensajeCancelacion

    init {
        cargarReservas()
    }

    fun cargarReservas() {
        val usuarioId = SesionManager.usuarioActual?.id ?: return

        viewModelScope.launch {
            _cargando.value = true
            val reservas = reservaRepository.obtenerReservasDeUsuario(usuarioId)
            _reservas.value = reservas.map { reserva ->
                val area = areaRepository.obtenerAreaPorId(reserva.areaId)
                ReservaConNombreArea(
                    reservaId = reserva.id,
                    nombreArea = area?.nombre ?: "Área #${reserva.areaId}",
                    fecha = reserva.fecha,
                    horaInicio = reserva.horaInicio,
                    horaFin = reserva.horaFin,
                    estado = reserva.estado
                )
            }
            _cargando.value = false
        }
    }

    fun cancelar(reserva: ReservaConNombreArea) {
        viewModelScope.launch {
            val sinPenalizacion = reservaRepository.cancelarReserva(reserva.reservaId)
            _mensajeCancelacion.value = if (sinPenalizacion) {
                "Reserva cancelada sin penalización."
            } else {
                "Reserva cancelada con menos de 4 horas de anticipación (puede aplicar penalización)."
            }
            cargarReservas()
        }
    }

    fun onMensajeCancelacionMostrado() {
        _mensajeCancelacion.value = null
    }
}
