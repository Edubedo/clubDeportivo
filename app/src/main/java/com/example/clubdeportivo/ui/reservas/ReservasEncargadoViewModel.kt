package com.example.clubdeportivo.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Reserva
import kotlinx.coroutines.launch
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

class ReservasEncargadoViewModel : ViewModel() {

    private val reservaRepository = AppContainer.reservaRepository

    private val _reservas = MutableLiveData<List<Reserva>>(emptyList())
    val reservas: LiveData<List<Reserva>> = _reservas

    // Guarda la asistencia usando el id de la reserva
    private val _asistencias =
        MutableLiveData<Map<String, String>>(emptyMap())

    val asistencias: LiveData<Map<String, String>> = _asistencias

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val areaTrabajo: String
        get() = SesionManager.usuarioActual?.areaTrabajo.orEmpty()

    init {
        cargarReservas()
    }

    fun cargarReservas() {

        val deporte = areaTrabajo

        if (deporte.isBlank()) {
            _error.value = Textos.get(R.string.you_don_t_have_a_work)
            return
        }

        viewModelScope.launch {

            _cargando.value = true
            _error.value = null

            try {

                val lista =
                    reservaRepository
                        .obtenerReservasPorDeporte(deporte)
                        .sortedWith(
                            compareBy<Reserva> { it.fecha }
                                .thenBy { it.horaInicio }
                        )

                _reservas.value = lista

                // Cargar asistencias que ya estén registradas
                val mapaAsistencias =
                    mutableMapOf<String, String>()

                lista.forEach { reserva ->

                    val asistencia =
                        reservaRepository.obtenerAsistencia(
                            reserva.id
                        )

                    if (asistencia != null) {
                        mapaAsistencias[reserva.id] = asistencia
                    }
                }

                _asistencias.value = mapaAsistencias

            } catch (e: Exception) {

                _error.value =
                    Textos.get(R.string.the_bookings_could_not_be_loaded)

                e.printStackTrace()
            }

            _cargando.value = false
        }
    }

    fun registrarAsistencia(
        reservaId: String,
        usuarioId: String,
        asistencia: String
    ) {

        viewModelScope.launch {

            try {

                reservaRepository.registrarAsistencia(
                    reservaId = reservaId,
                    usuarioId = usuarioId,
                    asistencia = asistencia
                )

                // Actualizamos inmediatamente la pantalla
                val mapa =
                    _asistencias.value
                        .orEmpty()
                        .toMutableMap()

                mapa[reservaId] = asistencia

                _asistencias.value = mapa

            } catch (e: Exception) {

                _error.value =
                    Textos.get(R.string.attendance_could_not_be_recorded)

                e.printStackTrace()
            }
        }
    }
}