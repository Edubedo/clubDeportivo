package com.example.clubdeportivo.util

/**
 * Lee los archivos de traducciones (assets/i18n/<idioma>.json): un objeto plano de clave → texto.
 *
 * No usa org.json para que funcione igual en la app y en las pruebas unitarias (JVM), donde las clases de Android
 * no existen.
 */
object LectorJson {

    fun leerTextos(json: String): Map<String, String> = Lector(json).objetoDeTextos()

    private class Lector(private val json: String) {
        private var i = 0

        fun objetoDeTextos(): Map<String, String> {
            val mapa = LinkedHashMap<String, String>()
            esperar('{')
            if (siguiente() == '}') {
                i++
            } else {
                while (true) {
                    val clave = texto()
                    esperar(':')
                    mapa[clave] = texto()
                    when (siguiente()) {
                        ',' -> i++
                        '}' -> { i++; break }
                        else -> error("se esperaba ',' o '}'")
                    }
                }
            }
            if (siguiente() != null) error("hay contenido después del objeto")
            return mapa
        }

        private fun texto(): String {
            esperar('"')
            val sb = StringBuilder()
            while (true) {
                if (i >= json.length) error("texto sin cerrar")
                when (val c = json[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= json.length) error("escape incompleto")
                        when (val e = json[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            'r' -> sb.append('\r')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (i + 4 > json.length) error("escape \\u incompleto")
                                sb.append(json.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> error("escape desconocido \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        /** Salta espacios y devuelve el próximo carácter sin consumirlo (null al final). */
        private fun siguiente(): Char? {
            while (i < json.length && json[i].isWhitespace()) i++
            return json.getOrNull(i)
        }

        private fun esperar(c: Char) {
            if (siguiente() != c) error("se esperaba '$c'")
            i++
        }

        private fun error(motivo: String): Nothing = throw IllegalArgumentException("JSON de traducciones inválido en la posición $i: $motivo")
    }
}
