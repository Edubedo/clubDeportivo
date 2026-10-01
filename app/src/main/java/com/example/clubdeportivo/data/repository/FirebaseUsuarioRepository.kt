package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUsuarioRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UsuarioRepository {

    private val usuarios = db.collection("usuarios")

    override suspend fun actualizarNombre(
        usuario: Usuario
    ): Resultado<Usuario> {

        return try {

            usuarios
                .document(usuario.id)
                .update(
                    mapOf(
                        "nombre" to usuario.nombre,
                        "actualizadoEn" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Resultado.Exito(usuario)

        } catch (e: Exception) {

            Resultado.Error(
                e.message ?: "No se pudo actualizar el perfil."
            )
        }
    }
}