package com.example.clubdeportivo.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import kotlinx.coroutines.launch
import com.example.clubdeportivo.util.Textos

data class ReservasAdministradorUi(
    val cargando: Boolean = true,
    val areas: List<Area> = emptyList(),
    val reservas: List<Reserva> = emptyList(),
    val areaIdSeleccionada: String? = null,
    val error: String? = null
)

class ReservasAdministradorViewModel(
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository
) : ViewModel() {

    private val _ui = MutableLiveData(ReservasAdministradorUi())
    val ui: LiveData<ReservasAdministradorUi> = _ui

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _ui.value = (_ui.value ?: ReservasAdministradorUi()).copy(cargando = true, error = null)
            try {
                val areas = areaRepository.obtenerAreas()
                val reservas = reservaRepository.obtenerReservasVigentes()
                    .sortedWith(compareBy<Reserva> { it.fecha }.thenBy { it.horaInicio })
                _ui.value = (_ui.value ?: ReservasAdministradorUi()).copy(
                    cargando = false,
                    areas = areas,
                    reservas = reservas,
                    areaIdSeleccionada = _ui.value?.areaIdSeleccionada?.takeIf { id ->
                        areas.any { it.id == id }
                    }
                )
            } catch (e: Exception) {
                _ui.value = (_ui.value ?: ReservasAdministradorUi()).copy(
                    cargando = false,
                    error = Textos.get("the_bookings_could_not_be_loaded")
                )
            }
        }
    }

    fun seleccionarArea(areaId: String?) {
        _ui.value = (_ui.value ?: ReservasAdministradorUi()).copy(areaIdSeleccionada = areaId)
    }
}
