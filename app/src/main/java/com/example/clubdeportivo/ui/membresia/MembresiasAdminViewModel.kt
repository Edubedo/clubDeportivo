package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MotivoSuspension
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.repository.GestionMembresiasRepository
import com.example.clubdeportivo.data.repository.PreciosRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Idioma
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.launch

/** Control de las membresías del club: alta de miembros (con su código), edición, renovación y suspensión. */
class MembresiasAdminViewModel(
    private val repositorio: GestionMembresiasRepository = AppContainer.gestionMembresiasRepository,
    private val preciosRepository: PreciosRepository = AppContainer.preciosRepository
) : ViewModel() {

    private val _membresias = MutableLiveData<List<MembresiaDetalle>>(emptyList())
    val membresias: LiveData<List<MembresiaDetalle>> = _membresias

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> = _cargando

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    /** Motivo por el que no se pudo guardar el formulario; se muestra dentro del propio formulario. */
    private val _errorFormulario = MutableLiveData<String?>()
    val errorFormulario: LiveData<String?> = _errorFormulario

    /** Sube cada vez que un guardado termina bien, para que la pantalla cierre el formulario. */
    private val _guardadoExitoso = MutableLiveData(0)
    val guardadoExitoso: LiveData<Int> = _guardadoExitoso

    /** Membresía recién registrada: se muestran sus códigos para entregarlos. */
    private val _registroReciente = MutableLiveData<MembresiaDetalle?>()
    val registroReciente: LiveData<MembresiaDetalle?> = _registroReciente

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _membresias.value = repositorio.obtenerTodas()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_memberships_could_not_be_loaded")
            }
            _cargando.value = false
        }
    }

    fun limpiarErrorFormulario() {
        _errorFormulario.value = null
    }

    /** Precios vigentes en este momento (editados o base), leídos al guardar para no usar uno desactualizado. */
    private suspend fun preciosActuales(): Map<String, Double> =
        try { preciosRepository.obtenerEditados() } catch (e: Exception) { emptyMap() }

    fun registrar(
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteId: Int?,
        personas: List<PersonaForm>,
        metodoPago: MetodoPago
    ) {
        val error = ReglasMembresia.validarPersonas(tipo, paqueteId, personas)
        if (error != null) {
            _errorFormulario.value = error
            return
        }
        viewModelScope.launch {
            _guardando.value = true
            _errorFormulario.value = null
            try {
                val precio = ClavesPrecio.deMembresia(tipo, plan, paqueteId, preciosActuales())
                _registroReciente.value = repositorio.registrar(tipo, plan, paqueteId, precio, personas, metodoPago)
                _guardadoExitoso.value = (_guardadoExitoso.value ?: 0) + 1
                cargar()
            } catch (e: Exception) {
                _errorFormulario.value = e.message ?: Textos.get("the_member_could_not_be_registered")
            }
            _guardando.value = false
        }
    }

    fun actualizar(
        detalle: MembresiaDetalle,
        plan: PlanIndividual?,
        paqueteId: Int?,
        personas: List<PersonaForm>,
        estado: EstadoMembresia?,
        motivoSuspension: String?
    ) {
        val membresia = detalle.membresia
        if (estado == EstadoMembresia.SUSPENDIDA && membresia.estado != EstadoMembresia.SUSPENDIDA && motivoSuspension.isNullOrBlank()) {
            _errorFormulario.value = Textos.get("choose_why_the_membership_is_being")
            return
        }
        val anteriorALosCodigos = detalle.personas.any { it.codigo.isBlank() }
        if (!anteriorALosCodigos) {
            val error = ReglasMembresia.validarPersonas(membresia.tipo, paqueteId, personas)
            if (error != null) {
                _errorFormulario.value = error
                return
            }
        }
        viewModelScope.launch {
            _guardando.value = true
            _errorFormulario.value = null
            try {
                // Si cambia el plan, pasa al precio vigente del nuevo plan; si no, conserva el que ya tenía.
                val cambioPlan = plan != membresia.plan || paqueteId != membresia.paqueteFamiliarId
                val precio = if (cambioPlan) {
                    ClavesPrecio.deMembresia(membresia.tipo, plan, paqueteId, preciosActuales())
                } else {
                    membresia.precio
                }
                if (anteriorALosCodigos) {
                    repositorio.cambiarPlan(membresia.id, plan, paqueteId, precio)
                } else {
                    repositorio.actualizar(membresia.id, plan, paqueteId, precio, personas)
                }
                if (estado != null && estado != membresia.estado) repositorio.cambiarEstado(membresia.id, estado, motivoSuspension)
                _mensaje.value = Textos.get("changes_saved_2")
                _guardadoExitoso.value = (_guardadoExitoso.value ?: 0) + 1
                cargar()
            } catch (e: Exception) {
                _errorFormulario.value = e.message ?: Textos.get("the_changes_could_not_be_saved")
            }
            _guardando.value = false
        }
    }

    fun cambiarEstado(detalle: MembresiaDetalle, estado: EstadoMembresia, motivo: String? = null) {
        viewModelScope.launch {
            try {
                repositorio.cambiarEstado(detalle.membresia.id, estado, motivo)
                _mensaje.value = if (estado == EstadoMembresia.SUSPENDIDA) {
                    Textos.get("membership_suspended_while_it_is_they")
                } else {
                    Textos.get("membership_reactivated")
                }
                cargar()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_status_could_not_be_changed")
            }
        }
    }

    /** Baja del miembro: ya no puede iniciar sesión (y si está dentro de la app, se le cierra la sesión). */
    fun darDeBaja(detalle: MembresiaDetalle) {
        viewModelScope.launch {
            try {
                repositorio.darDeBaja(detalle.membresia.id, Textos.en(Idioma.ES, MotivoSuspension.CLAVE_BAJA_MIEMBRO))
                _mensaje.value = Textos.get("miembro_baja_ok")
                cargar()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("miembro_baja_error")
            }
        }
    }

    fun renovar(detalle: MembresiaDetalle, metodoPago: MetodoPago) {
        val membresia = detalle.membresia
        viewModelScope.launch {
            try {
                val precio = ClavesPrecio.deMembresia(membresia.tipo, membresia.plan, membresia.paqueteFamiliarId, preciosActuales())
                repositorio.renovar(membresia.id, precio, metodoPago)
                _mensaje.value = Textos.get("payment_of_recorded_membership_renewed", dinero(precio)) +
                    "${Fechas.legible(ReglasMembresia.fechasDeRenovacion(membresia, Fechas.hoy()).second)}."
                cargar()
            } catch (e: Exception) {
                _mensaje.value = Textos.get("the_membership_could_not_be_renewed")
            }
        }
    }

    /** Libera el código de [codigo] para que su dueño pueda volver a registrarse (por ejemplo, si se equivocó de correo). */
    fun restablecerAcceso(codigo: String) {
        viewModelScope.launch {
            _guardando.value = true
            try {
                repositorio.restablecerAcceso(codigo)
                _mensaje.value = Textos.get("done_can_sign_up_again", codigo)
                _guardadoExitoso.value = (_guardadoExitoso.value ?: 0) + 1
                cargar()
            } catch (e: Exception) {
                _mensaje.value = e.message ?: Textos.get("the_access_could_not_be_reset")
            }
            _guardando.value = false
        }
    }

    fun cerrarRegistroReciente() {
        _registroReciente.value = null
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
