package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.model.etiquetaMotivoSuspension
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MembresiaUiState(
    val titulo: String = "",
    val mensaje: String? = null,
    val detalle: String? = null,
    val beneficios: List<String> = emptyList(),
    val integrantes: List<IntegranteFamiliar> = emptyList(),
    val membresia: Membresia? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

class MembresiaViewModel(
    private val membresiaRepository: MembresiaRepository = AppContainer.membresiaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembresiaUiState())
    val uiState = _uiState.asStateFlow()

    private var membresiaJob: Job? = null
    private var usuarioObservado: String? = null
    private var observacionIniciada = false

    init {
        observarMembresia(SesionManager.usuarioActual?.id)
    }

    fun observarMembresia(usuarioId: String? = SesionManager.usuarioActual?.id) {
        if (observacionIniciada && usuarioObservado == usuarioId) return
        observacionIniciada = true
        usuarioObservado = usuarioId
        membresiaJob?.cancel()

        val usuario = SesionManager.usuarioActual?.takeIf { it.id == usuarioId }
        if (usuarioId.isNullOrBlank() || usuario == null) {
            _uiState.value = MembresiaUiState(cargando = true)
            return
        }
        _uiState.value = MembresiaUiState()

        if (usuario.rol != Rol.SOCIO && usuario.rol != Rol.VISITANTE_EXTERNO) {
            _uiState.update {
                it.copy(
                    titulo = Textos.get("not_applicable"),
                    mensaje = Textos.get("this_role_is_club_staff_and"),
                    cargando = false
                )
            }
            return
        }

        if (usuario.rol == Rol.VISITANTE_EXTERNO) {
            _uiState.update {
                it.copy(
                    titulo = Textos.get("visit_access"),
                    mensaje = Textos.get("n_0f_per_visit").format(Catalogos.PRECIO_VISITA),
                    detalle = Textos.get("one_time_payment_valid_for_a") +
                        Textos.get("recurring_bookings_each_visit_is_appro"),
                    cargando = false
                )
            }
            return
        }

        membresiaJob = viewModelScope.launch {
            var primeraRespuestaRecibida = false
            val timeoutJob = launch {
                delay(TIEMPO_ESPERA_INICIAL_MS)
                if (!primeraRespuestaRecibida && usuarioObservado == usuario.id) {
                    _uiState.update {
                        it.copy(
                            titulo = it.titulo.ifBlank { Textos.get("the_memberships_could_not_be_loaded") },
                            cargando = false,
                            error = Textos.get("membership_status_timeout")
                        )
                    }
                }
            }
            membresiaRepository.observarMembresia(usuario.id)
                .catch { error ->
                    if (error is CancellationException) throw error
                    primeraRespuestaRecibida = true
                    timeoutJob.cancel()
                    _uiState.update {
                        it.copy(
                            titulo = it.titulo.ifBlank { Textos.get("the_memberships_could_not_be_loaded") },
                            cargando = false,
                            error = error.message ?: Textos.get("the_memberships_could_not_be_loaded")
                        )
                    }
                }
                .collectLatest { membresia ->
                    primeraRespuestaRecibida = true
                    timeoutJob.cancel()
                    if (membresia == null) {
                        _uiState.value = MembresiaUiState(
                            titulo = Textos.get("no_active_membership"),
                            mensaje = Textos.get("you_don_t_have_a_package"),
                            cargando = false
                        )
                    } else {
                        _uiState.value = contenidoDe(membresia)
                        val integrantes = try {
                            if (membresia.tipo == TipoMembresia.FAMILIAR) {
                                membresiaRepository.obtenerIntegrantes(membresia.id)
                            } else {
                                emptyList()
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            _uiState.update {
                                it.copy(cargando = false, error = error.message ?: Textos.get("the_memberships_could_not_be_loaded"))
                            }
                            emptyList()
                        }
                        if (_uiState.value.membresia?.id == membresia.id) {
                            _uiState.update { it.copy(integrantes = integrantes) }
                        }
                    }
                }
        }
    }

    fun reintentar() {
        observacionIniciada = false
        observarMembresia(SesionManager.usuarioActual?.id)
    }

    private fun contenidoDe(membresia: Membresia): MembresiaUiState {
        val titulo: String
        val beneficios: List<String>
        when (membresia.tipo) {
            TipoMembresia.FAMILIAR -> {
                val paquete = membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it) }
                titulo = Textos.get("manual_paquete_nombre", paquete?.nombre ?: Textos.get("manual_familiar"))
                beneficios = listOfNotNull(paquete?.descripcion)
            }
            TipoMembresia.INDIVIDUAL -> {
                titulo = membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: Textos.get("individual")
                beneficios = membresia.plan?.let { Catalogos.beneficiosPlanIndividual(it) } ?: emptyList()
            }
            TipoMembresia.VISITA -> {
                titulo = Textos.get("visit_access")
                beneficios = emptyList()
            }
        }

        val estado = ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())
        val etiquetaEstado = when (estado) {
            EstadoMembresia.ACTIVA -> Textos.get("active_2")
            EstadoMembresia.VENCIDA -> Textos.get("expired")
            EstadoMembresia.SUSPENDIDA -> Textos.get("suspended")
        }
        val detalle = when (estado) {
            EstadoMembresia.ACTIVA ->
                Textos.get("valid_from_to", Fechas.legible(membresia.fechaInicio), Fechas.legible(membresia.fechaVencimiento))
            EstadoMembresia.VENCIDA ->
                Textos.get("expired_on_renew_it_at_the", Fechas.legible(membresia.fechaVencimiento))
            EstadoMembresia.SUSPENDIDA ->
                Textos.get("manual_motivo_punto", etiquetaMotivoSuspension(membresia.motivoSuspension).ifBlank { Textos.get("not_specified") }) +
                    Textos.get("talk_to_the_front_desk_to")
        }
        return MembresiaUiState(
            titulo = titulo,
            mensaje = Textos.get("per_month_2", etiquetaEstado, dinero(membresia.precio)),
            detalle = detalle,
            beneficios = beneficios,
            membresia = membresia,
            cargando = false
        )
    }

    private companion object {
        const val TIEMPO_ESPERA_INICIAL_MS = 12_000L
    }
}
