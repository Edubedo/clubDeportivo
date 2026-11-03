package com.example.clubdeportivo.ui.components

import com.example.clubdeportivo.R

fun imagenDeporte(deporte: String): Int {
    val nombre = deporte.trim().lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")

    return when {


        nombre.contains("futbol") ->
            R.drawable.area_futbol

        nombre.contains("basquet") ||
        nombre.contains("baloncesto") ->
            R.drawable.area_basquetbol

        nombre.contains("natacion") ||
        nombre.contains("alberca") ->
            R.drawable.area_natacion

        nombre.contains("tenis") ->
            R.drawable.area_tenis

        else -> R.drawable.logo_club
    }
}