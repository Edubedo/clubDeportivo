package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Familia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.repository.FamiliaRepository
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import kotlinx.coroutines.launch
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

/** El paquete familiar de quien tiene sesión; si es el titular, también el alta de sus familiares. */
class FamiliaViewModel(
    private val familiaRepository: FamiliaRepository = AppContainer.familiaRepository
) : ViewModel() {

    private val _familia = MutableLiveData<Familia?>(null)
    val familia: LiveData<Familia?> = _familia

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

    /** Motivo por el que no se pudo dar de alta al familiar; se muestra dentro del formulario. */
    private val _errorFormulario = MutableLiveData<String?>()
    val errorFormulario: LiveData<String?> = _errorFormulario

    /** Familiar recién dado de alta: se muestra su código y el enlace de acceso para que se lo mande. */
    private val _invitacion = MutableLiveData<MiembroClub?>()
    val invitacion: LiveData<MiembroClub?> = _invitacion

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    init {
        cargar()
    }

    fun cargar() {
        val usuario = SesionManager.usuarioActual ?: return
        viewModelScope.launch {
            try {
                _familia.value = familiaRepository.obtenerFamilia(usuario.id)
            } catch (e: Exception) {
                _mensaje.value = Textos.get(R.string.your_family_package_could_not_be)
            }
        }
    }

    /** true si el titular puede sumar a alguien ahora: paquete activo y con lugar. */
    fun puedeAgregar(familia: Familia): Boolean =
        familia.esTitular && familia.lugaresLibres > 0 &&
            ReglasMembresia.estadoEfectivo(familia.membresia, Fechas.hoy()) == EstadoMembresia.ACTIVA

    fun agregar(nombre: String, parentesco: String, telefono: String, correo: String) {
        val actual = _familia.value ?: return
        if (_guardando.value == true) return
        val persona = PersonaForm(null, nombre, telefono, correo, parentesco)
        ReglasMembresia.validarFamiliar(persona, actual.miembros.size, actual.membresia.tipo, actual.membresia.paqueteFamiliarId)
            ?.let { _errorFormulario.value = it; return }
        _errorFormulario.value = null

        viewModelScope.launch {
            _guardando.value = true
            try {
                _invitacion.value = familiaRepository.agregarFamiliar(actual, persona)
                _familia.value = SesionManager.usuarioActual?.let { familiaRepository.obtenerFamilia(it.id) } ?: actual
            } catch (e: Exception) {
                _errorFormulario.value = e.message?.takeIf { it.length < 140 && !it.contains("PERMISSION") }
                    ?: Textos.get(R.string.the_family_member_could_not_be)
            }
            _guardando.value = false
        }
    }

    fun limpiarError() {
        _errorFormulario.value = null
    }

    fun mostrarInvitacion(miembro: MiembroClub) {
        _invitacion.value = miembro
    }

    fun cerrarInvitacion() {
        _invitacion.value = null
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
