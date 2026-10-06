package com.example.clubdeportivo.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.data.notificaciones.NotificacionesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonalNotificacionesViewModel : ViewModel() {

    val notificaciones = NotificacionesRepository.paraRol(DestinatarioNotificacion.EMPLEADOS)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _leidas = MutableStateFlow<Set<String>>(emptySet())
    val leidas: StateFlow<Set<String>> = _leidas.asStateFlow()

    fun marcarComoLeida(id: String) {
        _leidas.value = _leidas.value + id
    }

    fun enviarAvisoASocios(titulo: String, mensaje: String, onExito: () -> Unit) {
        if (titulo.isBlank() || mensaje.isBlank()) return

        viewModelScope.launch {
            val nuevaNotificacion = Notificacion(
                titulo = titulo,
                mensaje = mensaje,
                destinatario = DestinatarioNotificacion.SOCIOS
            )
            val resultado = NotificacionesRepository.publicar(nuevaNotificacion)

            if (resultado.isSuccess) {
                onExito()
            }
        }
    }
}