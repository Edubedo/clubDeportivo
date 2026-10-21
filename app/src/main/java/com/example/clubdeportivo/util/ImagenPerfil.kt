package com.example.clubdeportivo.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Lado máximo (en píxeles) de la foto de perfil: de sobra para un avatar y deja la imagen en ~20-40 KB. */
private const val LADO_MAXIMO = 256
private const val CALIDAD_JPEG = 75

/**
 * Lee la imagen de [uri], la reduce a [LADO_MAXIMO] px, corrige la rotación que traen las fotos de la cámara
 * y la devuelve como JPEG listo para guardar en el perfil. Devuelve null si no se pudo leer. Bloquea: llamar fuera del hilo principal.
 */
fun comprimirFotoPerfil(resolver: ContentResolver, uri: Uri): ByteArray? = try {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }

    var muestreo = 1
    while (medidas.outWidth / (muestreo * 2) >= LADO_MAXIMO && medidas.outHeight / (muestreo * 2) >= LADO_MAXIMO) {
        muestreo *= 2
    }
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }

    original?.let { bitmap ->
        val rotacion = resolver.openInputStream(uri)?.use { leerRotacion(it) } ?: 0
        val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(bitmap.width, bitmap.height))
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(rotacion.toFloat())
        }
        val final = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)

        ByteArrayOutputStream().use { salida ->
            final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
            salida.toByteArray()
        }
    }
} catch (e: Exception) {
    null
}

private fun leerRotacion(entrada: java.io.InputStream): Int =
    when (ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
