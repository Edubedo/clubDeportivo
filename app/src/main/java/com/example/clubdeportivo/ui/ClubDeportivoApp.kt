package com.example.clubdeportivo.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.clubdeportivo.ui.areas.AreasDetailScreen
import com.example.clubdeportivo.ui.home.HomeScreen
import com.example.clubdeportivo.ui.admin.home.AdminHomeScreen
import com.example.clubdeportivo.ui.login.LoginScreen
import com.example.clubdeportivo.ui.membresia.MembresiaScreen
import com.example.clubdeportivo.ui.navigation.Destinations
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.navigation.menuPara
import com.example.clubdeportivo.ui.navigation.rutaPermitida
import com.example.clubdeportivo.ui.navigation.rutaInicial
import com.example.clubdeportivo.ui.navigation.tituloPantalla
import com.example.clubdeportivo.ui.perfil.PerfilScreen
import com.example.clubdeportivo.ui.personal.PersonalScreen
import com.example.clubdeportivo.ui.registro.RegistroScreen
import com.example.clubdeportivo.ui.reservas.ReservasScreen
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.ui.reservas.ReservasEncargadoScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDeportivoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val rol = SesionManager.usuarioActual?.rol
    val menu = menuPara(rol)
    val enPantallaDeLogin = currentRoute == null ||
        currentRoute == Destinations.LOGIN ||
        currentRoute == Destinations.REGISTRO

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
                    menu.forEach { item ->
                        val seleccionado = currentRoute == item.route
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = { navegarAPestana(navController, item.route, menu.first().route) },
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
                        navController.navigate(rutaInicial(SesionManager.usuarioActual?.rol)) {
                            popUpTo(Destinations.LOGIN) { inclusive = true }
                        }
                    },
                    onIrRegistro = { navController.navigate(Destinations.REGISTRO) }
                )
            }
            composable(Destinations.REGISTRO) {
                RegistroScreen(
                    onRegistroExitoso = {
                        navController.navigate(rutaInicial(SesionManager.usuarioActual?.rol)) {
                            popUpTo(Destinations.LOGIN) { inclusive = true }
                        }
                    },
                    onVolverALogin = { navController.popBackStack() }
                )
            }
            composable(Destinations.HOME) {
                RutaProtegida(navController, Destinations.HOME) {
                    AdminHomeScreen(
                        onIrPerfil = {
                            navController.navigate(Destinations.PERFIL)
                        }
                    )
                }
            }
            composable(Destinations.PERSONAL) {
                RutaProtegida(navController, Destinations.PERSONAL) { PersonalScreen() }
            }
            composable(Destinations.AREAS) {
                RutaProtegida(navController, Destinations.AREAS) { AreasDetailScreen() }
            }
            composable(Destinations.RESERVAS) {

                val usuario = SesionManager.usuarioActual

                when (usuario?.rol) {

                    Rol.ADMIN_AREA,
                    Rol.AYUDANTE_AREA -> {
                        ReservasEncargadoScreen()
                    }

                    Rol.SOCIO,
                    Rol.VISITANTE_EXTERNO -> {
                        ReservasScreen()
                    }

                    else -> {
                        ReservasScreen()
                    }
                }
            }
            composable(Destinations.MEMBRESIA) { MembresiaScreen() }
            composable(Destinations.PERFIL) {
                PerfilScreen(
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

/** Muestra [contenido] solo si el rol de la sesión puede abrir [ruta]; si no, lo manda a su pantalla inicial. */
@Composable
private fun RutaProtegida(
    navController: androidx.navigation.NavController,
    ruta: String,
    contenido: @Composable () -> Unit
) {
    val rol = SesionManager.usuarioActual?.rol
    if (rutaPermitida(rol, ruta)) {
        contenido()
    } else {
        LaunchedEffect(Unit) {
            navController.navigate(rutaInicial(rol)) { popUpTo(0) { inclusive = true } }
        }
    }
}

/**
 * Cada pestaña del menú inferior SIEMPRE vuelve primero a Inicio y desde ahí abre la
 * pantalla elegida: así el botón atrás se comporta siempre igual (pantalla de detalle ->
 * pestaña -> Inicio -> salir). Traducción directa de la lógica que antes vivía en
 * MainActivity con NavOptions + BottomNavigationView.
 */
private fun navegarAPestana(navController: androidx.navigation.NavController, destino: String, raiz: String) {
    if (navController.currentDestination?.route == destino) return

    navController.navigate(destino) {
        popUpTo(raiz) { inclusive = destino == raiz }
        launchSingleTop = true
    }
}
