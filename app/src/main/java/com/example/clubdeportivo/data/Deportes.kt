package com.example.clubdeportivo.data

/** Deportes conocidos del club y su emoji; los deportes nuevos que se den de alta guardan el suyo en el área. */
object Deportes {
    /** Deportes con áreas en el club por ahora. Inventario, torneos y reservas toman esta misma lista. */
    val predefinidos = listOf("Básquetbol", "Tenis", "Fútbol", "Natación")

    private val emojis = mapOf(
        "Básquetbol" to "🏀",
        "Baloncesto" to "🏀",
        "Tenis" to "🎾",
        "Fútbol" to "⚽",
        "Natación" to "🏊"
    )

    fun emojiDe(tipo: String, emojiPropio: String = ""): String =
        emojiPropio.ifBlank { emojis[tipo] ?: "🏆" }

    /** "Tenis — Cancha C"; si el nombre ya menciona el deporte ("Cancha de fútbol 1") se deja tal cual. */
    fun titulo(tipo: String, nombre: String): String =
        if (tipo.isBlank() || nombre.contains(tipo, ignoreCase = true)) nombre else "$tipo — $nombre"
}
