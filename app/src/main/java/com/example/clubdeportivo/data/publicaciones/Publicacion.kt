package com.example.clubdeportivo.data.publicaciones

import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos
enum class AudienciaPublicacion(val etiqueta: String) {
    TODOS(Textos.get(R.string.all_2)),
    PERSONAL(Textos.get(R.string.staff)),
    SOCIOS(Textos.get(R.string.members_2))
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
