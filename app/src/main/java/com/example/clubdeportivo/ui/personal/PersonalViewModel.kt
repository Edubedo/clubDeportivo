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
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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

    fun enviarRestablecimiento(email: String) {
        viewModelScope.launch {
            _avisoFormulario.value = null
            when (val resultado = personalRepository.enviarRestablecimientoDeContrasena(email)) {
                is Resultado.Exito -> _avisoFormulario.value = "Enviamos un correo a $email para cambiar la contraseña."
                is Resultado.Error -> _avisoFormulario.value = resultado.mensaje
            }
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
