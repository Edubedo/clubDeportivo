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
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

/** Una membresía junto al nombre del socio y sus integrantes, ya resueltos para el panel de administración. */
data class MembresiaAdminItem(
    val membresia: Membresia,
    val nombreSocio: String,
    val integrantes: List<IntegranteFamiliar>
)

class MembresiaViewModel(
    private val membresiaRepository: MembresiaRepository = AppContainer.membresiaRepository,
    private val usuarioRepository: UsuarioRepository = AppContainer.usuarioRepository
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

    // --- Estado del panel de administración ---

    private val _membresiasAdmin = MutableLiveData<List<MembresiaAdminItem>>(emptyList())
    val membresiasAdmin: LiveData<List<MembresiaAdminItem>> = _membresiasAdmin

    private val _socios = MutableLiveData<List<Usuario>>(emptyList())
    val socios: LiveData<List<Usuario>> = _socios

    private val _mostrarModalCrear = MutableLiveData(false)
    val mostrarModalCrear: LiveData<Boolean> = _mostrarModalCrear

    private val _membresiaEnEdicion = MutableLiveData<MembresiaAdminItem?>(null)
    val membresiaEnEdicion: LiveData<MembresiaAdminItem?> = _membresiaEnEdicion

    private val _mensajeAdmin = MutableLiveData<String?>(null)
    val mensajeAdmin: LiveData<String?> = _mensajeAdmin

    init {
        val usuario = SesionManager.usuarioActual
        if (usuario?.rol == Rol.SUPERADMIN || usuario?.rol == Rol.ADMIN) {
            cargarMembresiasAdmin()
            viewModelScope.launch { _socios.value = usuarioRepository.obtenerSocios() }
        } else {
            cargarMembresia()
        }
    }

    private fun cargarMembresia() {
        val usuario = SesionManager.usuarioActual ?: return

        // El personal del club (todos los roles salvo socio y visitante) no
        // tiene membresía: trabaja ahí, no es cliente.
        if (usuario.rol != Rol.SOCIO && usuario.rol != Rol.VISITANTE_EXTERNO) {
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

    fun cargarMembresiasAdmin() {
        viewModelScope.launch {
            _cargando.value = true
            val socios = usuarioRepository.obtenerSocios()
            _membresiasAdmin.value = membresiaRepository.obtenerMembresias().map { membresia ->
                MembresiaAdminItem(
                    membresia = membresia,
                    nombreSocio = socios.find { it.id == membresia.usuarioId }?.nombre ?: "Socio desconocido",
                    integrantes = if (membresia.tipo == TipoMembresia.FAMILIAR) {
                        membresiaRepository.obtenerIntegrantes(membresia.id)
                    } else {
                        emptyList()
                    }
                )
            }
            _cargando.value = false
        }
    }

    fun abrirModalCrear() {
        _membresiaEnEdicion.value = null
        _mostrarModalCrear.value = true
    }

    fun abrirModalEditar(item: MembresiaAdminItem) {
        _membresiaEnEdicion.value = item
        _mostrarModalCrear.value = true
    }

    fun cerrarModal() {
        _mostrarModalCrear.value = false
        _membresiaEnEdicion.value = null
    }

    fun guardarMembresia(
        usuarioId: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String
    ) {
        val precio = when (tipo) {
            TipoMembresia.INDIVIDUAL -> plan?.let { Catalogos.precioPlanIndividual(it) } ?: 0.0
            TipoMembresia.FAMILIAR -> paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it)?.precioMensual } ?: 0.0
            TipoMembresia.VISITA -> Catalogos.PRECIO_VISITA
        }

        viewModelScope.launch {
            val idExistente = _membresiaEnEdicion.value?.membresia?.id
            if (idExistente == null) {
                membresiaRepository.crearMembresia(usuarioId, tipo, plan, paqueteFamiliarId, precio, fechaInicio, fechaVencimiento)
                _mensajeAdmin.value = "Membresía creada."
            } else {
                membresiaRepository.actualizarMembresia(
                    idExistente, tipo, plan, paqueteFamiliarId, precio, estado, fechaInicio, fechaVencimiento
                )
                _mensajeAdmin.value = "Membresía actualizada."
            }
            _mostrarModalCrear.value = false
            _membresiaEnEdicion.value = null
            cargarMembresiasAdmin()
        }
    }

    fun agregarIntegrante(membresiaId: String, nombre: String, parentesco: String) {
        viewModelScope.launch {
            membresiaRepository.agregarIntegrante(membresiaId, nombre, parentesco)
            cargarMembresiasAdmin()
        }
    }

    fun eliminarIntegrante(integranteId: String) {
        viewModelScope.launch {
            membresiaRepository.eliminarIntegrante(integranteId)
            cargarMembresiasAdmin()
        }
    }

    fun onMensajeAdminMostrado() {
        _mensajeAdmin.value = null
    }
}
