package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toUsuario(): Usuario? {
    val nombre = getString("nombre") ?: return null
    return Usuario(
        id = id,
        nombre = nombre,
        correo = getString("email") ?: "",
        rol = Rol.valueOf(getString("rol") ?: Rol.SOCIO.name),
        fotoUrl = getString("fotoUrl")
    )
}

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
                    "nombre", usuario.nombre
                )
                .await()

            Resultado.Exito(usuario)

        } catch (e: Exception) {

            Resultado.Error(
                e.message ?: "No se pudo actualizar el perfil."
            )
        }
    }

    override suspend fun obtenerSocios(): List<Usuario> {
        return usuarios.whereEqualTo("rol", Rol.SOCIO.name).get().await()
            .documents.mapNotNull { it.toUsuario() }
    }
}