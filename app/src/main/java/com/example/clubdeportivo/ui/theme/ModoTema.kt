package com.example.clubdeportivo.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Apariencia de la app: la que tenga el teléfono, siempre clara o siempre oscura. */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Automático"),
    CLARO("Claro"),
    OSCURO("Oscuro");

    companion object {
        fun deNombre(nombre: String?): ModoTema = entries.firstOrNull { it.name == nombre } ?: SISTEMA
    }
}

/** Guarda en el teléfono la apariencia que eligió la persona, para que siga igual la próxima vez que abra la app. */
object PreferenciaTema {
    private const val ARCHIVO = "preferencias_club"
    private const val CLAVE_MODO = "modo_tema"

    var modo by mutableStateOf(ModoTema.SISTEMA)
        private set

    fun cargar(contexto: Context) {
        modo = runCatching {
            ModoTema.deNombre(contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE_MODO, null))
        }.getOrDefault(ModoTema.SISTEMA)
    }

    fun elegir(contexto: Context, nuevo: ModoTema) {
        modo = nuevo
        runCatching { contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE_MODO, nuevo.name).apply() }
    }
}
