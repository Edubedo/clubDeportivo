package com.example.clubdeportivo.data.notificaciones

import com.example.clubdeportivo.util.Textos
/** A quién va dirigido un aviso. */
enum class DestinatarioNotificacion(val etiqueta: String) {
    EMPLEADOS(Textos.get("staff")),
    SOCIOS(Textos.get("members_2")),
    TODOS(Textos.get("all_2"))
}

/** Aviso público del club; los campos internos permiten presentarlo junto a notificaciones privadas en la bandeja. */
data class Notificacion(
    val id: String = "",
    val titulo: String,
    val mensaje: String,
    val destinatario: DestinatarioNotificacion,
    val autorId: String = "",
    val autorNombre: String = "",
    val fechaMillis: Long = System.currentTimeMillis(),
    val imagenUrl: String = "",
    val vigencia: String = "",
    val usuarioId: String = "",
    val tipo: String = "",
    val leida: Boolean = false,
    val esInterna: Boolean = false
)
