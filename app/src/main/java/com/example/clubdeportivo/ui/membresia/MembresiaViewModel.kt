package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.repository.MembresiaRepository
import kotlinx.coroutines.launch

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
            _titulo.value = "No aplica"
            _mensaje.value = "Este rol es personal del club y no tiene membresía de socio."
            return
        }

        if (usuario.rol == Rol.VISITANTE_EXTERNO) {
            _titulo.value = "Acceso de visita"
            _mensaje.value = "$%.0f por visita.".format(Catalogos.PRECIO_VISITA)
            _detalle.value = "Pago único, válido para un solo día. No incluye reservaciones " +
                "recurrentes: cada visita se aprueba por separado."
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            val membresia = membresiaRepository.obtenerMembresia(usuario.id)
            if (membresia == null) {
                _titulo.value = "Sin membresía activa"
                _mensaje.value = "Todavía no tienes un paquete contratado."
                _cargando.value = false
                return@launch
            }

            when (membresia.tipo) {
                TipoMembresia.FAMILIAR -> {
                    val paquete = membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it) }
                    _titulo.value = "Paquete ${paquete?.nombre ?: "familiar"}"
                    _beneficios.value = listOfNotNull(paquete?.descripcion)
                    _integrantes.value = membresiaRepository.obtenerIntegrantes(membresia.id)
                }
                TipoMembresia.INDIVIDUAL -> {
                    _titulo.value = membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: "Individual"
                    _beneficios.value = membresia.plan?.let { Catalogos.beneficiosPlanIndividual(it) } ?: emptyList()
                }
                TipoMembresia.VISITA -> {
                    _titulo.value = "Acceso de visita"
                }
            }

            _mensaje.value = "${membresia.estado.name} · $%.0f al mes".format(membresia.precio)
            _detalle.value = "Vigente del ${membresia.fechaInicio} al ${membresia.fechaVencimiento}"
            _cargando.value = false
        }
    }
}
