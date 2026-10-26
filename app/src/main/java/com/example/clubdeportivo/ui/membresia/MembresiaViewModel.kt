package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import kotlinx.coroutines.launch
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaMotivoSuspension

class MembresiaViewModel(
    private val membresiaRepository: MembresiaRepository = AppContainer.membresiaRepository
) : ViewModel() {

    private val _titulo = MutableLiveData("")
    val titulo: LiveData<String> = _titulo

    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    private val _detalle = MutableLiveData<String?>(null)
    val detalle: LiveData<String?> = _detalle

    private val _beneficios = MutableLiveData<List<String>>(emptyList())
    val beneficios: LiveData<List<String>> = _beneficios

    private val _integrantes = MutableLiveData<List<IntegranteFamiliar>>(emptyList())
    val integrantes: LiveData<List<IntegranteFamiliar>> = _integrantes

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    init {
        cargarMembresia()
    }

    private fun cargarMembresia() {
        val usuario = SesionManager.usuarioActual ?: return

        // El personal del club (todos los roles salvo socio y visitante) no
        // tiene membresía: trabaja ahí, no es cliente.
        if (usuario.rol != Rol.SOCIO && usuario.rol != Rol.VISITANTE_EXTERNO) {
            _titulo.value = Textos.get("not_applicable")
            _mensaje.value = Textos.get("this_role_is_club_staff_and")
            return
        }

        if (usuario.rol == Rol.VISITANTE_EXTERNO) {
            _titulo.value = Textos.get("visit_access")
            _mensaje.value = Textos.get("n_0f_per_visit").format(Catalogos.PRECIO_VISITA)
            _detalle.value = Textos.get("one_time_payment_valid_for_a") +
                Textos.get("recurring_bookings_each_visit_is_appro")
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            val membresia = membresiaRepository.obtenerMembresia(usuario.id)
            if (membresia == null) {
                _titulo.value = Textos.get("no_active_membership")
                _mensaje.value = Textos.get("you_don_t_have_a_package")
                _cargando.value = false
                return@launch
            }

            when (membresia.tipo) {
                TipoMembresia.FAMILIAR -> {
                    val paquete = membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it) }
                    _titulo.value = Textos.get("manual_paquete_nombre", paquete?.nombre ?: Textos.get("manual_familiar"))
                    _beneficios.value = listOfNotNull(paquete?.descripcion)
                    _integrantes.value = membresiaRepository.obtenerIntegrantes(membresia.id)
                }
                TipoMembresia.INDIVIDUAL -> {
                    _titulo.value = membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: Textos.get("individual")
                    _beneficios.value = membresia.plan?.let { Catalogos.beneficiosPlanIndividual(it) } ?: emptyList()
                }
                TipoMembresia.VISITA -> {
                    _titulo.value = Textos.get("visit_access")
                }
            }

            val estadoActual = ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())
            val estadoTexto = when (estadoActual) {
                EstadoMembresia.ACTIVA -> Textos.get("active_2")
                EstadoMembresia.VENCIDA -> Textos.get("expired")
                EstadoMembresia.SUSPENDIDA -> Textos.get("suspended")
            }
            _mensaje.value = Textos.get("per_month_2", estadoTexto, dinero(membresia.precio))
            _detalle.value = when (estadoActual) {
                EstadoMembresia.ACTIVA ->
                    Textos.get("valid_from_to", Fechas.legible(membresia.fechaInicio), Fechas.legible(membresia.fechaVencimiento))
                EstadoMembresia.VENCIDA ->
                    Textos.get("expired_on_renew_it_at_the", Fechas.legible(membresia.fechaVencimiento))
                EstadoMembresia.SUSPENDIDA ->
                    Textos.get("manual_motivo_punto", etiquetaMotivoSuspension(membresia.motivoSuspension).ifBlank { Textos.get("not_specified") }) +
                        Textos.get("talk_to_the_front_desk_to")
            }
            _cargando.value = false
        }
    }
}
