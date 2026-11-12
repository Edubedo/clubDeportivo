package com.example.clubdeportivo.ui.membresia

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.data.repository.RenovacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.util.comprimirComprobanteRenovacion

class RenovacionViewModel(
    private val repository: RenovacionRepository = AppContainer.renovacionRepository
) : ViewModel() {
    private val _solicitudPendiente = MutableStateFlow<SolicitudRenovacion?>(null)
    val solicitudPendiente = _solicitudPendiente.asStateFlow()

    private val _solicitudesPendientes = MutableStateFlow<List<SolicitudRenovacion>>(emptyList())
    val solicitudesPendientes = _solicitudesPendientes.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _enviado = MutableStateFlow(false)
    val enviado = _enviado.asStateFlow()

    private val _accionEnCurso = MutableStateFlow<String?>(null)
    val accionEnCurso = _accionEnCurso.asStateFlow()

    private var socioListener: Job? = null
    private var adminListener: Job? = null
    private var socioObservado: String? = null

    fun observarSolicitudSocio(socioId: String) {
        if (socioObservado == socioId) return
        socioObservado = socioId
        socioListener?.cancel()
        socioListener = viewModelScope.launch {
            repository.observarSolicitudPendiente(socioId)
                .catch { error ->
                    _error.value = "${Textos.get("renewal_member_listener_failed")} ${error.message.orEmpty()}"
                }
                .collect { _solicitudPendiente.value = it }
        }
    }

    fun observarPendientes() {
        if (adminListener?.isActive == true) return
        adminListener = viewModelScope.launch {
            repository.observarSolicitudesPendientes()
                .catch { error ->
                    _error.value = "${Textos.get("renewal_admin_listener_failed")} ${error.message.orEmpty()}"
                }
                .collect { _solicitudesPendientes.value = it }
        }
    }

    fun enviar(solicitud: SolicitudRenovacion, comprobante: Uri, resolver: ContentResolver) {
        if (_solicitudPendiente.value != null) {
            _error.value = Textos.get("renewal_duplicate_pending")
            return
        }
        viewModelScope.launch {
            _guardando.value = true
            _error.value = null
            try {
                val jpeg = withContext(Dispatchers.IO) {
                    comprimirComprobanteRenovacion(resolver, comprobante)
                } ?: error(Textos.get("renewal_proof_compress_failed"))
                val dataUri = "data:image/jpeg;base64," +
                    Base64.encodeToString(jpeg, Base64.NO_WRAP)
                repository.enviar(solicitud, dataUri)
                _enviado.value = true
            } catch (error: Exception) {
                _error.value = error.message ?: Textos.get("renewal_send_error")
            } finally {
                _guardando.value = false
            }
        }
    }

    fun aprobar(solicitud: SolicitudRenovacion) {
        ejecutarAccion(solicitud.id) { repository.aprobar(solicitud) }
    }

    fun rechazar(solicitud: SolicitudRenovacion) {
        ejecutarAccion(solicitud.id) { repository.rechazar(solicitud.id) }
    }

    private fun ejecutarAccion(id: String, accion: suspend () -> Unit) {
        viewModelScope.launch {
            _accionEnCurso.value = id
            _error.value = null
            try {
                accion()
            } catch (error: Exception) {
                _error.value = error.message ?: Textos.get("renewal_action_error")
            } finally {
                _accionEnCurso.value = null
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    fun limpiarEnviado() {
        _enviado.value = false
    }
}
