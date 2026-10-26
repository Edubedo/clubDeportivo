package com.example.clubdeportivo

import com.example.clubdeportivo.util.LectorJson
import com.example.clubdeportivo.util.Textos
import java.io.File

/**
 * Las pruebas unitarias corren en la JVM, sin assets de Android. Esto lee los textos reales en español
 * (assets/i18n/es.json) para que el código que usa [Textos] devuelva lo mismo que en la app.
 */
object TextosDePrueba {
    private val porClave: Map<String, String> by lazy {
        val archivo = listOf(File("src/main/assets/i18n/es.json"), File("app/src/main/assets/i18n/es.json")).first { it.isFile }
        LectorJson.leerTextos(archivo.readText(Charsets.UTF_8))
    }

    fun instalar() {
        Textos.resolutorDePrueba = { clave, argumentos ->
            val texto = porClave[clave] ?: error("Falta el texto $clave en assets/i18n/es.json")
            if (argumentos.isEmpty()) texto else String.format(texto, *argumentos)
        }
    }
}
