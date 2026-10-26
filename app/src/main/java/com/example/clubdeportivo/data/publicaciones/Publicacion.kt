package com.example.clubdeportivo.data.publicaciones

import com.example.clubdeportivo.util.Textos
enum class AudienciaPublicacion(val etiqueta: String) {
    TODOS(Textos.get("all_2")),
    PERSONAL(Textos.get("staff")),
    SOCIOS(Textos.get("members_2"))
}

data class Publicacion(
    val id: String = "",
    val titulo: String,
    val descripcion: String,
    val imagenUrl: String,
    val audiencia: AudienciaPublicacion,
    val vigencia: String,
    val autorId: String = "",
    val fechaMillis: Long = System.currentTimeMillis()
)
