package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.FirebaseSeeder
import com.example.clubdeportivo.data.model.InscripcionTorneo
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.firestore.AggregateSource
import com.example.clubdeportivo.data.SesionManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseTorneoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TorneoRepository {

    private val torneos = db.collection("torneos")
    private val inscripciones = db.collection("inscripcionesTorneo")

    /**
     * `inscritos` es un contador guardado en el propio torneo (se actualiza en la misma transacción que crea la
     * inscripción), así la lista no hace una consulta de conteo por cada torneo. En torneos anteriores al contador
     * el campo no existe y se cuenta en el momento.
     */
    private suspend fun DocumentSnapshot.toTorneo(): Torneo? {
        val nombre = getString("nombre") ?: return null
        val inscritos = getLong("inscritos")
            ?: inscripciones.whereEqualTo("torneoId", id).count().get(AggregateSource.SERVER).await().count
        return Torneo(
            id = id,
            nombre = nombre,
            disciplina = getString("disciplina") ?: "",
            areaId = getString("areaId") ?: "",
            fechaInicio = getString("fechaInicio") ?: "",
            fechaFin = getString("fechaFin") ?: "",
            cupoMaximo = (getLong("cupoMaximo") ?: 0).toInt(),
            inscritos = inscritos.toInt(),
            // getString() devuelve null en torneos guardados antes de que existiera este campo.
            horaInicio = getString("horaInicio") ?: "",
            horaFin = getString("horaFin") ?: ""
        )
    }

    private suspend fun nombreDelArea(areaId: String): String =
        db.collection("areas").document(areaId).get().await().getString("nombre") ?: ""

    override suspend fun obtenerTorneos(): List<Torneo> {
        FirebaseSeeder.asegurarTorneos(db)
        return torneos.get().await().documents.mapNotNull { it.toTorneo() }
    }

    /**
     * El id de la inscripción es `torneoId_usuarioId`: una persona no puede inscribirse dos veces al mismo torneo.
     * Todo ocurre en una transacción para que dos inscripciones simultáneas no sobrepasen el cupo.
     */
    override suspend fun inscribirse(torneoId: String, usuarioId: String): InscripcionTorneo {
        val torneoRef = torneos.document(torneoId)
        val inscripcionRef = inscripciones.document("${torneoId}_$usuarioId")
        val usuario = db.collection("usuarios").document(usuarioId).get().await()
        val fecha = Fechas.hoy()

        db.runTransaction { transaccion ->
            val torneo = transaccion.get(torneoRef)
            if (!torneo.exists()) throw IllegalStateException("El torneo ya no existe.")
            if (transaccion.get(inscripcionRef).exists()) throw IllegalStateException("Ya estás inscrito en este torneo.")

            val cupo = torneo.getLong("cupoMaximo") ?: 0L
            val actuales = torneo.getLong("inscritos") ?: 0L
            if (actuales >= cupo) throw IllegalStateException("El torneo ya no tiene cupo.")

            transaccion.set(
                inscripcionRef,
                mapOf(
                    "torneoId" to torneoId,
                    "torneoNombre" to (torneo.getString("nombre") ?: ""),
                    "usuarioId" to usuarioId,
                    "usuarioNombre" to (usuario.getString("nombre") ?: ""),
                    "fechaInscripcion" to fecha,
                    "estado" to "CONFIRMADA",
                    "creadoEn" to FieldValue.serverTimestamp()
                )
            )
            transaccion.update(torneoRef, mapOf("inscritos" to actuales + 1, "actualizadoEn" to FieldValue.serverTimestamp()))
            null
        }.await()

        return InscripcionTorneo(inscripcionRef.id, torneoId, usuarioId, fecha)
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
        val datos = mapOf(
            "nombre" to nombre,
            "disciplina" to disciplina,
            "areaId" to areaId,
            "areaNombre" to nombreDelArea(areaId),
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin,
            "cupoMaximo" to cupoMaximo.toLong(),
            "inscritos" to 0L,
            "creadoPor" to (SesionManager.usuarioActual?.id ?: ""),
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        val documento = torneos.add(datos).await()
        return Torneo(documento.id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, inscritos = 0, horaInicio = horaInicio, horaFin = horaFin)
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
        val datos = mapOf(
            "nombre" to nombre,
            "disciplina" to disciplina,
            "areaId" to areaId,
            "areaNombre" to nombreDelArea(areaId),
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin,
            "cupoMaximo" to cupoMaximo.toLong(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        // merge: no se pisan el contador de inscritos ni los datos de creación.
        torneos.document(id).set(datos, SetOptions.merge()).await()
        val inscritos = torneos.document(id).get().await().getLong("inscritos")
            ?: inscripciones.whereEqualTo("torneoId", id).count().get(AggregateSource.SERVER).await().count
        return Torneo(id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, inscritos.toInt(), horaInicio, horaFin)
    }
}
