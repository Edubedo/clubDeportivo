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
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

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
            _titulo.value = Textos.get(R.string.not_applicable)
            _mensaje.value = Textos.get(R.string.this_role_is_club_staff_and)
            return
        }

        if (usuario.rol == Rol.VISITANTE_EXTERNO) {
            _titulo.value = Textos.get(R.string.visit_access)
            _mensaje.value = Textos.get(R.string.n_0f_per_visit).format(Catalogos.PRECIO_VISITA)
            _detalle.value = Textos.get(R.string.one_time_payment_valid_for_a) +
                Textos.get(R.string.recurring_bookings_each_visit_is_appro)
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            val membresia = membresiaRepository.obtenerMembresia(usuario.id)
            if (membresia == null) {
                _titulo.value = Textos.get(R.string.no_active_membership)
                _mensaje.value = Textos.get(R.string.you_don_t_have_a_package)
                _cargando.value = false
                return@launch
            }

            when (membresia.tipo) {
                TipoMembresia.FAMILIAR -> {
                    val paquete = membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it) }
                    _titulo.value = Textos.get(R.string.manual_paquete_nombre, paquete?.nombre ?: Textos.get(R.string.manual_familiar))
                    _beneficios.value = listOfNotNull(paquete?.descripcion)
                    _integrantes.value = membresiaRepository.obtenerIntegrantes(membresia.id)
                }
                TipoMembresia.INDIVIDUAL -> {
                    _titulo.value = membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: Textos.get(R.string.individual)
                    _beneficios.value = membresia.plan?.let { Catalogos.beneficiosPlanIndividual(it) } ?: emptyList()
                }
                TipoMembresia.VISITA -> {
                    _titulo.value = Textos.get(R.string.visit_access)
                }
            }

            val estadoActual = ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy())
            val estadoTexto = when (estadoActual) {
                EstadoMembresia.ACTIVA -> Textos.get(R.string.active_2)
                EstadoMembresia.VENCIDA -> Textos.get(R.string.expired)
                EstadoMembresia.SUSPENDIDA -> Textos.get(R.string.suspended)
            }
            _mensaje.value = Textos.get(R.string.per_month_2, estadoTexto, dinero(membresia.precio))
            _detalle.value = when (estadoActual) {
                EstadoMembresia.ACTIVA ->
                    Textos.get(R.string.valid_from_to, Fechas.legible(membresia.fechaInicio), Fechas.legible(membresia.fechaVencimiento))
                EstadoMembresia.VENCIDA ->
                    Textos.get(R.string.expired_on_renew_it_at_the, Fechas.legible(membresia.fechaVencimiento))
                EstadoMembresia.SUSPENDIDA ->
                    Textos.get(R.string.manual_motivo_punto, membresia.motivoSuspension.ifBlank { Textos.get(R.string.not_specified) }) +
                        Textos.get(R.string.talk_to_the_front_desk_to)
            }
            _cargando.value = false
        }
    }
}
