package com.example.clubdeportivo.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.admin.home.AdminHomeScreen
import com.example.clubdeportivo.ui.avisos.AvisosSheet
import com.example.clubdeportivo.ui.avisos.AvisosViewModel
import com.example.clubdeportivo.ui.areas.AreasDetailScreen
import com.example.clubdeportivo.ui.areas.AreasScreen
import com.example.clubdeportivo.ui.inventario.InventarioScreen
import com.example.clubdeportivo.ui.login.LoginScreen
import com.example.clubdeportivo.ui.membresia.MembresiaScreen
import com.example.clubdeportivo.ui.navigation.Destinations
import com.example.clubdeportivo.ui.navigation.menuPara
import com.example.clubdeportivo.ui.navigation.rutaInicial
import com.example.clubdeportivo.ui.navigation.rutaPermitida
import com.example.clubdeportivo.ui.perfil.PerfilScreen
import com.example.clubdeportivo.ui.personal.PersonalScreen
import com.example.clubdeportivo.ui.registro.RegistroScreen
import com.example.clubdeportivo.ui.reservas.MisReservasScreen
import com.example.clubdeportivo.ui.reservas.ReservasEncargadoScreen
import com.example.clubdeportivo.ui.reservas.ReservasScreen
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.TextoTenue
import com.example.clubdeportivo.util.FotoPerfilManager

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

    // La cabecera azul del login queda detrás de la barra de estado, así que ahí los íconos van claros.
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = vista.context.buscarActividad()?.window
            if (ventana != null) {
                WindowCompat.getInsetsController(ventana, vista).isAppearanceLightStatusBars = !enPantallaDeLogin
            }
        }
    }

    // Avisos del club: todos los roles tienen su bandeja; la campana marca cuántos faltan por leer.
    val avisos: AvisosViewModel = viewModel()
    var mostrarAvisos by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(usuario?.id) { avisos.cargar() }
    LaunchedEffect(avisos.mensaje) {
        avisos.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            avisos.onMensajeMostrado()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = if (enPantallaDeLogin) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!enPantallaDeLogin) {
                Column(modifier = Modifier.background(Superficie)) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Marca),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SportsSoccer,
                                        contentDescription = null,
                                        tint = SobreMarca,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = "Athletic Club",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextoPrincipal,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            usuario?.nombre?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() },
                                            rol?.nombreLegible()
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoSecundario,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                IconButton(onClick = { mostrarAvisos = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (avisos.sinLeer > 0) {
                                                Badge(containerColor = Marca, contentColor = SobreMarca) {
                                                    Text(if (avisos.sinLeer > 9) "9+" else avisos.sinLeer.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = if (avisos.sinLeer > 0) "Avisos, ${avisos.sinLeer} sin leer" else "Avisos",
                                            tint = TextoSecundario
                                        )
                                    }
                                }
                                AvatarUsuario(
                                    usuarioId = usuario?.id,
                                    nombre = usuario?.nombre.orEmpty(),
                                    onClick = {
                                        if (menu.any { it.route == Destinations.PERFIL }) {
                                            navegarAPestana(navController, Destinations.PERFIL, menu.first().route)
                                        } else {
                                            navController.navigate(Destinations.PERFIL) { launchSingleTop = true }
                                        }
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Superficie,
                            scrolledContainerColor = Superficie,
                            titleContentColor = TextoPrincipal
                        )
                    )
                    HorizontalDivider(color = Borde)
                }
            }
        },
        bottomBar = {
            if (!enPantallaDeLogin) {
                Column(modifier = Modifier.background(Superficie)) {
                    HorizontalDivider(color = Borde)
                    NavigationBar(
                        containerColor = Superficie,
                        tonalElevation = 0.dp
                    ) {
                        menu.forEach { item ->
                            val seleccionado = currentRoute == item.route
                            NavigationBarItem(
                                selected = seleccionado,
                                onClick = { navegarAPestana(navController, item.route, menu.first().route) },
                                icon = {
                                    Icon(
                                        imageVector = if (seleccionado) item.iconSelected else item.iconUnselected,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Marca,
                                    selectedTextColor = Marca,
                                    unselectedIconColor = TextoTenue,
                                    unselectedTextColor = TextoTenue,
                                    indicatorColor = MarcaSuave
                                )
                            )
                        }
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
                    AdminHomeScreen()
                }
            }
            composable(Destinations.PERSONAL) {
                RutaProtegida(navController, Destinations.PERSONAL) { PersonalScreen() }
            }
            composable(Destinations.AREAS) {
                RutaProtegida(navController, Destinations.AREAS) {
                    // Un encargado solo ve su área (el inventario tiene su propia pestaña en su menú).
                    if (rol?.esEncargado() == true) AreasScreen() else AreasDetailScreen()
                }
            }
            composable(Destinations.INVENTARIO) {
                RutaProtegida(navController, Destinations.INVENTARIO) { InventarioScreen() }
            }
            composable(Destinations.RESERVAS) {
                if (SesionManager.usuarioActual?.rol?.esEncargado() == true) ReservasEncargadoScreen() else ReservasScreen()
            }
            composable(Destinations.MIS_RESERVAS) {
                RutaProtegida(navController, Destinations.MIS_RESERVAS) {
                    MisReservasScreen()
                }
            }
            composable(Destinations.MEMBRESIA) {
                RutaProtegida(navController, Destinations.MEMBRESIA) { MembresiaScreen() }
            }
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

    if (mostrarAvisos) {
        AvisosSheet(onCerrar = { mostrarAvisos = false }, viewModel = avisos)
    }
}

/** Foto de perfil (si la hay) o inicial del nombre, en un círculo que lleva al perfil. */
@Composable
private fun AvatarUsuario(usuarioId: String?, nombre: String, onClick: () -> Unit) {
    val contexto = LocalContext.current
    val foto = remember(usuarioId, SesionManager.versionFotoPerfil) {
        usuarioId?.let { FotoPerfilManager.obtenerFoto(contexto, it) }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MarcaSuave)
            .clickable(onClickLabel = "Abrir perfil", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (foto != null) {
            AsyncImage(
                model = foto,
                contentDescription = "Foto de perfil",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = nombre.trim().firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = Marca,
                fontSize = 14.sp
            )
        }
    }
}

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
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
