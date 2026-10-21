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
import com.example.clubdeportivo.data.repository.FotoPerfilRepository
import com.example.clubdeportivo.data.repository.UsuarioRepository
import com.example.clubdeportivo.util.comprimirFotoPerfil
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PerfilViewModel(
    private val usuarioRepository: UsuarioRepository =
        AppContainer.usuarioRepository,
    private val fotoPerfilRepository: FotoPerfilRepository =
        AppContainer.fotoPerfilRepository
) : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

    private val _subiendoFoto = MutableLiveData(false)
    val subiendoFoto: LiveData<Boolean> = _subiendoFoto

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    init {
        _usuario.value = SesionManager.usuarioActual
    }

    fun actualizarPerfil(nombre: String) {

        val usuarioActual = _usuario.value ?: return

        if (nombre.isBlank()) {
            _mensaje.value = "El nombre no puede estar vacío."
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

                    _mensaje.value =
                        "Perfil actualizado correctamente."
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
                _mensaje.value = "No se pudo leer la imagen elegida."
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

    fun mostrarMensaje(texto: String) {
        _mensaje.value = texto
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    fun cerrarSesion() {
        AppContainer.authRepository.cerrarSesion()
        SesionManager.cerrarSesion()
    }
}