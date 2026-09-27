package com.example.clubdeportivo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.clubdeportivo.data.model.Usuario

/**
 * Guarda en memoria al usuario que inició sesión, para que cualquier
 * pantalla pueda consultar "quién soy" sin volver a pedirlo a la API.
 *
 * Nota: al cerrar la app se pierde esta información. Para producción,
 * lo normal es guardar el token de sesión en almacenamiento seguro
 * (por ejemplo EncryptedSharedPreferences o DataStore).
 */
object SesionManager {
    var usuarioActual: Usuario? = null
        private set

    var versionFotoPerfil by mutableIntStateOf(0)
        private set

    fun iniciarSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

    fun cerrarSesion() {
        usuarioActual = null
    }
    fun notificarCambioFoto() {
        versionFotoPerfil++
    }


    fun haySesionActiva(): Boolean = usuarioActual != null
}
