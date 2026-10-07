package com.example.clubdeportivo.util

/** Requisitos mínimos de seguridad de una contraseña nueva (alta de personal y registro de socios). */
object ReglasContrasena {
    const val LONGITUD_MINIMA = 8

    data class Requisito(val texto: String, val cumple: Boolean)

    /** Cada requisito con su estado actual, en el orden en que se muestran. */
    fun requisitos(contrasena: String): List<Requisito> = listOf(
        Requisito("Mínimo $LONGITUD_MINIMA caracteres", contrasena.length >= LONGITUD_MINIMA),
        Requisito("Una letra mayúscula", contrasena.any { it.isUpperCase() }),
        Requisito("Una letra minúscula", contrasena.any { it.isLowerCase() }),
        Requisito("Un número", contrasena.any { it.isDigit() }),
        Requisito("Un carácter especial (! @ # \$ % & * ...)", contrasena.any { esEspecial(it) }),
        Requisito("Sin espacios", contrasena.isNotEmpty() && contrasena.none { it.isWhitespace() })
    )

    fun esValida(contrasena: String): Boolean = requisitos(contrasena).all { it.cumple }

    /** Mensaje del primer requisito que falta, o null si la contraseña cumple todos. */
    fun primerError(contrasena: String): String? {
        if (contrasena.isEmpty()) return "Escribe una contraseña."
        return when {
            contrasena.length < LONGITUD_MINIMA -> "La contraseña debe tener al menos $LONGITUD_MINIMA caracteres."
            contrasena.any { it.isWhitespace() } -> "La contraseña no puede tener espacios."
            contrasena.none { it.isUpperCase() } -> "La contraseña debe incluir al menos una letra mayúscula."
            contrasena.none { it.isLowerCase() } -> "La contraseña debe incluir al menos una letra minúscula."
            contrasena.none { it.isDigit() } -> "La contraseña debe incluir al menos un número."
            contrasena.none { esEspecial(it) } -> "La contraseña debe incluir al menos un carácter especial (! @ # \$ % & *)."
            else -> null
        }
    }

    private fun esEspecial(c: Char): Boolean = !c.isLetterOrDigit() && !c.isWhitespace()
}
