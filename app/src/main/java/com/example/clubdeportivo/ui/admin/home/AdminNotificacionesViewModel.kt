package com.example.clubdeportivo.ui.admin.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.data.notificaciones.NotificacionesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EnviarNotificacionUiState(
    val titulo: String = "",
    val mensaje: String = "",
    val destinatario: DestinatarioNotificacion = DestinatarioNotificacion.SOCIOS,
    val errorTitulo: String? = null,
    val errorMensaje: String? = null,
    val errorGeneral: String? = null,
    val enviando: Boolean = false,
    val enviada: Boolean = false
)

class AdminNotificacionesViewModel : ViewModel() {

    private val _ui = MutableStateFlow(EnviarNotificacionUiState())
    val ui: StateFlow<EnviarNotificacionUiState> = _ui.asStateFlow()

    fun onTitulo(valor: String) =
        _ui.update { it.copy(titulo = valor.take(60), errorTitulo = null) }

    fun onMensaje(valor: String) =
        _ui.update { it.copy(mensaje = valor.take(300), errorMensaje = null) }

    fun onDestinatario(valor: DestinatarioNotificacion) =
        _ui.update { it.copy(destinatario = valor) }

    fun enviar(autorId: String?) {
        val estado = _ui.value
        if (estado.enviando) return

        val errorTitulo = if (estado.titulo.isBlank()) "Escribe un título" else null
        val errorMensaje = if (estado.mensaje.isBlank()) "Escribe un mensaje" else null

        if (errorTitulo != null || errorMensaje != null) {
            _ui.update { it.copy(errorTitulo = errorTitulo, errorMensaje = errorMensaje) }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(enviando = true, errorGeneral = null) }

            val resultado = NotificacionesRepository.publicar(
                Notificacion(
                    titulo = estado.titulo.trim(),
                    mensaje = estado.mensaje.trim(),
                    destinatario = estado.destinatario,
                    autorId = autorId
                )
            )

            _ui.update {
                if (resultado.isSuccess) {
                    EnviarNotificacionUiState(enviada = true) // limpia el formulario
                } else {
                    it.copy(enviando = false, errorGeneral = "No se pudo enviar. Intenta de nuevo.")
                }
            }
        }
    }

    /** Se llama después de cerrar la hoja para resetear la bandera. */
    fun consumirEnviada() = _ui.update { it.copy(enviada = false) }
}