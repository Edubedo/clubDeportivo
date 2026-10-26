package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import com.example.clubdeportivo.util.Textos

/** Cuenta interna de acceso creada para una persona nueva: su código y el uid de su cuenta en Firebase Authentication. */
internal data class CuentaDeAcceso(val codigo: String, val uid: String)

/**
 * Crea las cuentas internas de acceso (una por código) que se entregan a cada persona del club. Se hace con una
 * segunda instancia de Firebase para que crear una cuenta no cierre la sesión de quien da de alta.
 * Lo usan el personal (alta de membresías) y el titular de un paquete familiar (alta de su familia).
 */
internal object CuentasDeAcceso {
    private const val NOMBRE_APP_ALTAS = "altaMiembros"
    private const val MAX_INTENTOS_CODIGO = 10

    fun auth(): FirebaseAuth {
        val principal = FirebaseApp.getInstance()
        val app = runCatching { FirebaseApp.getInstance(NOMBRE_APP_ALTAS) }
            .getOrElse { FirebaseApp.initializeApp(principal.applicationContext, principal.options, NOMBRE_APP_ALTAS) }
        return FirebaseAuth.getInstance(app).also { EmuladoresFirebase.configurar(it) }
    }

    /** Genera un código que no exista en [miembros] y crea su cuenta de acceso. */
    suspend fun crear(miembros: CollectionReference): CuentaDeAcceso {
        val auth = auth()
        repeat(MAX_INTENTOS_CODIGO) {
            val codigo = ReglasMembresia.generarCodigo()
            // Comprobación extra: quien no es del personal no puede leer un documento que no existe (las reglas lo niegan),
            // y en ese caso basta con que la cuenta de acceso no choque (cada código tiene su propio correo interno).
            val yaExiste = runCatching { miembros.document(codigo).get().await().exists() }.getOrDefault(false)
            if (yaExiste) return@repeat
            try {
                val uid = auth.createUserWithEmailAndPassword(ReglasMembresia.emailDeCodigo(codigo), codigo).await()
                    .user?.uid ?: error(Textos.get("the_access_account_could_not_be"))
                auth.signOut()
                return CuentaDeAcceso(codigo, uid)
            } catch (e: FirebaseAuthUserCollisionException) {
                // Ya existía una cuenta con ese código: se genera otro.
            }
        }
        error(Textos.get("a_unique_code_could_not_be"))
    }
}

/** Documento `miembros/{codigo}` de una persona recién dada de alta. */
internal fun datosDeMiembro(persona: PersonaForm, membresiaId: String, parentesco: String, uid: String, hoy: String, esTitular: Boolean) = mapOf(
    "nombre" to persona.nombre.trim(),
    "telefono" to persona.telefono.trim(),
    "correo" to persona.correo.trim(),
    "membresiaId" to membresiaId,
    "parentesco" to parentesco,
    "esTitular" to esTitular,
    "usuarioId" to uid,
    "fechaRegistro" to hoy,
    "creadoEn" to FieldValue.serverTimestamp()
)

/** Perfil de acceso de una persona con código. `email` es el correo interno de la cuenta, no el de contacto (ese vive en `miembros`). */
internal fun datosDeUsuarioMiembro(persona: PersonaForm, codigo: String, rol: String, hoy: String) = mapOf(
    "nombre" to persona.nombre.trim(),
    "email" to ReglasMembresia.emailDeCodigo(codigo),
    "telefono" to persona.telefono.trim(),
    "rol" to rol,
    "estado" to "ACTIVO",
    "codigoMiembro" to codigo,
    "fechaRegistro" to hoy,
    "creadoEn" to FieldValue.serverTimestamp(),
    "actualizadoEn" to FieldValue.serverTimestamp()
)

/** Documento público mínimo que permite saber si un código existe y si ya tiene cuenta, sin exponer datos personales. */
internal fun datosDeRegistroCodigo(uidInterno: String) = mapOf(
    "usuarioId" to uidInterno,
    "cuentaCreada" to false,
    "creadoEn" to FieldValue.serverTimestamp()
)
