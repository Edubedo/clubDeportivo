package com.example.clubdeportivo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado del arranque. La pantalla de arranque de marca se queda a la vista hasta que la app sabe si hay una sesión
 * guardada: así no se ve primero un login y un instante después el inicio (ni una pantalla vacía en medio).
 */
object ArranqueApp {
    var sesionRevisada by mutableStateOf(false)
}
