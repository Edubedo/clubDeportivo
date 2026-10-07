package com.example.clubdeportivo.ui.perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.repository.UsuarioRepository
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val usuarioRepository: UsuarioRepository =
        AppContainer.usuarioRepository
) : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

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

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    fun cerrarSesion() {
        AppContainer.authRepository.cerrarSesion()
        SesionManager.cerrarSesion()
    }
}