package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.DatosPersonal
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.data.model.ROLES_DE_PERSONAL
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Roles que cuentan como personal. Incluye los heredados (SUPERADMIN, ADMIN_AREA) para no perder a nadie ya registrado. */
private val ROLES_QUE_SON_PERSONAL = (ROLES_DE_PERSONAL + listOf(Rol.SUPERADMIN, Rol.ADMIN_AREA)).map { it.name }

private fun DocumentSnapshot.toPersonal(): Personal? {
    val nombre = getString("nombre") ?: return null
    val rol = runCatching { Rol.valueOf(getString("rol").orEmpty()) }.getOrNull() ?: return null
    return Personal(
        id = id,
        nombre = nombre,
        email = getString("email") ?: getString("correo").orEmpty(),
        telefono = getString("telefono").orEmpty(),
        estado = getString("estado") ?: "ACTIVO",
        rol = rol,
        tipoPersonal = getString("tipoPersonal").orEmpty(),
        turno = getString("turno").orEmpty(),
        areaTrabajo = getString("areaTrabajo")?.takeIf { it.isNotBlank() },
        fotoUrl = getString("fotoUrl")?.takeIf { it.isNotBlank() },
        fechaIngreso = getString("fechaIngreso") ?: getString("fechaRegistro").orEmpty()
    )
}

class FirebasePersonalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authPrincipal: FirebaseAuth = FirebaseAuth.getInstance()
) : PersonalRepository {

    private val usuarios = db.collection("usuarios")

    /**
     * Crear una cuenta con `createUserWithEmailAndPassword` deja iniciada la sesión de la cuenta nueva y
     * sacaría al administrador de la suya. Por eso las altas se hacen en una segunda instancia de Firebase
     * (mismo proyecto, sesión aparte) que se cierra apenas termina.
     */
    private fun authDeAltas(): FirebaseAuth {
        val principal = FirebaseApp.getInstance()
        val app = runCatching { FirebaseApp.getInstance(NOMBRE_APP_ALTAS) }
            .getOrElse { FirebaseApp.initializeApp(principal.applicationContext, principal.options, NOMBRE_APP_ALTAS) }
        return FirebaseAuth.getInstance(app)
    }

    override suspend fun obtenerPersonal(): List<Personal> =
        usuarios.whereIn("rol", ROLES_QUE_SON_PERSONAL).get().await().documents
            .mapNotNull { it.toPersonal() }
            .sortedBy { it.nombre.lowercase() }

    override suspend fun contarPersonal(): Int =
        usuarios.whereIn("rol", ROLES_QUE_SON_PERSONAL).get().await().size()

    override suspend fun crear(datos: DatosPersonal): Resultado<Personal> {
        val auth = authDeAltas()
        return try {
            val uid = auth.createUserWithEmailAndPassword(datos.email.trim(), datos.contrasena).await().user?.uid
                ?: return Resultado.Error("No se pudo crear la cuenta, intenta de nuevo.")
            val ahora = FieldValue.serverTimestamp()
            usuarios.document(uid).set(
                camposDe(datos) + mapOf(
                    "email" to datos.email.trim(),
                    "fechaRegistro" to Fechas.hoy(),
                    "fechaIngreso" to Fechas.hoy(),
                    "creadoPor" to (authPrincipal.currentUser?.uid ?: ""),
                    "creadoEn" to ahora
                )
            ).await()
            Resultado.Exito(
                Personal(
                    uid, datos.nombre.trim(), datos.email.trim(), datos.telefono.trim(), datos.estado, datos.rol,
                    datos.tipoPersonal, datos.turno, datos.areaTrabajo, datos.fotoUrl, Fechas.hoy()
                )
            )
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        } finally {
            auth.signOut()
        }
    }

    override suspend fun actualizar(id: String, datos: DatosPersonal): Resultado<Unit> = try {
        // merge: solo se tocan estos campos (el código de miembro, la fecha de registro, etc. se conservan).
        usuarios.document(id).set(camposDe(datos), SetOptions.merge()).await()
        Resultado.Exito(Unit)
    } catch (e: Exception) {
        Resultado.Error(traducirError(e))
    }

    override suspend fun cambiarContrasena(id: String, contrasena: String): Resultado<Unit> {
        val token = try {
            authPrincipal.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        } ?: return Resultado.Error("Tu sesión expiró. Cierra sesión y vuelve a entrar.")

        val url = "https://us-central1-${FirebaseApp.getInstance().options.projectId}.cloudfunctions.net/$FUNCION_CAMBIAR_CONTRASENA"
        return withContext(Dispatchers.IO) {
            val conexion = URL(url).openConnection() as HttpURLConnection
            try {
                conexion.requestMethod = "POST"
                conexion.connectTimeout = 15_000
                conexion.readTimeout = 20_000
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json")
                conexion.setRequestProperty("Authorization", "Bearer $token")
                val cuerpo = JSONObject().put("data", JSONObject().put("uid", id).put("contrasena", contrasena))
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }

                val codigo = conexion.responseCode
                val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(texto) }.getOrNull()
                if (codigo in 200..299 && json?.optJSONObject("result")?.optBoolean("ok") == true) {
                    Resultado.Exito(Unit)
                } else {
                    // Los mensajes de la función ya vienen en español y listos para mostrar; si no hay, se explica lo general.
                    Resultado.Error(
                        json?.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                            ?: if (codigo == 404) "La función para cambiar contraseñas todavía no está publicada en Firebase."
                            else "No se pudo cambiar la contraseña. Intenta de nuevo."
                    )
                }
            } catch (e: Exception) {
                Resultado.Error("No se pudo conectar. Revisa tu internet e intenta de nuevo.")
            } finally {
                conexion.disconnect()
            }
        }
    }

    private fun camposDe(datos: DatosPersonal): Map<String, Any?> = mapOf(
        "nombre" to datos.nombre.trim(),
        "telefono" to datos.telefono.trim(),
        "estado" to datos.estado,
        "rol" to datos.rol.name,
        "tipoPersonal" to datos.tipoPersonal,
        "turno" to datos.turno,
        // Sin área = "" (el campo siempre existe).
        "areaTrabajo" to (datos.areaTrabajo ?: ""),
        "fotoUrl" to datos.fotoUrl,
        "actualizadoEn" to FieldValue.serverTimestamp()
    )

    private fun traducirError(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
        else -> e.message ?: "Ocurrió un error de conexión. Revisa tu internet e intenta de nuevo."
    }

    private companion object {
        const val NOMBRE_APP_ALTAS = "altas-de-personal"
        const val FUNCION_CAMBIAR_CONTRASENA = "cambiarContrasenaPersonal"
    }
}
