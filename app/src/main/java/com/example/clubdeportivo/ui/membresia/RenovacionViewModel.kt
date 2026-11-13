package com.example.clubdeportivo.ui.membresia

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.data.repository.RenovacionRepository
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.util.comprimirComprobanteRenovacion
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RenovacionUiState(
    val solicitudSocio: SolicitudRenovacion? = null,
    val solicitudLista: Boolean = false,
    val errorConsultaSolicitud: Boolean = false,
    val solicitudesPendientes: List<SolicitudRenovacion> = emptyList(),
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val accionEnCurso: String? = null,
    val error: String? = null
)

class RenovacionViewModel(
    private val repository: RenovacionRepository = AppContainer.renovacionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RenovacionUiState())
    val uiState = _uiState.asStateFlow()

    private var socioListener: Job? = null
    private var adminListener: Job? = null
    private var socioObservado: String? = null
    private var solicitudRemota: SolicitudRenovacion? = null
    private var solicitudOptimista: SolicitudRenovacion? = null

    fun observarSolicitudSocio(socioId: String) {
        if (socioObservado == socioId) return
        socioObservado = socioId
        socioListener?.cancel()
        solicitudRemota = null
        solicitudOptimista = null
        _uiState.update {
            it.copy(
                solicitudSocio = null,
                solicitudLista = false,
                errorConsultaSolicitud = false,
                enviando = false,
                enviado = false,
                error = null
            )
        }
        socioListener = viewModelScope.launch {
                val timeoutJob = launch {
                    delay(TIEMPO_ESPERA_INICIAL_MS)
                    if (!_uiState.value.solicitudLista && socioObservado == socioId) {
                        socioObservado = null
                        _uiState.update {
                            it.copy(
                                errorConsultaSolicitud = true,
                                error = Textos.get("renewal_status_timeout")
                            )
                        }
                    }
                }
                try {
                    repository.observarUltimaSolicitud(socioId)
                        .catch { error ->
                            if (error is kotlinx.coroutines.CancellationException) throw error
                            socioObservado = null
                            _uiState.update {
                                it.copy(
                                    errorConsultaSolicitud = true,
                                    error = "${Textos.get("renewal_member_listener_failed")} ${error.message.orEmpty()}"
                                )
                            }
                        }
                        .collect { remota ->
                            timeoutJob.cancel()
                            solicitudRemota = remota
                            if (solicitudOptimista?.id == remota?.id) solicitudOptimista = null
                            _uiState.update {
                                it.copy(
                                    solicitudSocio = solicitudOptimista ?: remota,
                                    solicitudLista = true,
                                    errorConsultaSolicitud = false
                                )
                            }
                        }
                } finally {
                    timeoutJob.cancel()
                }
        }
    }

    fun observarPendientes() {
        if (adminListener?.isActive == true) return
        adminListener = viewModelScope.launch {
            repository.observarSolicitudesPendientes()
                .catch { error ->
                    _uiState.update {
                        it.copy(error = "${Textos.get("renewal_admin_listener_failed")} ${error.message.orEmpty()}")
                    }
                }
                .collect { pendientes ->
                    _uiState.update { it.copy(solicitudesPendientes = pendientes) }
                }
        }
    }

    fun enviar(solicitud: SolicitudRenovacion, comprobante: Uri, resolver: ContentResolver) {
        val actual = _uiState.value
        if (actual.enviando || actual.solicitudSocio?.status == SolicitudRenovacion.PENDIENTE) {
            _uiState.update { it.copy(error = Textos.get("renewal_duplicate_pending")) }
            return
        }

        val optimista = solicitud.copy(
            id = UUID.randomUUID().toString(),
            status = SolicitudRenovacion.PENDIENTE,
            createdAt = System.currentTimeMillis(),
            motivoRechazo = ""
        )
        solicitudOptimista = optimista
        _uiState.update {
            it.copy(solicitudSocio = optimista, enviando = true, enviado = false, error = null)
        }

        viewModelScope.launch {
            try {
                val jpeg = withContext(Dispatchers.IO) {
                    comprimirComprobanteRenovacion(resolver, comprobante)
                } ?: error(Textos.get("renewal_proof_compress_failed"))
                val dataUri = "data:image/jpeg;base64," +
                    Base64.encodeToString(jpeg, Base64.NO_WRAP)
                val guardada = repository.enviar(optimista, dataUri)
                solicitudOptimista = if (solicitudRemota?.id == guardada.id) null else guardada
                _uiState.update {
                    it.copy(
                        solicitudSocio = solicitudOptimista ?: solicitudRemota,
                        enviando = false,
                        enviado = true
                    )
                }
            } catch (error: Exception) {
                solicitudOptimista = null
                _uiState.update {
                    it.copy(
                        solicitudSocio = solicitudRemota,
                        enviando = false,
                        enviado = false,
                        error = error.message ?: Textos.get("renewal_send_error")
                    )
                }
            }
        }
    }

    fun aprobar(solicitud: SolicitudRenovacion) {
        ejecutarAccion(solicitud.id) { repository.aprobar(solicitud) }
    }

    fun rechazar(solicitud: SolicitudRenovacion, motivo: String) {
        ejecutarAccion(solicitud.id) { repository.rechazar(solicitud.id, motivo) }
    }

    private fun ejecutarAccion(id: String, accion: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(accionEnCurso = id, error = null) }
            try {
                accion()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(error = error.message ?: Textos.get("renewal_action_error"))
                }
            } finally {
                _uiState.update { it.copy(accionEnCurso = null) }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    fun limpiarEnviado() {
        _uiState.update { it.copy(enviado = false) }
    }

    private companion object {
        const val TIEMPO_ESPERA_INICIAL_MS = 12_000L
    }
}
