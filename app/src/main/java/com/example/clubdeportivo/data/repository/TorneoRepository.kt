package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.InscripcionTorneo
import com.example.clubdeportivo.data.model.Torneo
import kotlinx.coroutines.delay

interface TorneoRepository {
    suspend fun obtenerTorneos(): List<Torneo>
    suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo
    /** true si algún torneo bloquea esa área en esa fecha (los torneos ocupan el área completa). */
    suspend fun hayTorneoQueBloqueaArea(areaId: String, fecha: String): Boolean
}

class FakeTorneoRepository : TorneoRepository {

    private var siguienteInscripcionId = 1
    private val torneos = mutableListOf(
        Torneo("1", "Copa Otoño de Fútbol", "Fútbol", areaId = "1", "2026-10-05", "2026-10-20", 16, 10),
        Torneo("2", "Torneo Relámpago de Tenis", "Tenis", areaId = "7", "2026-09-25", "2026-09-27", 8, 8),
        Torneo("3", "Liga Interna de Básquetbol", "Básquetbol", areaId = "3", "2026-11-01", "2026-12-15", 12, 5)
    )

    override suspend fun obtenerTorneos(): List<Torneo> {
        delay(400)
        // Se devuelve una copia: torneos es una MutableList que inscribirse()
        // modifica en el sitio, y ListAdapter ignora una lista nueva si es
        // la MISMA instancia (por referencia) que la anterior.
        return torneos.toList()
    }

    override suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo {
        delay(400)
        val indice = torneos.indexOfFirst { it.id == torneoId }
        if (indice != -1) {
            val torneo = torneos[indice]
            torneos[indice] = torneo.copy(inscritos = torneo.inscritos + 1)
        }
        return InscripcionTorneo((siguienteInscripcionId++).toString(), torneoId, usuarioId, "2026-09-08")
    }

    override suspend fun hayTorneoQueBloqueaArea(areaId: String, fecha: String): Boolean {
        return torneos.any { it.areaId == areaId && fecha in it.fechaInicio..it.fechaFin }
    }
}
