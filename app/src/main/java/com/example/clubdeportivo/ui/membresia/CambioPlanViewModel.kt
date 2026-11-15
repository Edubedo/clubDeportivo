package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.SolicitudCambioPlan
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esSupervisor
import com.example.clubdeportivo.data.repository.CambioPlanRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasCambioPlan
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CambioPlanUiState(
    /** La última solicitud del miembro (la más reciente, sea cual sea su estado). */
    val ultima: SolicitudCambioPlan? = null,
    val ultimaLista: Boolean = false,
    /** Solicitudes esperando respuesta (las ve el personal que las resuelve). */
    val pendientes: List<SolicitudCambioPlan> = emptyList(),
    val enviando: Boolean = false,
    val accionEnCurso: String? = null,
    /** Mensaje para mostrar una vez (aviso de éxito o de error). */
    val mensaje: String? = null
)

class CambioPlanViewModel(
    private val repository: CambioPlanRepository = AppContainer.cambioPlanRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(CambioPlanUiState())
    val ui = _ui.asStateFlow()

    private var socioObservado: String? = null
    private var jobSocio: Job? = null
    private var jobPendientes: Job? = null

    fun observarSocio(socioId: String) {
        if (socioObservado == socioId) return
        socioObservado = socioId
        jobSocio?.cancel()
        jobSocio = viewModelScope.launch {
            repository.observarUltima(socioId)
                .catch { if (it is CancellationException) throw it; socioObservado = null }
                .collect { ultima -> _ui.update { it.copy(ultima = ultima, ultimaLista = true) } }
        }
    }

    fun observarPendientes() {
        if (jobPendientes?.isActive == true) return
        jobPendientes = viewModelScope.launch {
            repository.observarPendientes()
                .catch { if (it is CancellationException) throw it }
                .collect { lista -> _ui.update { it.copy(pendientes = lista) } }
        }
    }

    /** Un miembro pide pasar de su plan a [nuevo]. Las reglas (activa, sin otra solicitud...) se revisan aquí y en Firestore. */
    fun solicitar(membresia: Membresia, nuevo: PlanIndividual) {
        val usuario = SesionManager.usuarioActual ?: return
        val actual = membresia.plan ?: return
        if (_ui.value.enviando) return
        viewModelScope.launch {
            _ui.update { it.copy(enviando = true, mensaje = null) }
            try {
                val hayRenovacion = repository.hayRenovacionPendiente(membresia.id, socioId = usuario.id)
                val motivo = ReglasCambioPlan.motivoNoPuedePedir(
                    membresia = membresia,
                    hoy = Fechas.hoy(),
                    nuevo = nuevo,
                    hayCambioPendiente = _ui.value.ultima?.status == SolicitudCambioPlan.PENDIENTE,
                    hayRenovacionPendiente = hayRenovacion
                )
                if (motivo != null) {
                    _ui.update { it.copy(enviando = false, mensaje = motivo) }
                    return@launch
                }
                repository.enviar(membresia.id, actual, membresia.precio, nuevo, usuario.nombre)
                _ui.update { it.copy(enviando = false, mensaje = Textos.get("cambio_plan_enviada")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(enviando = false, mensaje = Textos.get("cambio_plan_error_envio")) }
            }
        }
    }

    fun aprobar(solicitud: SolicitudCambioPlan) = accion(solicitud.id, Textos.get("cambio_plan_aprobado_ok")) {
        repository.aprobar(solicitud)
    }

    fun rechazar(solicitud: SolicitudCambioPlan, motivo: String) = accion(solicitud.id, Textos.get("cambio_plan_rechazado_ok")) {
        repository.rechazar(solicitud.id, motivo)
    }

    private fun accion(id: String, exito: String, bloque: suspend () -> Unit) {
        // Quien resuelve es un administrador o un supervisor (Firestore lo exige también).
        val rol = SesionManager.usuarioActual?.rol
        if (rol?.esAdministrador() != true && rol?.esSupervisor() != true) return
        viewModelScope.launch {
            _ui.update { it.copy(accionEnCurso = id, mensaje = null) }
            try {
                bloque()
                _ui.update { it.copy(mensaje = exito) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(mensaje = e.message ?: Textos.get("cambio_plan_error_accion")) }
            } finally {
                _ui.update { it.copy(accionEnCurso = null) }
            }
        }
    }

    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null) }
}
