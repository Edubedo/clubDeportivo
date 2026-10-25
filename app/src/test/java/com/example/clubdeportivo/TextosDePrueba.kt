package com.example.clubdeportivo

import com.example.clubdeportivo.util.Textos
import java.io.File

/**
 * Las pruebas unitarias corren en la JVM, sin recursos de Android. Esto lee los textos reales en español
 * (los archivos strings de res/values) para que el código que usa [Textos] devuelva lo mismo que en la app.
 */
object TextosDePrueba {
    private val porNombre: Map<String, String> by lazy { leerRecursos() }
    private val nombrePorId: Map<Int, String> by lazy {
        R.string::class.java.fields.filter { it.type == Int::class.javaPrimitiveType }.associate { it.getInt(null) to it.name }
    }

    fun instalar() {
        Textos.resolutorDePrueba = { id, argumentos ->
            val nombre = nombrePorId[id] ?: error("Recurso desconocido: $id")
            val texto = porNombre[nombre] ?: error("Falta el texto $nombre en res/values")
            if (argumentos.isEmpty()) texto else String.format(texto, *argumentos)
        }
    }

    private fun leerRecursos(): Map<String, String> {
        val carpeta = listOf(File("src/main/res/values"), File("app/src/main/res/values")).first { it.isDirectory }
        val mapa = mutableMapOf<String, String>()
        carpeta.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }!!.forEach { archivo ->
            Regex("""<string name="(\w+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(archivo.readText())
                .forEach { mapa[it.groupValues[1]] = decodificar(it.groupValues[2]) }
        }
        return mapa
    }

    /** Quita las comillas que envuelven el valor y resuelve las secuencias de escape de los recursos de Android. */
    private fun decodificar(crudo: String): String {
        val valor = crudo.trim().removeSurrounding("\"")
        val sb = StringBuilder()
        var i = 0
        while (i < valor.length) {
            val c = valor[i]
            if (c == '\\' && i + 1 < valor.length) {
                when (val n = valor[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString().replace("&amp;", "&").replace("&lt;", "<")
    }
}
