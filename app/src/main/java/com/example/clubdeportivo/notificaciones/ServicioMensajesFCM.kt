package com.example.clubdeportivo.notificaciones

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ServicioMensajesFCM : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { usuarioId ->
            GestorTokenFCM.registrarEnSegundoPlano(usuarioId, token)
        }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val destinatarioId = mensaje.data["usuarioId"] ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != destinatarioId) return

        val titulo = mensaje.data["titulo"] ?: mensaje.notification?.title ?: return
        val texto = mensaje.data["mensaje"] ?: mensaje.notification?.body ?: return
        val categoria = runCatching {
            CategoriaNotificacion.valueOf(mensaje.data["categoria"].orEmpty())
        }.getOrDefault(CategoriaNotificacion.AVISOS)
        if (!PreferenciasNotificaciones.activa(this, categoria)) return
        Notificador.mostrar(
            this,
            Recordatorio(
                clave = mensaje.data["notificacionId"] ?: mensaje.messageId.orEmpty(),
                categoria = categoria,
                titulo = titulo,
                mensaje = texto,
                ruta = mensaje.data["ruta"]?.takeIf(String::isNotBlank)
            )
        )
    }
}
