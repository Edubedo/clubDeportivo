package com.example.clubdeportivo.data.model

/** Material deportivo del club (balones, raquetas...). Vive en la colección `herramientas` de Firestore. */
data class ArticuloInventario(
    val id: String,
    val nombre: String,
    val deporte: String,
    val cantidad: Int,
    val icono: String,
    val stockMinimo: Int
)
