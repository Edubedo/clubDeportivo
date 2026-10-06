package com.example.clubdeportivo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.clubdeportivo.ui.areas.AreasDetailScreen
import com.example.clubdeportivo.ui.admin.home.AdminHomeScreen
import com.example.clubdeportivo.ui.login.LoginScreen
import com.example.clubdeportivo.ui.membresia.MembresiaScreen
import com.example.clubdeportivo.ui.navigation.Destinations
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.navigation.menuPara
import com.example.clubdeportivo.ui.navigation.rutaPermitida
import com.example.clubdeportivo.ui.navigation.rutaInicial
import com.example.clubdeportivo.ui.perfil.PerfilScreen
import com.example.clubdeportivo.ui.personal.PersonalScreen
import com.example.clubdeportivo.ui.registro.RegistroScreen
import com.example.clubdeportivo.ui.reservas.ReservasScreen
import com.example.clubdeportivo.ui.reservas.MisReservasScreen
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.ui.reservas.ReservasEncargadoScreen
import com.example.clubdeportivo.ui.admin.home.EnviarNotificacionSheet
import com.example.clubdeportivo.ui.personal.PersonalNotificacionesSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDeportivoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val usuario = SesionManager.usuarioActual
    val rol = usuario?.rol
    val menu = menuPara(rol)
    val enPantallaDeLogin = currentRoute == null ||
            currentRoute == Destinations.LOGIN ||
            currentRoute == Destinations.REGISTRO

    var mostrarNotificacionesGlobal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!enPantallaDeLogin) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (rol == Rol.SOCIO)
                                    "Bienvenido, ${usuario?.nombre ?: "Socio"}"
                                else "Club Deportivo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF192338)
                            )
                            Text(
                                text = when (rol) {
                                    Rol.ADMIN -> "Admin"
                                    Rol.ADMIN_AREA, Rol.AYUDANTE_AREA -> "Instructor"
                                    else -> "Socio del Club"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF31487A)
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            // 🌐 Botón minimalista de Idioma
                            Surface(
                                onClick = { },
                                shape = CircleShape,
                                color = Color(0xFFD6E4FE),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🌐", fontSize = 14.sp)
                                }
                            }

                            // 📢 Botón de Notificaciones
                            Surface(
                                onClick = { mostrarNotificacionesGlobal = true },
                                shape = CircleShape,
                                color = Color(0xFFD6E4FE),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📢", fontSize = 14.sp)
                                }
                            }

                            // 👤 Botón de Perfil superior (Visible SOLO para Admin e Instructores, oculto para Socios)
                            if (rol != Rol.SOCIO) {
                                Surface(
                                    onClick = { navController.navigate(Destinations.PERFIL) },
                                    shape = CircleShape,
                                    color = Color(0xFFD6E4FE),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = usuario?.nombre?.firstOrNull()?.uppercase() ?: "A",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E2E4F),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF192338)
                    )
                )
            }
        },
        bottomBar = {
            if (!enPantallaDeLogin) {
                NavigationBar(
                    containerColor = Color.White
                ) {
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
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1E2E4F),
                                unselectedIconColor = Color(0xFF31487A),
                                selectedTextColor = Color(0xFF1E2E4F),
                                unselectedTextColor = Color(0xFF31487A),
                                indicatorColor = Color(0xFFD6E4FE)
                            )
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
                val rolActual = SesionManager.usuarioActual?.rol
                when (rolActual) {
                    Rol.ADMIN_AREA,
                    Rol.AYUDANTE_AREA -> {
                        ReservasEncargadoScreen()
                    }
                    else -> {
                        ReservasScreen()
                    }
                }
            }
            composable(Destinations.MIS_RESERVAS) {
                RutaProtegida(navController, Destinations.MIS_RESERVAS) {
                    MisReservasScreen()
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

    if (mostrarNotificacionesGlobal) {
        val rolActual = SesionManager.usuarioActual?.rol
        if (rolActual == Rol.ADMIN_AREA || rolActual == Rol.AYUDANTE_AREA) {
            PersonalNotificacionesSheet(onCerrar = { mostrarNotificacionesGlobal = false })
        } else {
            EnviarNotificacionSheet(onDismiss = { mostrarNotificacionesGlobal = false })
        }
    }
}

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

private fun navegarAPestana(navController: androidx.navigation.NavController, destino: String, raiz: String) {
    if (navController.currentDestination?.route == destino) return

    navController.navigate(destino) {
        popUpTo(raiz) { inclusive = destino == raiz }
        launchSingleTop = true
    }
}