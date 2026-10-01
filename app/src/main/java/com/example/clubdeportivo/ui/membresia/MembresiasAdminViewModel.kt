package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.repository.GestionMembresiasRepository
import com.example.clubdeportivo.data.repository.PreciosRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
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
                _mensaje.value = "No se pudieron cargar las membresías."
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

    fun registrar(tipo: TipoMembresia, plan: PlanIndividual?, paqueteId: Int?, personas: List<PersonaForm>) {
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
                _registroReciente.value = repositorio.registrar(tipo, plan, paqueteId, precio, personas)
                _guardadoExitoso.value = (_guardadoExitoso.value ?: 0) + 1
                cargar()
            } catch (e: Exception) {
                _errorFormulario.value = e.message ?: "No se pudo registrar al miembro."
            }
            _guardando.value = false
        }
    }

    fun actualizar(
        detalle: MembresiaDetalle,
        plan: PlanIndividual?,
        paqueteId: Int?,
        personas: List<PersonaForm>,
        estado: EstadoMembresia?
    ) {
        val membresia = detalle.membresia
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
                if (estado != null && estado != membresia.estado) repositorio.cambiarEstado(membresia.id, estado)
                _mensaje.value = "Cambios guardados."
                _guardadoExitoso.value = (_guardadoExitoso.value ?: 0) + 1
                cargar()
            } catch (e: Exception) {
                _errorFormulario.value = e.message ?: "No se pudieron guardar los cambios."
            }
            _guardando.value = false
        }
    }

    fun cambiarEstado(detalle: MembresiaDetalle, estado: EstadoMembresia) {
        viewModelScope.launch {
            try {
                repositorio.cambiarEstado(detalle.membresia.id, estado)
                _mensaje.value = if (estado == EstadoMembresia.SUSPENDIDA) "Membresía suspendida: sus códigos no pueden entrar." else "Membresía reactivada."
                cargar()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo cambiar el estado."
            }
        }
    }

    fun renovar(detalle: MembresiaDetalle) {
        val membresia = detalle.membresia
        viewModelScope.launch {
            try {
                val precio = ClavesPrecio.deMembresia(membresia.tipo, membresia.plan, membresia.paqueteFamiliarId, preciosActuales())
                repositorio.renovar(membresia.id, precio)
                _mensaje.value = "Membresía renovada un mes (hasta el ${ReglasMembresia.fechasDeRenovacion(membresia, Fechas.hoy()).second})."
                cargar()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo renovar la membresía."
            }
        }
    }

    fun cerrarRegistroReciente() {
        _registroReciente.value = null
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
