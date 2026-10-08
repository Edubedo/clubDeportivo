package com.example.clubdeportivo.ui.personal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.MigracionEsquema
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.DatosPersonal
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.PersonalRepository
import com.example.clubdeportivo.util.ReglasContrasena
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/** Texto con el que empieza el aviso de éxito al cambiar una contraseña (la pantalla lo pinta en verde). */
const val MENSAJE_CONTRASENA_CAMBIADA = "Contraseña actualizada para "

/** Personal del club: lista, altas (con cuenta de acceso real) y ediciones. Todo sobre la colección `usuarios`. */
class PersonalViewModel(
    private val personalRepository: PersonalRepository = AppContainer.personalRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository
) : ViewModel() {

    private val _personal = MutableLiveData<List<Personal>>(emptyList())
    val personal: LiveData<List<Personal>> = _personal

    /** Áreas del club (deportes: Fútbol, Tenis...), no las canchas individuales. */
    private val _areas = MutableLiveData<List<String>>(emptyList())
    val areas: LiveData<List<String>> = _areas

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> = _cargando

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    /** Aviso para mostrar dentro del formulario abierto (un snackbar quedaría tapado por el diálogo). */
    private val _avisoFormulario = MutableLiveData<String?>()
    val avisoFormulario: LiveData<String?> = _avisoFormulario

    /** Pasa a true cuando un guardado terminó bien, para que la pantalla cierre el formulario. */
    private val _guardadoExitoso = MutableLiveData(false)
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                // Por si es la primera vez tras la fusión de empleados en usuarios (no hace nada si ya está hecha).
                MigracionEsquema.asegurar(FirebaseFirestore.getInstance())
            } catch (e: Exception) {
                Log.w("Esquema", "No se pudo migrar la estructura de la base de datos", e)
            }
            try {
                _areas.value = areaRepository.obtenerAreas().map { it.tipo.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                _personal.value = personalRepository.obtenerPersonal()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo cargar el personal. Revisa tu conexión."
            }
            _cargando.value = false
        }
    }

    fun guardar(existente: Personal?, datos: DatosPersonal) {
        val yo = SesionManager.usuarioActual
        _avisoFormulario.value = null
        if (yo?.rol?.esAdministrador() != true) {
            _avisoFormulario.value = "Solo los administradores pueden gestionar al personal."
            return
        }
        // Evita que un administrador se quite a sí mismo el acceso y deje la sección sin nadie que la controle.
        if (existente?.id == yo.id && (datos.estado != "ACTIVO" || !datos.rol.esAdministrador())) {
            _avisoFormulario.value = "No puedes desactivarte ni quitarte el rol de administrador a ti mismo."
            return
        }
        // Un encargado trabaja solo con lo de su área: sin área no vería reservas, áreas ni inventario.
        if (!datos.rol.esAdministrador() && datos.areaTrabajo.isNullOrBlank()) {
            _avisoFormulario.value = "Elige el área que va a encargar."
            return
        }
        viewModelScope.launch {
            _guardando.value = true
            val resultado = if (existente == null) personalRepository.crear(datos) else personalRepository.actualizar(existente.id, datos)
            when (resultado) {
                is Resultado.Exito -> {
                    _mensaje.value = if (existente == null) "Personal agregado: ${datos.nombre.trim()}" else "Cambios guardados"
                    _guardadoExitoso.value = true
                    cargarDatos()
                }
                is Resultado.Error -> _avisoFormulario.value = resultado.mensaje
            }
            _guardando.value = false
        }
    }

    /** Cambia la contraseña de [persona] sin correo (la valida el servidor). El resultado se avisa dentro del formulario abierto. */
    fun cambiarContrasena(persona: Personal, contrasena: String) {
        _avisoFormulario.value = null
        if (SesionManager.usuarioActual?.rol?.esAdministrador() != true) {
            _avisoFormulario.value = "Solo los administradores pueden cambiar contraseñas del personal."
            return
        }
        ReglasContrasena.primerError(contrasena)?.let {
            _avisoFormulario.value = it
            return
        }
        viewModelScope.launch {
            _guardando.value = true
            when (val resultado = personalRepository.cambiarContrasena(persona.id, contrasena)) {
                is Resultado.Exito -> _avisoFormulario.value = "${MENSAJE_CONTRASENA_CAMBIADA}${persona.nombre.trim()}."
                is Resultado.Error -> _avisoFormulario.value = resultado.mensaje
            }
            _guardando.value = false
        }
    }

    fun limpiarAvisoFormulario() {
        _avisoFormulario.value = null
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    fun onGuardadoProcesado() {
        _guardadoExitoso.value = false
    }
}
