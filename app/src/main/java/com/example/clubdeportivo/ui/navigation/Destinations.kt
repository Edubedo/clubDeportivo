package com.example.clubdeportivo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esCliente

/** Rutas equivalentes a los ids de res/navigation/nav_graph.xml. */
object Destinations {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val PERSONAL = "personal"
    const val AREAS = "areas"
    const val RESERVAS = "reservas"
    const val MIS_RESERVAS = "mis_reservas" // 🚀 Nueva ruta exclusiva para socios
    const val MEMBRESIA = "membresia"
    const val PERFIL = "perfil"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/** Pestañas del menú inferior para Administradores. */
val bottomNavItems = listOf(
    BottomNavItem(Destinations.HOME, "Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destinations.PERSONAL, "Personal", Icons.Filled.Person, Icons.Outlined.Person),
    BottomNavItem(Destinations.AREAS, "Áreas", Icons.Filled.Place, Icons.Outlined.Place),
    BottomNavItem(Destinations.RESERVAS, "Reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.MEMBRESIA, "Membresías", Icons.Filled.CardMembership, Icons.Outlined.CardMembership)
)

/** Menú para encargados de área. */
val bottomNavItemsEncargado = listOf(
    BottomNavItem(Destinations.RESERVAS, "Reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.AREAS, "Áreas", Icons.Filled.Place, Icons.Outlined.Place),
    BottomNavItem(Destinations.MEMBRESIA, "Membresías", Icons.Filled.CardMembership, Icons.Outlined.CardMembership),
    BottomNavItem(Destinations.PERFIL, "Perfil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

/** Menú de socios y visitantes: incluye Reservas, Mis reservas, Membresía y Perfil. */
val bottomNavItemsCliente = listOf(
    BottomNavItem(Destinations.RESERVAS, "Reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.MIS_RESERVAS, "Mis reservas", Icons.Filled.ListAlt, Icons.Outlined.ListAlt), // 🚀 Agregado aquí
    BottomNavItem(Destinations.MEMBRESIA, "Membresía", Icons.Filled.CardMembership, Icons.Outlined.CardMembership),
    BottomNavItem(Destinations.PERFIL, "Perfil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

/** Menú inferior según el rol: administradores, encargados de área o clientes. */
fun menuPara(rol: Rol?): List<BottomNavItem> = when {
    rol == null || rol.esCliente() -> bottomNavItemsCliente
    rol.esAdministrador() -> bottomNavItems
    else -> bottomNavItemsEncargado
}

/** Primera pantalla tras entrar: el panel para los administradores, las reservas para todos los demás. */
fun rutaInicial(rol: Rol?): String = if (rol?.esAdministrador() == true) Destinations.HOME else Destinations.RESERVAS

/** Una ruta solo se abre si está en el menú del rol (o es el perfil). */
fun rutaPermitida(rol: Rol?, ruta: String): Boolean =
    ruta == Destinations.PERFIL || menuPara(rol).any { it.route == ruta }

fun tituloPantalla(route: String?): String = when {
    route == Destinations.HOME -> "Inicio"
    route == Destinations.PERSONAL -> "Personal"
    route == Destinations.AREAS -> "Áreas"
    route == Destinations.RESERVAS -> "Reservas"
    route == Destinations.MIS_RESERVAS -> "Mis reservas" // 🚀 Título de la nueva pantalla
    route == Destinations.MEMBRESIA -> "Membresías"
    route == Destinations.PERFIL -> "Mi perfil"
    else -> "ClubDeportivo"
}