package com.example.clubdeportivo.data.notificaciones

/** A quién va dirigido un aviso. */
enum class DestinatarioNotificacion(val etiqueta: String) {
    EMPLEADOS("Personal"),
    SOCIOS("Socio"),
    TODOS("Todos")
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
