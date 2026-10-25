package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Permite probar la app contra los emuladores locales de Firebase en lugar de la base real. Solo se activa al
 * compilar con `-PfirebaseEmulador=10.0.2.2` (la dirección de la computadora vista desde el emulador de Android);
 * sin esa propiedad no hace nada y la app usa el proyecto real.
 */
internal object EmuladoresFirebase {
    private val host: String = BuildConfig.EMULADOR_FIREBASE

    val activo get() = host.isNotBlank()

    fun configurar(auth: FirebaseAuth) {
        if (!activo) return
        runCatching { auth.useEmulator(host, 9099) }
        // Con el emulador no hay reCAPTCHA que validar (y Play Services del emulador de Android suele fallar con él).
        runCatching { auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true) }
    }

    fun configurar(db: FirebaseFirestore) {
        if (activo) runCatching { db.useEmulator(host, 8080) }
    }
}
