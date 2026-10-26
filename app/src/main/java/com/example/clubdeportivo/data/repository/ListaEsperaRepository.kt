package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EntradaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import com.example.clubdeportivo.util.ReglasListaEspera
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Lista de espera de horas llenas (colección `listaEspera`). Las reglas de la fila están en [ReglasListaEspera]. */
interface ListaEsperaRepository {
    /** Todas las entradas de un área en un día (de cualquier persona): con ellas se arma la fila de cada hora. */
    suspend fun obtenerDeArea(areaId: String, fecha: String): List<EntradaEspera>
    suspend fun obtenerDeUsuario(usuarioId: String): List<EntradaEspera>
    /** Entra a la fila (o vuelve a entrar, hasta atrás, si había salido). */
    suspend fun unirse(entrada: EntradaEspera)
    suspend fun salir(id: String)
    /** Aparta el lugar a estas entradas a partir de [ahoraMillis]. */
    suspend fun marcarAvisadas(ids: List<String>, ahoraMillis: Long)
    /** La persona ya reservó: sus entradas de esas horas salen de la fila. */
    suspend fun marcarAtendidas(usuarioId: String, areaId: String, fecha: String, horas: IntRange)
}

private fun DocumentSnapshot.toEntrada(): EntradaEspera? {
    val usuarioId = getString("usuarioId") ?: return null
    return EntradaEspera(
        id = id,
        usuarioId = usuarioId,
        usuarioNombre = getString("usuarioNombre").orEmpty(),
        areaId = getString("areaId").orEmpty(),
        areaNombre = getString("areaNombre").orEmpty(),
        deporte = getString("deporte").orEmpty(),
        fecha = getString("fecha").orEmpty(),
        hora = (getLong("hora") ?: return null).toInt(),
        personas = (getLong("personas") ?: 1L).toInt().coerceAtLeast(1),
        estado = runCatching { EstadoEspera.valueOf(getString("estado").orEmpty()) }.getOrDefault(EstadoEspera.CANCELADA),
        creadoMillis = getLong("creadoMillis") ?: 0L,
        avisadoMillis = getLong("avisadoMillis")
    )
}

class FirebaseListaEsperaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ListaEsperaRepository {

    private val coleccion = db.collection("listaEspera")

    override suspend fun obtenerDeArea(areaId: String, fecha: String): List<EntradaEspera> =
        coleccion.whereEqualTo("areaId", areaId).whereEqualTo("fecha", fecha).get().await()
            .documents.mapNotNull { it.toEntrada() }

    override suspend fun obtenerDeUsuario(usuarioId: String): List<EntradaEspera> =
        coleccion.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.toEntrada() }

    override suspend fun unirse(entrada: EntradaEspera) {
        coleccion.document(entrada.id).set(
            mapOf(
                "usuarioId" to entrada.usuarioId,
                "usuarioNombre" to entrada.usuarioNombre,
                "areaId" to entrada.areaId,
                "areaNombre" to entrada.areaNombre,
                "deporte" to entrada.deporte,
                "fecha" to entrada.fecha,
                "hora" to entrada.hora.toLong(),
                "personas" to entrada.personas.toLong(),
                "estado" to EstadoEspera.ESPERANDO.name,
                "creadoMillis" to entrada.creadoMillis,
                "avisadoMillis" to null,
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun salir(id: String) {
        coleccion.document(id).update(
            mapOf("estado" to EstadoEspera.CANCELADA.name, "actualizadoEn" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun marcarAvisadas(ids: List<String>, ahoraMillis: Long) {
        if (ids.isEmpty()) return
        val lote = db.batch()
        ids.forEach { lote.update(coleccion.document(it), mapOf("estado" to EstadoEspera.AVISADO.name, "avisadoMillis" to ahoraMillis)) }
        lote.commit().await()
    }

    override suspend fun marcarAtendidas(usuarioId: String, areaId: String, fecha: String, horas: IntRange) {
        val propias = obtenerDeUsuario(usuarioId).filter {
            it.areaId == areaId && it.fecha == fecha && it.hora in horas &&
                (it.estado == EstadoEspera.ESPERANDO || it.estado == EstadoEspera.AVISADO)
        }
        if (propias.isEmpty()) return
        val lote = db.batch()
        propias.forEach {
            lote.update(
                coleccion.document(it.id),
                mapOf("estado" to EstadoEspera.ATENDIDA.name, "actualizadoEn" to FieldValue.serverTimestamp())
            )
        }
        lote.commit().await()
    }
}
