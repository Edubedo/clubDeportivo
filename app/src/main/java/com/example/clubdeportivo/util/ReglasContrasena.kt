package com.example.clubdeportivo.util

import com.example.clubdeportivo.R
/** Requisitos mínimos de seguridad de una contraseña nueva (alta de personal y registro de socios). */
object ReglasContrasena {
    const val LONGITUD_MINIMA = 8

    data class Requisito(val texto: String, val cumple: Boolean)

    /** Cada requisito con su estado actual, en el orden en que se muestran. */
    fun requisitos(contrasena: String): List<Requisito> = listOf(
        Requisito(Textos.get(R.string.minimum_characters, LONGITUD_MINIMA), contrasena.length >= LONGITUD_MINIMA),
        Requisito(Textos.get(R.string.one_uppercase_letter), contrasena.any { it.isUpperCase() }),
        Requisito(Textos.get(R.string.one_lowercase_letter), contrasena.any { it.isLowerCase() }),
        Requisito(Textos.get(R.string.one_number), contrasena.any { it.isDigit() }),
        Requisito(Textos.get(R.string.one_special_character), contrasena.any { esEspecial(it) }),
        Requisito(Textos.get(R.string.no_spaces), contrasena.isNotEmpty() && contrasena.none { it.isWhitespace() })
    )

    fun esValida(contrasena: String): Boolean = requisitos(contrasena).all { it.cumple }

    /** Mensaje del primer requisito que falta, o null si la contraseña cumple todos. */
    fun primerError(contrasena: String): String? {
        if (contrasena.isEmpty()) return Textos.get(R.string.enter_a_password)
        return when {
            contrasena.length < LONGITUD_MINIMA -> Textos.get(R.string.the_password_must_be_at_least, LONGITUD_MINIMA)
            contrasena.any { it.isWhitespace() } -> Textos.get(R.string.the_password_can_t_contain_spaces)
            contrasena.none { it.isUpperCase() } -> Textos.get(R.string.the_password_must_include_at_least)
            contrasena.none { it.isLowerCase() } -> Textos.get(R.string.the_password_must_include_at_least_2)
            contrasena.none { it.isDigit() } -> Textos.get(R.string.the_password_must_include_at_least_3)
            contrasena.none { esEspecial(it) } -> Textos.get(R.string.the_password_must_include_at_least_4)
            else -> null
        }
    }

    private fun esEspecial(c: Char): Boolean = !c.isLetterOrDigit() && !c.isWhitespace()
}
