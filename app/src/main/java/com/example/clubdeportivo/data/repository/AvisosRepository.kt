package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface AvisosRepository {
    /** El personal ve todos los avisos; socios y visitantes solo los dirigidos a socios o a todos. Los más recientes primero. */
    suspend fun obtener(esPersonal: Boolean): List<Notificacion>
    suspend fun publicar(aviso: Notificacion): Notificacion
    suspend fun actualizar(id: String, titulo: String, mensaje: String)
    suspend fun eliminar(id: String)
}

private fun DocumentSnapshot.toAviso(): Notificacion? {
    val titulo = getString("titulo") ?: return null
    return Notificacion(
        id = id,
        titulo = titulo,
        mensaje = getString("mensaje").orEmpty(),
        destinatario = runCatching { DestinatarioNotificacion.valueOf(getString("destinatario").orEmpty()) }
            .getOrDefault(DestinatarioNotificacion.TODOS),
        autorId = getString("autorId").orEmpty(),
        autorNombre = getString("autorNombre").orEmpty(),
        fechaMillis = getLong("fechaMillis") ?: 0L
    )
}

class FirebaseAvisosRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AvisosRepository {

    private val coleccion = db.collection("avisos")

    override suspend fun obtener(esPersonal: Boolean): List<Notificacion> {
        val consulta = if (esPersonal) {
            coleccion.orderBy("fechaMillis", Query.Direction.DESCENDING).limit(MAXIMO)
        } else {
            coleccion.whereIn("destinatario", listOf(DestinatarioNotificacion.SOCIOS.name, DestinatarioNotificacion.TODOS.name))
                .limit(MAXIMO)
        }
        return consulta.get().await().documents.mapNotNull { it.toAviso() }.sortedByDescending { it.fechaMillis }
    }

    override suspend fun publicar(aviso: Notificacion): Notificacion {
        val datos = mapOf(
            "titulo" to aviso.titulo,
            "mensaje" to aviso.mensaje,
            "destinatario" to aviso.destinatario.name,
            "autorId" to aviso.autorId,
            "autorNombre" to aviso.autorNombre,
            "fechaMillis" to aviso.fechaMillis,
            "creadoEn" to FieldValue.serverTimestamp()
        )
        val documento = coleccion.add(datos).await()
        return aviso.copy(id = documento.id)
    }

    override suspend fun actualizar(id: String, titulo: String, mensaje: String) {
        coleccion.document(id).update(
            mapOf("titulo" to titulo, "mensaje" to mensaje, "editadoEn" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun eliminar(id: String) {
        coleccion.document(id).delete().await()
    }

    private companion object {
        const val MAXIMO = 100L
    }
}
