package com.example.clubdeportivo.data.publicaciones

enum class AudienciaPublicacion(val etiqueta: String) {
    TODOS("Todos"),
    PERSONAL("Personal"),
    SOCIOS("Socio")
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
