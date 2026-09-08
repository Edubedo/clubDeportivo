package com.example.clubdeportivo.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.clubdeportivo.ui.areas.AreasScreen
import com.example.clubdeportivo.ui.home.HomeScreen
import com.example.clubdeportivo.ui.login.LoginScreen
import com.example.clubdeportivo.ui.membresia.MembresiaScreen
import com.example.clubdeportivo.ui.navigation.Destinations
import com.example.clubdeportivo.ui.navigation.bottomNavItems
import com.example.clubdeportivo.ui.navigation.tituloPantalla
import com.example.clubdeportivo.ui.perfil.PerfilScreen
import com.example.clubdeportivo.ui.reservar.ReservarScreen
import com.example.clubdeportivo.ui.reservas.MisReservasScreen
import com.example.clubdeportivo.ui.torneos.TorneosScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDeportivoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val enPantallaDeLogin = currentRoute == null || currentRoute == Destinations.LOGIN

    Scaffold(
        topBar = {
            if (!enPantallaDeLogin) {
                TopAppBar(
                    title = { Text(tituloPantalla(currentRoute)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            if (!enPantallaDeLogin) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val seleccionado = currentRoute == item.route
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = { navegarAPestana(navController, item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (seleccionado) item.iconSelected else item.iconUnselected,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destinations.HOME) {
                HomeScreen(
                    onIrAreas = { navController.navigate(Destinations.AREAS) },
                    onIrReservas = { navController.navigate(Destinations.MIS_RESERVAS) },
                    onIrTorneos = { navController.navigate(Destinations.TORNEOS) },
                    onIrMembresia = { navController.navigate(Destinations.MEMBRESIA) }
                )
            }
            composable(Destinations.AREAS) {
                AreasScreen(
                    onAreaClick = { area ->
                        navController.navigate(Destinations.reservar(area.id, area.nombre))
                    }
                )
            }
            composable(
                route = Destinations.RESERVAR,
                arguments = listOf(
                    navArgument("areaId") { type = NavType.IntType },
                    navArgument("areaNombre") { type = NavType.StringType }
                )
            ) { entry ->
                val areaId = entry.arguments?.getInt("areaId") ?: 0
                val areaNombre = Uri.decode(entry.arguments?.getString("areaNombre").orEmpty())
                ReservarScreen(areaId = areaId, areaNombre = areaNombre)
            }
            composable(Destinations.MIS_RESERVAS) { MisReservasScreen() }
            composable(Destinations.MEMBRESIA) { MembresiaScreen() }
            composable(Destinations.TORNEOS) { TorneosScreen() }
            composable(Destinations.PERFIL) {
                PerfilScreen(
                    onVerMembresia = { navController.navigate(Destinations.MEMBRESIA) },
                    onCerrarSesion = {
                        navController.navigate(Destinations.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Cada pestaña del menú inferior SIEMPRE vuelve primero a Inicio y desde ahí abre la
 * pantalla elegida: así el botón atrás se comporta siempre igual (pantalla de detalle ->
 * pestaña -> Inicio -> salir). Traducción directa de la lógica que antes vivía en
 * MainActivity con NavOptions + BottomNavigationView.
 */
private fun navegarAPestana(navController: androidx.navigation.NavController, destino: String) {
    if (navController.currentDestination?.route == destino) return

    navController.navigate(destino) {
        popUpTo(Destinations.HOME) { inclusive = destino == Destinations.HOME }
        launchSingleTop = true
    }
}
