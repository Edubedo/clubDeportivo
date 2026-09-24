package com.example.clubdeportivo.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector

/** Rutas equivalentes a los ids de res/navigation/nav_graph.xml. */
object Destinations {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val PERSONAL = "personal"
    const val AREAS = "areas"
    const val RESERVAR = "reservar/{areaId}/{areaNombre}"
    const val MIS_RESERVAS = "misReservas"
    const val MEMBRESIA = "membresia"
    const val PERFIL = "perfil"

    fun reservar(areaId: String, areaNombre: String) = "reservar/${Uri.encode(areaId)}/${Uri.encode(areaNombre)}"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/** Pestañas del menú inferior; el mismo set que idsMenuInferior en el MainActivity anterior. */
val bottomNavItems = listOf(
    BottomNavItem(Destinations.HOME, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destinations.PERSONAL, "Personal", Icons.Filled.Person, Icons.Outlined.Person),
    BottomNavItem(Destinations.AREAS, "Áreas", Icons.Filled.Place, Icons.Outlined.Place),
    BottomNavItem(Destinations.MIS_RESERVAS, "Reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.PERFIL, "Perfil", Icons.Filled.Person, Icons.Outlined.Person),
)

fun tituloPantalla(route: String?): String = when {
    route == Destinations.HOME -> "Inicio"
    route == Destinations.PERSONAL -> "Personal"
    route == Destinations.AREAS -> "Áreas del club"
    route?.startsWith("reservar/") == true -> "Reservar"
    route == Destinations.MIS_RESERVAS -> "Mis reservas"
    route == Destinations.MEMBRESIA -> "Mi membresía"
    route == Destinations.PERFIL -> "Mi perfil"
    else -> "ClubDeportivo"
}
