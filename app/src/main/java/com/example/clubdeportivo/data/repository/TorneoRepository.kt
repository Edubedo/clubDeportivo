package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.InscripcionTorneo
import com.example.clubdeportivo.data.model.Torneo
import kotlinx.coroutines.delay

interface TorneoRepository {
    suspend fun obtenerTorneos(): List<Torneo>
    suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo
    /** Ids de los torneos en los que [usuarioId] ya está inscrito. */
    suspend fun torneosInscritos(usuarioId: String): Set<String>
    suspend fun crearTorneo(
        nombre: String,
        disciplina: String,
        areaId: String,
        fechaInicio: String,
        fechaFin: String,
        cupoMaximo: Int,
        horaInicio: String,
        horaFin: String
    ): Torneo
    suspend fun actualizarTorneo(
        id: String,
        nombre: String,
        disciplina: String,
        areaId: String,
        fechaInicio: String,
        fechaFin: String,
        cupoMaximo: Int,
        horaInicio: String,
        horaFin: String
    ): Torneo
    /** Borra el torneo y las inscripciones que tenía. */
    suspend fun eliminarTorneo(id: String)
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

    private val inscritosPorUsuario = mutableMapOf<String, MutableSet<String>>()

    override suspend fun torneosInscritos(usuarioId: String): Set<String> = inscritosPorUsuario[usuarioId].orEmpty().toSet()

    override suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo {
        delay(400)
        inscritosPorUsuario.getOrPut(usuarioId) { mutableSetOf() }.add(torneoId)
        val indice = torneos.indexOfFirst { it.id == torneoId }
        if (indice != -1) {
            val torneo = torneos[indice]
            torneos[indice] = torneo.copy(inscritos = torneo.inscritos + 1)
        }
        return InscripcionTorneo((siguienteInscripcionId++).toString(), torneoId, usuarioId, "2026-09-08")
    }

    override suspend fun crearTorneo(
        nombre: String,
        disciplina: String,
        areaId: String,
        fechaInicio: String,
        fechaFin: String,
        cupoMaximo: Int,
        horaInicio: String,
        horaFin: String
    ): Torneo {
        delay(300)
        val torneo = Torneo(
            id = (torneos.size + 1).toString(),
            nombre = nombre,
            disciplina = disciplina,
            areaId = areaId,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            cupoMaximo = cupoMaximo,
            inscritos = 0,
            horaInicio = horaInicio,
            horaFin = horaFin
        )
        torneos.add(torneo)
        return torneo
    }

    override suspend fun actualizarTorneo(
        id: String,
        nombre: String,
        disciplina: String,
        areaId: String,
        fechaInicio: String,
        fechaFin: String,
        cupoMaximo: Int,
        horaInicio: String,
        horaFin: String
    ): Torneo {
        delay(300)
        val indice = torneos.indexOfFirst { it.id == id }
        val actualizado = (
            torneos.getOrNull(indice)
                ?: Torneo(id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, 0)
            ).copy(
                nombre = nombre,
                disciplina = disciplina,
                areaId = areaId,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                cupoMaximo = cupoMaximo,
                horaInicio = horaInicio,
                horaFin = horaFin
            )
        if (indice != -1) torneos[indice] = actualizado
        return actualizado
    }

    override suspend fun eliminarTorneo(id: String) {
        delay(300)
        torneos.removeAll { it.id == id }
    }
}
