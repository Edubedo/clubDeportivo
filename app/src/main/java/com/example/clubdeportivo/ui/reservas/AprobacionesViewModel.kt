package com.example.clubdeportivo.ui.reservas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.launch

/**
 * Reservas de visitantes externos que esperan aprobación del personal. Un encargado de área solo ve las de su
 * deporte; el administrador ve todas.
 */
class AprobacionesViewModel : ViewModel() {

    private val repositorio = AppContainer.reservaRepository

    var pendientes by mutableStateOf<List<Reserva>?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    /** Reservas con una acción en curso (para no permitir tocar dos veces el mismo botón). */
    var enProceso by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                val hoy = Fechas.hoy()
                val usuario = SesionManager.usuarioActual
                val soloDeSuArea = usuario?.rol == Rol.AYUDANTE_AREA || usuario?.rol == Rol.ADMIN_AREA
                pendientes = repositorio.obtenerReservasVigentes()
                    .filter { it.estado == EstadoReserva.PENDIENTE_APROBACION && it.fecha >= hoy }
                    .filter { !soloDeSuArea || it.deporte.equals(usuario?.areaTrabajo, ignoreCase = true) }
                    .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            } catch (e: Exception) {
                pendientes = pendientes ?: emptyList()
                mensaje = "No se pudieron cargar las reservas por aprobar."
            }
        }
    }

    fun aprobar(reserva: Reserva) = resolver(reserva, "Reserva aprobada.") { repositorio.aprobarReserva(reserva.id) }

    fun rechazar(reserva: Reserva) = resolver(reserva, "Reserva rechazada.") { repositorio.rechazarReserva(reserva.id) }

    private fun resolver(reserva: Reserva, exito: String, accion: suspend () -> Unit) {
        if (reserva.id in enProceso) return
        viewModelScope.launch {
            enProceso = enProceso + reserva.id
            try {
                accion()
                pendientes = pendientes?.filterNot { it.id == reserva.id }
                mensaje = exito
            } catch (e: Exception) {
                mensaje = "No se pudo completar la acción. Intenta de nuevo."
            }
            enProceso = enProceso - reserva.id
        }
    }

    fun onMensajeMostrado() {
        mensaje = null
    }
}
