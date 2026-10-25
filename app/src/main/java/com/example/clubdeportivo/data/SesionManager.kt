package com.example.clubdeportivo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    // Es estado de Compose: cuando la sesión cambia (por ejemplo, si un administrador suspende la cuenta)
    // las pantallas se enteran solas y la app puede sacar a la persona.
    var usuarioActual: Usuario? by mutableStateOf(null)
        private set

    var versionFotoPerfil by mutableIntStateOf(0)
        private set

    /** Por qué se cerró la sesión sin que la persona lo pidiera (cuenta suspendida o eliminada); null si no pasó. */
    var motivoExpulsion: String? by mutableStateOf(null)
        private set

    fun iniciarSesion(usuario: Usuario) {
        usuarioActual = usuario
        motivoExpulsion = null
    }

    fun cerrarSesion() {
        usuarioActual = null
    }

    /** Cierra la sesión porque la cuenta dejó de ser válida; [motivo] se le muestra a la persona en un aviso. */
    fun expulsar(motivo: String) {
        usuarioActual = null
        motivoExpulsion = motivo
    }

    fun onExpulsionMostrada() {
        motivoExpulsion = null
    }
    fun notificarCambioFoto() {
        versionFotoPerfil++
    }


    fun haySesionActiva(): Boolean = usuarioActual != null
}
