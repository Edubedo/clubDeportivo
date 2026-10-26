package com.example.clubdeportivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Revisa que los archivos de assets/i18n estén completos y que [LectorJson] los lea bien. */
class TraduccionesJsonTest {

    private fun leer(codigo: String): Map<String, String> {
        val archivo = listOf(File("src/main/assets/i18n/$codigo.json"), File("app/src/main/assets/i18n/$codigo.json")).first { it.isFile }
        return LectorJson.leerTextos(archivo.readText(Charsets.UTF_8))
    }

    private val marcador = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""")

    @Test
    fun cadaIdiomaTieneLasMismasClaves() {
        val es = leer(Idioma.ES.codigo)
        Idioma.entries.forEach { idioma ->
            val otro = leer(idioma.codigo)
            assertEquals("Claves que faltan en ${idioma.codigo}.json", emptySet<String>(), es.keys - otro.keys)
            assertEquals("Claves de más en ${idioma.codigo}.json", emptySet<String>(), otro.keys - es.keys)
        }
    }

    @Test
    fun lasTraduccionesUsanLosMismosMarcadores() {
        val es = leer(Idioma.ES.codigo)
        val en = leer(Idioma.EN.codigo)
        val distintos = es.keys.filter { clave ->
            marcador.findAll(es.getValue(clave)).map { it.value }.sorted().toList() !=
                marcador.findAll(en.getValue(clave)).map { it.value }.sorted().toList()
        }
        assertTrue("Marcadores distintos entre es y en: $distintos", distintos.isEmpty())
    }

    @Test
    fun lectorResuelveEscapes() {
        val textos = LectorJson.leerTextos("""{ "a": "Línea 1\nLínea 2", "b": "Dice \"hola\" \\ ñ", "c": "" }""")
        assertEquals("Línea 1\nLínea 2", textos["a"])
        assertEquals("Dice \"hola\" \\ ñ", textos["b"])
        assertEquals("", textos["c"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun lectorRechazaJsonRoto() {
        LectorJson.leerTextos("""{ "a": "sin cerrar }""")
    }
}
