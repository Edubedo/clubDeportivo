package com.example.clubdeportivo.ui.perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.ContentResolver
import android.net.Uri
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.data.repository.FotoPerfilRepository
import com.example.clubdeportivo.data.repository.UsuarioRepository
import com.example.clubdeportivo.notificaciones.GestorTokenFCM
import com.example.clubdeportivo.util.comprimirFotoPerfil
import com.example.clubdeportivo.util.ReglasContrasena
import com.example.clubdeportivo.util.Resultado
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val usuarioRepository: UsuarioRepository =
        AppContainer.usuarioRepository,
    private val fotoPerfilRepository: FotoPerfilRepository =
        AppContainer.fotoPerfilRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

    private val _subiendoFoto = MutableLiveData(false)
    val subiendoFoto: LiveData<Boolean> = _subiendoFoto

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _cambiandoContrasena = MutableLiveData(false)
    val cambiandoContrasena: LiveData<Boolean> = _cambiandoContrasena

    /** true cuando la contraseña se cambió: la pantalla cierra el formulario. */
    private val _contrasenaCambiada = MutableLiveData(false)
    val contrasenaCambiada: LiveData<Boolean> = _contrasenaCambiada

    /** Mensaje de error del formulario de contraseña (se muestra dentro del diálogo, no en el snackbar). */
    private val _errorContrasena = MutableLiveData<String?>()
    val errorContrasena: LiveData<String?> = _errorContrasena

    init {
        _usuario.value = SesionManager.usuarioActual
    }

    fun actualizarPerfil(nombre: String) {

        val usuarioActual = _usuario.value ?: return

        if (nombre.isBlank()) {
            _mensaje.value = Textos.get("perfil_nombre_vacio")
            return
        }

        val usuarioActualizado = usuarioActual.copy(
            nombre = nombre.trim()
        )

        viewModelScope.launch {

            _guardando.value = true

            when (
                val resultado =
                    usuarioRepository.actualizarNombre(usuarioActualizado)
            ) {

                is Resultado.Exito -> {

                    _usuario.value = resultado.datos

                    SesionManager.iniciarSesion(
                        resultado.datos
                    )

                    _mensaje.value = Textos.get("perfil_actualizado")
                }

                is Resultado.Error -> {

                    _mensaje.value =
                        resultado.mensaje
                }
            }

            _guardando.value = false
        }
    }

    /** Comprime la imagen elegida, la sube a la nube y deja la nueva foto en la sesión. */
    fun cambiarFoto(resolver: ContentResolver, uri: Uri) {
        val usuarioActual = _usuario.value ?: return
        if (_subiendoFoto.value == true) return

        viewModelScope.launch {
            _subiendoFoto.value = true

            val jpeg = withContext(Dispatchers.IO) { comprimirFotoPerfil(resolver, uri) }
            if (jpeg == null) {
                _mensaje.value = Textos.get("perfil_imagen_error")
            } else {
                when (val resultado = fotoPerfilRepository.subir(usuarioActual.id, jpeg)) {
                    is Resultado.Exito -> guardarFotoEnSesion(resultado.datos)
                    is Resultado.Error -> _mensaje.value = resultado.mensaje
                }
            }

            _subiendoFoto.value = false
        }
    }

    fun quitarFoto() {
        val usuarioActual = _usuario.value ?: return
        if (_subiendoFoto.value == true) return

        viewModelScope.launch {
            _subiendoFoto.value = true

            when (val resultado = fotoPerfilRepository.eliminar(usuarioActual.id)) {
                is Resultado.Exito -> guardarFotoEnSesion(null)
                is Resultado.Error -> _mensaje.value = resultado.mensaje
            }

            _subiendoFoto.value = false
        }
    }

    private fun guardarFotoEnSesion(fotoUrl: String?) {
        val actualizado = (SesionManager.usuarioActual ?: _usuario.value ?: return).copy(fotoUrl = fotoUrl)
        _usuario.value = actualizado
        SesionManager.iniciarSesion(actualizado)
        SesionManager.notificarCambioFoto()
    }

    /** Cambia la contraseña de quien tiene la sesión abierta, después de validar lo escrito. */
    fun cambiarContrasena(actual: String, nueva: String, confirmacion: String) {
        if (_cambiandoContrasena.value == true) return
        val error = ReglasContrasena.errorAlCambiar(actual, nueva, confirmacion)
        if (error != null) {
            _errorContrasena.value = error
            return
        }
        _errorContrasena.value = null
        viewModelScope.launch {
            _cambiandoContrasena.value = true
            when (val resultado = authRepository.cambiarContrasena(actual, nueva)) {
                is Resultado.Exito -> {
                    _contrasenaCambiada.value = true
                    _mensaje.value = Textos.get("password_changed")
                }
                is Resultado.Error -> _errorContrasena.value = resultado.mensaje
            }
            _cambiandoContrasena.value = false
        }
    }

    fun onContrasenaCambiadaMostrada() {
        _contrasenaCambiada.value = false
        _errorContrasena.value = null
    }

    fun limpiarErrorContrasena() {
        _errorContrasena.value = null
    }

    fun mostrarMensaje(texto: String) {
        _mensaje.value = texto
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    fun cerrarSesion() {
        val uid = SesionManager.usuarioActual?.id
        viewModelScope.launch {
            if (uid != null) {
                runCatching { withTimeout(3_000L) { GestorTokenFCM.eliminar(uid) } }
                    .onFailure { android.util.Log.e("PerfilViewModel", "No se pudo desvincular FCM de $uid", it) }
            }
            authRepository.cerrarSesion()
            SesionManager.cerrarSesion()
        }
    }
}