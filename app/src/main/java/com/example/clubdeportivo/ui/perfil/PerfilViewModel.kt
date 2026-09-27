package com.example.clubdeportivo.ui.perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Usuario

class PerfilViewModel : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    init {
        _usuario.value = SesionManager.usuarioActual
    }

    fun actualizarPerfil(
        nombre: String,
        correo: String
    ) {
        val usuarioActual = _usuario.value ?: return

        if (nombre.isBlank() || correo.isBlank()) {
            return
        }

        val usuarioActualizado = usuarioActual.copy(
            nombre = nombre.trim(),
            correo = correo.trim()
        )

        _usuario.value = usuarioActualizado

        SesionManager.iniciarSesion(usuarioActualizado)
    }

    fun cerrarSesion() {
        SesionManager.cerrarSesion()
    }
}