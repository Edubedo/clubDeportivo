package com.example.clubdeportivo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esCliente
import com.example.clubdeportivo.data.model.esEncargado

/** Rutas equivalentes a los ids de res/navigation/nav_graph.xml. */
object Destinations {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val PERSONAL = "personal"
    const val AREAS = "areas"
    const val INVENTARIO = "inventario"
    const val RESERVAS = "reservas"
    const val MIS_RESERVAS = "mis_reservas" // 🚀 Nueva ruta exclusiva para socios
    const val MEMBRESIA = "membresia"
    const val PERFIL = "perfil"
    const val REPORTES = "reportes"
}

/** [label] es la clave del texto traducido de la pestaña (en assets/i18n; se resuelve con `tr` al dibujarla). */
data class BottomNavItem(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/** Pestañas del menú inferior para Administradores. */
val bottomNavItems = listOf(
    BottomNavItem(Destinations.HOME, "nav_dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destinations.PERSONAL, "nav_personal", Icons.Filled.Person, Icons.Outlined.Person),
    BottomNavItem(Destinations.AREAS, "nav_areas", Icons.Filled.Place, Icons.Outlined.Place),
    BottomNavItem(Destinations.RESERVAS, "nav_reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.MEMBRESIA, "nav_membres_corto", Icons.Filled.CardMembership, Icons.Outlined.CardMembership),
    BottomNavItem(Destinations.REPORTES, "rep_titulo", Icons.Filled.Assessment, Icons.Outlined.Assessment)
)

/** Menú para encargados de área: las reservas, el área y el inventario de su deporte. */
val bottomNavItemsEncargado = listOf(
    BottomNavItem(Destinations.HOME, "nav_home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destinations.RESERVAS, "nav_reservar", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.AREAS, "nav_mi_area", Icons.Filled.Place, Icons.Outlined.Place)
)

/** Menú de socios y visitantes: incluye Inicio, Reservar y Membresía. */
val bottomNavItemsCliente = listOf(
    BottomNavItem(Destinations.HOME, "nav_home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destinations.RESERVAS, "nav_reservas", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    BottomNavItem(Destinations.MEMBRESIA, "nav_membresia", Icons.Filled.CardMembership, Icons.Outlined.CardMembership)
)

/** Menú inferior según el rol: administradores, encargados de área o clientes. */
fun menuPara(rol: Rol?): List<BottomNavItem> = when {
    rol == null || rol.esCliente() -> bottomNavItemsCliente
    rol.esAdministrador() -> bottomNavItems
    else -> bottomNavItemsEncargado
}

/** Todos los roles llegan a su inicio, que presenta el panel correspondiente a su perfil. */
fun rutaInicial(rol: Rol?): String = Destinations.HOME

/** Una ruta solo se abre si está en el menú del rol o es una ruta secundaria permitida. */
fun rutaPermitida(rol: Rol?, ruta: String): Boolean =
    ruta == Destinations.PERFIL ||
        (rol?.esCliente() == true && ruta == Destinations.MIS_RESERVAS) ||
        (rol?.let { it.esEncargado() && ruta == Destinations.INVENTARIO } == true) ||
        menuPara(rol).any { it.route == ruta }

/** Clave del título de la pantalla en assets/i18n. */
fun tituloPantalla(route: String?): String = when {
    route == Destinations.HOME -> "nav_home"
    route == Destinations.PERSONAL -> "nav_personal"
    route == Destinations.AREAS -> "nav_areas"
    route == Destinations.INVENTARIO -> "titulo_inventario"
    route == Destinations.RESERVAS -> "nav_reservas"
    route == Destinations.MIS_RESERVAS -> "reservas_title"
    route == Destinations.MEMBRESIA -> "titulo_membresias"
    route == Destinations.PERFIL -> "perfil_title"
    route == Destinations.REPORTES -> "rep_titulo"
    else -> "app_name"
}