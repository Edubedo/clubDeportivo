package com.example.clubdeportivo.util

import android.content.Context
import android.net.Uri
import java.io.File

object FotoPerfilManager {

    fun guardarFoto(
        context: Context,
        uri: Uri,
        usuarioId: String
    ): File? {
        return try {
            val archivo = File(
                context.filesDir,
                "profile_$usuarioId.jpg"
            )

            context.contentResolver.openInputStream(uri)?.use { input ->
                archivo.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            archivo
        } catch (e: Exception) {
            null
        }
    }

    fun obtenerFoto(
        context: Context,
        usuarioId: String
    ): File? {
        val archivo = File(
            context.filesDir,
            "profile_$usuarioId.jpg"
        )

        return archivo.takeIf { it.exists() }
    }

    fun eliminarFoto(
        context: Context,
        usuarioId: String
    ): Boolean {
        val archivo = File(
            context.filesDir,
            "profile_$usuarioId.jpg"
        )

        return if (archivo.exists()) {
            archivo.delete()
        } else {
            true
        }
    }
}