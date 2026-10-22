package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.publicaciones.AudienciaPublicacion
import com.example.clubdeportivo.data.publicaciones.Publicacion
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface PublicacionesRepository {
    suspend fun obtener(audiencias: List<AudienciaPublicacion>): List<Publicacion>
    suspend fun crear(publicacion: Publicacion): Publicacion
    suspend fun actualizar(publicacion: Publicacion)
    suspend fun eliminar(id: String)
}

private fun DocumentSnapshot.toPublicacion(): Publicacion? {
    val titulo = getString("titulo") ?: return null
    return Publicacion(
        id = id,
        titulo = titulo,
        descripcion = getString("descripcion").orEmpty(),
        imagenUrl = getString("imagenUrl").orEmpty(),
        audiencia = runCatching {
            AudienciaPublicacion.valueOf(getString("audiencia").orEmpty())
        }.getOrDefault(AudienciaPublicacion.TODOS),
        vigencia = getString("vigencia").orEmpty(),
        autorId = getString("autorId").orEmpty(),
        fechaMillis = getLong("fechaMillis") ?: 0L
    )
}

class FirebasePublicacionesRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PublicacionesRepository {
    private val coleccion = db.collection("publicaciones")

    override suspend fun obtener(audiencias: List<AudienciaPublicacion>): List<Publicacion> =
        coleccion.whereIn("audiencia", audiencias.map { it.name })
            .get()
            .await()
            .documents
            .mapNotNull { it.toPublicacion() }
            .sortedByDescending { it.fechaMillis }
            .take(MAXIMO.toInt())

    override suspend fun crear(publicacion: Publicacion): Publicacion {
        val documento = coleccion.add(publicacion.aDatos()).await()
        return publicacion.copy(id = documento.id)
    }

    override suspend fun actualizar(publicacion: Publicacion) {
        coleccion.document(publicacion.id).update(
            mapOf(
                "titulo" to publicacion.titulo,
                "descripcion" to publicacion.descripcion,
                "imagenUrl" to publicacion.imagenUrl,
                "audiencia" to publicacion.audiencia.name,
                "vigencia" to publicacion.vigencia,
                "editadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun eliminar(id: String) {
        coleccion.document(id).delete().await()
    }

    private fun Publicacion.aDatos() = mapOf(
        "titulo" to titulo,
        "descripcion" to descripcion,
        "imagenUrl" to imagenUrl,
        "audiencia" to audiencia.name,
        "vigencia" to vigencia,
        "autorId" to autorId,
        "fechaMillis" to fechaMillis,
        "creadoEn" to FieldValue.serverTimestamp()
    )

    private companion object {
        const val MAXIMO = 100L
    }
}
