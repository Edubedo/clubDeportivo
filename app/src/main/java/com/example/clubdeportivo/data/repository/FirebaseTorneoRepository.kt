package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.FirebaseSeeder
import com.example.clubdeportivo.data.model.InscripcionTorneo
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTorneoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TorneoRepository {

    private val torneos = db.collection("torneos")
    private val inscripciones = db.collection("inscripcionesTorneo")

    /**
     * A diferencia de la versión "Fake", acá NO se guarda un número de "inscritos" en el
     * documento del torneo (se podría desactualizar). Se cuenta en el momento cuántas filas
     * de "inscripcionesTorneo" apuntan a este torneo.
     */
    private suspend fun DocumentSnapshot.toTorneo(): Torneo? {
        val nombre = getString("nombre") ?: return null
        val inscritos = inscripciones.whereEqualTo("torneoId", id)
            .count().get(AggregateSource.SERVER).await().count
        return Torneo(
            id = id,
            nombre = nombre,
            disciplina = getString("disciplina") ?: "",
            areaId = getString("areaId") ?: "",
            fechaInicio = getString("fechaInicio") ?: "",
            fechaFin = getString("fechaFin") ?: "",
            cupoMaximo = (getLong("cupoMaximo") ?: 0).toInt(),
            inscritos = inscritos.toInt()
        )
    }

    override suspend fun obtenerTorneos(): List<Torneo> {
        FirebaseSeeder.asegurarTorneos(db)
        return torneos.get().await().documents.mapNotNull { it.toTorneo() }
    }

    override suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo {
        val datos = mapOf(
            "torneoId" to torneoId,
            "usuarioId" to usuarioId,
            "fechaInscripcion" to Fechas.hoy()
        )
        val documento = inscripciones.add(datos).await()
        return InscripcionTorneo(documento.id, torneoId, usuarioId, datos["fechaInscripcion"] as String)
    }

    override suspend fun hayTorneoQueBloqueaArea(areaId: String, fecha: String): Boolean {
        val torneosDelArea = torneos.whereEqualTo("areaId", areaId).get().await()
        return torneosDelArea.documents.any { doc ->
            val inicio = doc.getString("fechaInicio") ?: return@any false
            val fin = doc.getString("fechaFin") ?: return@any false
            fecha in inicio..fin
        }
    }
}
