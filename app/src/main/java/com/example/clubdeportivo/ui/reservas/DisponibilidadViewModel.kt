package com.example.clubdeportivo.ui.reservas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.RestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasReserva
import kotlinx.coroutines.launch

/** Una hora del día de un área con su ocupación: lo que ve el encargado para saber si le queda lugar. */
data class FilaDisponibilidad(
    val estado: ReglasReserva.EstadoHora,
    /** Lugares pedidos en solicitudes todavía en revisión (ya están dentro de [ReglasReserva.EstadoHora.ocupadas]). */
    val enRevision: Int
) {
    val hora get() = estado.hora
}

/** Disponibilidad por hora de las áreas del encargado (o de todas, si lo abre un administrador). */
class DisponibilidadViewModel(
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository,
    private val horarioRepository: RestriccionHorarioRepository = AppContainer.restriccionHorarioRepository
) : ViewModel() {

    var cargando by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var areas by mutableStateOf<List<Area>>(emptyList())
        private set
    var area by mutableStateOf<Area?>(null)
        private set
    var fecha by mutableStateOf(Fechas.hoy())
        private set
    var filas by mutableStateOf<List<FilaDisponibilidad>>(emptyList())
        private set

    val fechas = Fechas.proximosDias(Catalogos.ANTICIPACION_MAXIMA_DIAS + 1)

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val usuario = SesionManager.usuarioActual
                areas = areaRepository.obtenerAreas().filter { usuario.puedeVerDeporte(it.tipo) }
                area = areas.firstOrNull { it.id == area?.id } ?: areas.firstOrNull()
                recalcular()
            } catch (e: Exception) {
                error = "No se pudo consultar la disponibilidad."
            }
            cargando = false
        }
    }

    fun elegirArea(nueva: Area) {
        area = nueva
        refrescar()
    }

    fun elegirFecha(nueva: String) {
        fecha = nueva
        refrescar()
    }

    private fun refrescar() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                recalcular()
            } catch (e: Exception) {
                error = "No se pudo consultar la disponibilidad."
            }
            cargando = false
        }
    }

    private suspend fun recalcular() {
        val actual = area ?: run { filas = emptyList(); return }
        val reservas = reservaRepository.obtenerReservasDeArea(actual.id)
        val torneos = torneoRepository.obtenerTorneos()
        val horario = horarioRepository.obtenerHorarioDeArea(actual)
        val primera = (ReglasReserva.aMinutos(horario.horaInicio) ?: return) / 60
        val ultima = (ReglasReserva.aMinutos(horario.horaFin) ?: return) / 60 - 1
        filas = (primera..ultima).map { hora ->
            FilaDisponibilidad(
                estado = ReglasReserva.estadoDeHora(actual, reservas, torneos, fecha, hora),
                enRevision = ReglasReserva.personasEnRevision(actual.id, reservas, fecha, hora)
            )
        }
    }
}
