package com.example.clubdeportivo.data.notificaciones

import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos
/** A quién va dirigido un aviso. */
enum class DestinatarioNotificacion(val etiqueta: String) {
    EMPLEADOS(Textos.get(R.string.staff)),
    SOCIOS(Textos.get(R.string.members_2)),
    TODOS(Textos.get(R.string.all_2))
}

/** Un aviso del club (mantenimientos, cambios de horario, torneos...). Vive en la colección `avisos`. */
data class Notificacion(
    val id: String = "",
    val titulo: String,
    val mensaje: String,
    val destinatario: DestinatarioNotificacion,
    val autorId: String = "",
    val autorNombre: String = "",
    val fechaMillis: Long = System.currentTimeMillis(),
    val imagenUrl: String = "",
    val vigencia: String = ""
)
