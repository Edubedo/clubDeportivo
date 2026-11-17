package com.example.clubdeportivo.notificaciones

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

object GestorTokenFCM {
    private const val TAG = "GestorTokenFCM"
    private const val COLECCION = "fcmTokens"

    suspend fun registrar(usuarioId: String, token: String? = null) {
        val tokenActual = token ?: FirebaseMessaging.getInstance().token.await()
        val documentoId = hash(tokenActual)
        FirebaseFirestore.getInstance().collection(COLECCION).document(documentoId).set(
            mapOf(
                "usuarioId" to usuarioId,
                "token" to tokenActual,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun eliminar(usuarioId: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != usuarioId) return
        val token = FirebaseMessaging.getInstance().token.await()
        FirebaseFirestore.getInstance().collection(COLECCION).document(hash(token))
            .delete().await()
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun registrarEnSegundoPlano(usuarioId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { registrar(usuarioId, token) }.onFailure {
                Log.e(TAG, "No se pudo registrar el token FCM para $usuarioId", it)
            }
        }
    }
}
