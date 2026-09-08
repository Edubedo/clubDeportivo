package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toUsuario(uid: String, correoDeRespaldo: String): Usuario? {
    val nombre = getString("nombre") ?: return null
    return Usuario(
        id = uid,
        nombre = nombre,
        correo = getString("email") ?: correoDeRespaldo,
        rol = Rol.valueOf(getString("rol") ?: Rol.SOCIO.name)
    )
}

/**
 * Login y registro reales con Firebase Authentication (usuario/contraseña). La contraseña
 * la guarda y verifica Firebase, no nosotros — nunca vive en Firestore. El resto del perfil
 * (nombre, rol, etc.) sí se guarda en Firestore, en `usuarios/{uid}`, donde `uid` es el
 * identificador que genera Firebase Authentication para esa cuenta.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val usuarios = db.collection("usuarios")

    override suspend fun login(correo: String, password: String): Resultado<Usuario> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo, password).await()
            val uid = resultado.user?.uid
                ?: return Resultado.Error("No se pudo iniciar sesión, intenta de nuevo.")

            val usuario = usuarios.document(uid).get().await().toUsuario(uid, correo)
                ?: return Resultado.Error("Tu cuenta no tiene un perfil guardado. Contacta a un administrador.")

            Resultado.Exito(usuario)
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        }
    }

    override suspend fun registrar(nombre: String, correo: String, password: String, rol: Rol): Resultado<Usuario> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(correo, password).await()
            val uid = resultado.user?.uid
                ?: return Resultado.Error("No se pudo crear la cuenta, intenta de nuevo.")

            val datos = mapOf(
                "nombre" to nombre,
                "email" to correo,
                "rol" to rol.name,
                "estado" to "ACTIVO",
                "fechaRegistro" to Fechas.hoy()
            )
            usuarios.document(uid).set(datos).await()

            Resultado.Exito(Usuario(id = uid, nombre = nombre, correo = correo, rol = rol))
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        }
    }

    private fun traducirError(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (usa 6+ caracteres)."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        else -> e.message ?: "Ocurrió un error de conexión. Revisa tu internet e intenta de nuevo."
    }
}
