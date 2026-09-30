package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.inventario.InventarioScreen

/** Sección "Áreas": gestión de áreas e inventario. (Los torneos viven en "Reservas".) */
@Composable
fun AreasDetailScreen() {
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        PestanasPildora(
            opciones = listOf("Áreas", "Inventario"),
            seleccionada = pestana,
            onSeleccion = { pestana = it }
        )

        when (pestana) {
            0 -> AreasScreen()
            1 -> InventarioScreen()
        }
    }
}
