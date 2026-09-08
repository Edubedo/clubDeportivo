package com.example.clubdeportivo.util

/**
 * Envuelve el resultado de una operación (llamada a la API o al repositorio)
 * para que la Vista sepa si mostrar datos, un error o un cargando.
 */
sealed class Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>()
    data class Error(val mensaje: String) : Resultado<Nothing>()
}
