package com.example.clubdeportivo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.clubdeportivo.util.tr

/**
 * Un proceso a medias (por ejemplo, una reserva con deporte y horas ya elegidos) que no se debe perder sin querer.
 *
 * La pantalla del proceso lo declara con [ProtegerSalida]; cualquier salida (menú inferior, perfil, botón atrás, cambio de
 * pestaña) pasa por [intentar], que pregunta "¿seguro que quieres salir?" antes de seguir. Si la persona confirma, se
 * ejecuta lo que la pantalla pidió para descartar el proceso y después la salida.
 */
object ProcesoEnCurso {
    var activo by mutableStateOf(false)
        private set

    /** Clave del texto (en assets/i18n) que explica qué se pierde al salir. */
    var mensaje by mutableStateOf("salir_proceso_mensaje")
        private set

    /** Salida que está esperando la respuesta de la persona; mientras no sea null se muestra [DialogoSalidaProceso]. */
    var salidaPendiente by mutableStateOf<(() -> Unit)?>(null)
        private set

    private var alAbandonar: (() -> Unit)? = null

    internal fun registrar(mensaje: String, alAbandonar: () -> Unit) {
        this.mensaje = mensaje
        this.alAbandonar = alAbandonar
        activo = true
    }

    internal fun liberar() {
        activo = false
        alAbandonar = null
        salidaPendiente = null
    }

    /** Ejecuta [salida] ya, o pregunta primero si hay un proceso en curso. */
    fun intentar(salida: () -> Unit) {
        if (activo) salidaPendiente = salida else salida()
    }

    internal fun confirmarSalida() {
        val salida = salidaPendiente
        val descartar = alAbandonar
        liberar()
        descartar?.invoke()
        salida?.invoke()
    }

    internal fun cancelarSalida() {
        salidaPendiente = null
    }
}

/**
 * Marca esta pantalla como "proceso en curso" mientras [activo] sea true. [alAbandonar] se ejecuta si la persona
 * confirma que quiere salirse (para limpiar lo que había elegido).
 */
@Composable
fun ProtegerSalida(
    activo: Boolean,
    mensaje: String = "salir_proceso_mensaje",
    alAbandonar: () -> Unit = {}
) {
    val descartar by rememberUpdatedState(alAbandonar)
    DisposableEffect(activo, mensaje) {
        if (activo) ProcesoEnCurso.registrar(mensaje) { descartar() }
        onDispose { if (activo) ProcesoEnCurso.liberar() }
    }
}

/** Pregunta de confirmación de [ProcesoEnCurso]; se dibuja una sola vez, al final de la pantalla principal. */
@Composable
fun DialogoSalidaProceso() {
    if (ProcesoEnCurso.salidaPendiente == null) return
    DialogoConfirmacion(
        titulo = tr("salir_proceso_titulo"),
        mensaje = tr(ProcesoEnCurso.mensaje),
        textoConfirmar = tr("salir_si"),
        textoCancelar = tr("seguir_aqui"),
        onConfirmar = ProcesoEnCurso::confirmarSalida,
        onCancelar = ProcesoEnCurso::cancelarSalida
    )
}
