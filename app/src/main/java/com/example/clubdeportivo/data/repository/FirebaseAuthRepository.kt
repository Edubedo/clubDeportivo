package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.Resultado
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toUsuario(
    uid: String,
    correoDeRespaldo: String
): Usuario? {

    val nombre = getString("nombre") ?: return null

    return Usuario(
        id = uid,
        nombre = nombre,
        correo = getString("email") ?: correoDeRespaldo,
        rol = Rol.valueOf(getString("rol") ?: Rol.SOCIO.name),
        fotoUrl = getString("fotoUrl"),
        areaTrabajo = getString("areaTrabajo")
    )
}

/**
 * Login y registro reales con Firebase Authentication (correo y contraseña). La contraseña la guarda y verifica
 * Firebase, no nosotros — nunca vive en Firestore. El resto del perfil (nombre, rol, etc.) sí se guarda en
 * Firestore, en `usuarios/{uid}`, donde `uid` es el identificador que genera Firebase Authentication.
 *
 * Los miembros del club se registran con el código que les dio el personal (ver [registrarMiembro]).
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val usuarios = db.collection("usuarios")
    private val registroCodigos = db.collection("registroCodigos")

    override suspend fun login(correo: String, password: String): Resultado<Usuario> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo, password).await()
            val uid = resultado.user?.uid
                ?: return Resultado.Error("No se pudo iniciar sesión, intenta de nuevo.")

            val perfil = usuarios.document(uid).get().await()
            if (perfil.getString("estado") == "INACTIVO") {
                auth.signOut()
                return Resultado.Error("Tu cuenta está desactivada. Contacta a un administrador.")
            }
            val usuario = perfil.toUsuario(uid, correo)
                ?: return Resultado.Error("Tu cuenta no tiene un perfil guardado. Contacta a un administrador.")

            Resultado.Exito(usuario)
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        }
    }

    override suspend fun consultarCodigo(codigo: String): Resultado<EstadoCodigo> {
        val codigoNormalizado = ReglasMembresia.normalizarCodigo(codigo)
        if (!ReglasMembresia.esCodigo(codigoNormalizado)) {
            return Resultado.Error("El código no tiene el formato correcto. Se ve así: CLB-7K3M9Q.")
        }
        return try {
            val registro = registroCodigos.document(codigoNormalizado).get().await()
            when {
                !registro.exists() ->
                    Resultado.Error("No encontramos ese código. Revísalo o pídelo en recepción.")
                registro.getBoolean("cuentaCreada") == true -> Resultado.Exito(EstadoCodigo.YA_REGISTRADO)
                else -> Resultado.Exito(EstadoCodigo.DISPONIBLE)
            }
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        }
    }

    /**
     * Registro de un miembro con su código. El personal ya dejó al miembro dado de alta con una cuenta interna
     * cuya clave es el propio código; conocerlo es la prueba de que la persona es quien dice ser. El registro:
     *
     *  1. Entra con esa cuenta interna (en una segunda instancia de Firebase, para no tocar la sesión principal).
     *  2. Crea la cuenta real de la persona (su correo y su contraseña).
     *  3. Marca el código como "ya registrado" y apunta el miembro a la cuenta real.
     *  4. Crea el perfil de la cuenta real con el rol y el código del miembro.
     *  5. Borra la cuenta interna, para que el código ya no sirva como contraseña.
     *
     * Si algo falla a medias se deshace lo hecho, así el código sigue disponible para intentarlo otra vez.
     */
    override suspend fun registrarMiembro(codigo: String, nombre: String, correo: String, password: String): Resultado<Usuario> {
        val codigoNormalizado = ReglasMembresia.normalizarCodigo(codigo)
        if (!ReglasMembresia.esCodigo(codigoNormalizado)) {
            return Resultado.Error("El código no tiene el formato correcto. Se ve así: CLB-7K3M9Q.")
        }

        val appCodigo = appSecundaria()
        val authCodigo = FirebaseAuth.getInstance(appCodigo)
        val dbCodigo = FirebaseFirestore.getInstance(appCodigo)

        var cuentaNueva: com.google.firebase.auth.FirebaseUser? = null
        var uidInterno: String? = null
        var reclamado = false
        return try {
            // 1. Prueba de que conoce el código.
            uidInterno = try {
                authCodigo.signInWithEmailAndPassword(ReglasMembresia.emailDeCodigo(codigoNormalizado), codigoNormalizado)
                    .await().user?.uid
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return Resultado.Error("No pudimos validar ese código. Si ya creaste tu cuenta, inicia sesión con tu correo.")
            } catch (e: FirebaseAuthInvalidUserException) {
                return Resultado.Error("No pudimos validar ese código. Si ya creaste tu cuenta, inicia sesión con tu correo.")
            } ?: return Resultado.Error("No se pudo validar el código, intenta de nuevo.")

            val perfilInterno = dbCodigo.collection("usuarios").document(uidInterno).get().await()
            val miembro = dbCodigo.collection("miembros").document(codigoNormalizado).get().await()
            if (!miembro.exists()) {
                return Resultado.Error("Este código ya no está activo. Habla con recepción.")
            }
            val rol = runCatching { Rol.valueOf(perfilInterno.getString("rol").orEmpty()) }.getOrDefault(Rol.SOCIO)

            // 2. Cuenta real.
            cuentaNueva = auth.createUserWithEmailAndPassword(correo.trim(), password).await().user
            val uidNuevo = cuentaNueva?.uid ?: return Resultado.Error("No se pudo crear la cuenta, intenta de nuevo.")

            // 3. Reclamo del código (con la sesión del código).
            val lote = dbCodigo.batch()
            lote.update(
                dbCodigo.collection("registroCodigos").document(codigoNormalizado),
                mapOf("cuentaCreada" to true, "usuarioNuevo" to uidNuevo, "activadaEn" to FieldValue.serverTimestamp())
            )
            lote.update(
                dbCodigo.collection("miembros").document(codigoNormalizado),
                mapOf(
                    "usuarioId" to uidNuevo,
                    "cuentaCreada" to true,
                    "correoCuenta" to correo.trim(),
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            lote.commit().await()
            reclamado = true

            // 4. Perfil de la cuenta real (con la sesión principal).
            usuarios.document(uidNuevo).set(
                mapOf(
                    "nombre" to nombre.trim(),
                    "email" to correo.trim(),
                    "telefono" to (perfilInterno.getString("telefono") ?: ""),
                    "rol" to rol.name,
                    "estado" to "ACTIVO",
                    "codigoMiembro" to codigoNormalizado,
                    "fechaRegistro" to Fechas.hoy(),
                    "creadoEn" to FieldValue.serverTimestamp(),
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            ).await()

            // 5. La cuenta interna ya no hace falta: se borra para que el código deje de servir como contraseña.
            runCatching { authCodigo.currentUser?.delete()?.await() }
            authCodigo.signOut()

            Resultado.Exito(Usuario(id = uidNuevo, nombre = nombre.trim(), correo = correo.trim(), rol = rol))
        } catch (e: Exception) {
            deshacerRegistro(authCodigo, dbCodigo, codigoNormalizado, uidInterno, reclamado, cuentaNueva)
            Resultado.Error(traducirError(e, alRegistrar = true))
        } finally {
            runCatching { authCodigo.signOut() }
        }
    }

    /**
     * Deja todo como estaba antes de intentar el registro (el código sigue disponible y la cuenta nueva no queda a medias).
     * Las reglas de Firestore solo dejan tocar cada documento a quien lo reclamó: el registro del código lo revierte
     * la sesión interna del código y el miembro (que ya apunta a la cuenta nueva) lo revierte la sesión de la cuenta
     * nueva, así que ambas siguen abiertas hasta aquí y la cuenta nueva se borra al final.
     */
    private suspend fun deshacerRegistro(
        authCodigo: FirebaseAuth,
        dbCodigo: FirebaseFirestore,
        codigo: String,
        uidInterno: String?,
        reclamado: Boolean,
        cuentaNueva: com.google.firebase.auth.FirebaseUser?
    ) {
        if (reclamado && uidInterno != null) {
            runCatching {
                dbCodigo.collection("registroCodigos").document(codigo).update(
                    mapOf("cuentaCreada" to false, "usuarioNuevo" to FieldValue.delete(), "activadaEn" to FieldValue.delete())
                ).await()
            }
            runCatching {
                db.collection("miembros").document(codigo).update(
                    mapOf(
                        "usuarioId" to uidInterno,
                        "cuentaCreada" to false,
                        "correoCuenta" to FieldValue.delete(),
                        "actualizadoEn" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        runCatching { cuentaNueva?.delete()?.await() }
        runCatching { auth.signOut() }
        authCodigo.signOut()
    }

    private fun appSecundaria(): FirebaseApp {
        val principal = FirebaseApp.getInstance()
        return runCatching { FirebaseApp.getInstance(NOMBRE_APP_REGISTRO) }
            .getOrElse { FirebaseApp.initializeApp(principal.applicationContext, principal.options, NOMBRE_APP_REGISTRO) }
    }

    override suspend fun enviarRestablecimiento(correo: String): Resultado<Unit> {
        return try {
            auth.sendPasswordResetEmail(correo.trim()).await()
            Resultado.Exito(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Resultado.Error("No existe una cuenta con ese correo.")
        } catch (e: Exception) {
            Resultado.Error(traducirError(e))
        }
    }

    override suspend fun restaurarSesion(): Usuario? {
        val actual = auth.currentUser ?: return null
        return try {
            val perfil = usuarios.document(actual.uid).get().await()
            if (perfil.getString("estado") == "INACTIVO") {
                auth.signOut()
                return null
            }
            perfil.toUsuario(actual.uid, actual.email.orEmpty())
        } catch (e: Exception) {
            // Sin conexión o sin permisos: se pide iniciar sesión de nuevo.
            null
        }
    }

    override fun cerrarSesion() {
        auth.signOut()
    }

    private fun traducirError(e: Exception, alRegistrar: Boolean = false): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo. Inicia sesión o usa otro correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usa 8+ caracteres con mayúscula, número y símbolo."
        is FirebaseAuthInvalidCredentialsException ->
            if (alRegistrar) "El correo no es válido. Revísalo e intenta de nuevo." else "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet e intenta de nuevo."
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "No tienes permiso para hacer esto. Si crees que es un error, avisa al administrador del club."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Revisa tu internet e intenta de nuevo."
            else -> "Algo salió mal. Intenta de nuevo en un momento."
        }
        else -> "Algo salió mal. Revisa tu conexión e intenta de nuevo."
    }

    private companion object {
        const val NOMBRE_APP_REGISTRO = "registroMiembros"
    }
}
