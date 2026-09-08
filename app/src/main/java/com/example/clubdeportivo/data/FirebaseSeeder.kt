package com.example.clubdeportivo.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Datos de catálogo (áreas y torneos de ejemplo) que en la versión de prueba venían
 * escritos directamente en el código (`FakeAreaRepository`, `FakeTorneoRepository`).
 * Firestore, a diferencia de eso, empieza completamente vacío — así que la primera vez
 * que la app lee una de estas colecciones y la encuentra vacía, la llena una única vez
 * con estos mismos datos de ejemplo. Las próximas veces no hace nada (la colección ya
 * no está vacía).
 */
object FirebaseSeeder {

    suspend fun asegurarAreas(db: FirebaseFirestore) {
        val coleccion = db.collection("areas")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val areas = listOf(
            "1" to mapOf("nombre" to "Cancha de fútbol 1", "tipo" to "Fútbol", "capacidad" to 22L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "2" to mapOf("nombre" to "Cancha de fútbol 2", "tipo" to "Fútbol", "capacidad" to 22L, "disponibilidad" to "OCUPADA", "permiteExternos" to true),
            "3" to mapOf("nombre" to "Cancha de básquetbol 1", "tipo" to "Básquetbol", "capacidad" to 10L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "4" to mapOf("nombre" to "Cancha de básquetbol 2", "tipo" to "Básquetbol", "capacidad" to 10L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "5" to mapOf("nombre" to "Alberca 1", "tipo" to "Natación", "capacidad" to 40L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "6" to mapOf("nombre" to "Alberca 2", "tipo" to "Natación", "capacidad" to 40L, "disponibilidad" to "MANTENIMIENTO", "permiteExternos" to false),
            "7" to mapOf("nombre" to "Cancha de tenis 1", "tipo" to "Tenis", "capacidad" to 4L, "disponibilidad" to "OCUPADA", "permiteExternos" to true),
            "8" to mapOf("nombre" to "Cancha de tenis 2", "tipo" to "Tenis", "capacidad" to 4L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "9" to mapOf("nombre" to "Gimnasio", "tipo" to "Gimnasio", "capacidad" to 30L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "10" to mapOf("nombre" to "Salón de usos múltiples", "tipo" to "Clases grupales", "capacidad" to 25L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false)
        )
        for ((id, datos) in areas) {
            coleccion.document(id).set(datos).await()
        }
    }

    suspend fun asegurarTorneos(db: FirebaseFirestore) {
        val coleccion = db.collection("torneos")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val torneos = listOf(
            "1" to mapOf("nombre" to "Copa Otoño de Fútbol", "disciplina" to "Fútbol", "areaId" to "1", "fechaInicio" to "2026-10-05", "fechaFin" to "2026-10-20", "cupoMaximo" to 16L),
            "2" to mapOf("nombre" to "Torneo Relámpago de Tenis", "disciplina" to "Tenis", "areaId" to "7", "fechaInicio" to "2026-09-25", "fechaFin" to "2026-09-27", "cupoMaximo" to 8L),
            "3" to mapOf("nombre" to "Liga Interna de Básquetbol", "disciplina" to "Básquetbol", "areaId" to "3", "fechaInicio" to "2026-11-01", "fechaFin" to "2026-12-15", "cupoMaximo" to 12L)
        )
        for ((id, datos) in torneos) {
            coleccion.document(id).set(datos).await()
        }
    }
}
