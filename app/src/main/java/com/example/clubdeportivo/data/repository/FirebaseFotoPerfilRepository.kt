package com.example.clubdeportivo.data.repository

import android.util.Base64
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Tope de la foto ya comprimida; un documento de Firestore admite hasta 1 MB, aquí hay mucho margen. */
private const val TAMANO_MAXIMO_BYTES = 200 * 1024

/**
 * Guarda la foto dentro del propio perfil, en `usuarios/{uid}.fotoUrl`, como un data URI en base64
 * (`data:image/jpeg;base64,...`). Coil sabe mostrarlo directamente. No usa Firebase Storage, así que
 * funciona con el plan gratuito.
 */
class FirebaseFotoPerfilRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FotoPerfilRepository {

    private val usuarios = db.collection("usuarios")

    override suspend fun subir(usuarioId: String, jpeg: ByteArray): Resultado<String> {
        if (jpeg.size > TAMANO_MAXIMO_BYTES) return Resultado.Error("La foto es demasiado pesada.")

        return try {
            val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)

            usuarios.document(usuarioId)
                .update(mapOf("fotoUrl" to dataUri, "actualizadoEn" to FieldValue.serverTimestamp()))
                .await()

            Resultado.Exito(dataUri)
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo guardar la foto.")
        }
    }

    override suspend fun eliminar(usuarioId: String): Resultado<Unit> = try {
        usuarios.document(usuarioId)
            .update(mapOf("fotoUrl" to FieldValue.delete(), "actualizadoEn" to FieldValue.serverTimestamp()))
            .await()

        Resultado.Exito(Unit)
    } catch (e: Exception) {
        Resultado.Error(e.message ?: "No se pudo quitar la foto.")
    }
}
