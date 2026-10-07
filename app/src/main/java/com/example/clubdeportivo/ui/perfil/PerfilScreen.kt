package com.example.clubdeportivo.ui.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.InitialsAvatar
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.FotoPerfilManager

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val usuario by viewModel.usuario.observeAsState()
    val guardando by viewModel.guardando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val context = LocalContext.current

    var fotoPerfil by remember(usuario?.id) {
        mutableStateOf(
            usuario?.id?.let { id ->
                FotoPerfilManager.obtenerFoto(context, id)
            }
        )
    }

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && usuario != null) {
            val nuevaFoto = FotoPerfilManager.guardarFoto(
                context = context,
                uri = uri,
                usuarioId = usuario!!.id
            )
            fotoPerfil = nuevaFoto
            SesionManager.notificarCambioFoto()
        }
    }

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var confirmandoSalida by remember { mutableStateOf(false) }

    LaunchedEffect(usuario) {
        nombre = usuario?.nombre.orEmpty()
        correo = usuario?.correo.orEmpty()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MargenPantalla, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (fotoPerfil != null) {
                    AsyncImage(
                        model = fotoPerfil,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable { selectorFoto.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.clickable { selectorFoto.launch("image/*") }) {
                        InitialsAvatar(nombre = usuario?.nombre ?: "?", size = 96.dp)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectorFoto.launch("image/*") }) {
                    Text(
                        text = if (fotoPerfil == null) "Agregar foto" else "Cambiar foto",
                        color = Marca,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (fotoPerfil != null) {
                    TextButton(
                        onClick = {
                            usuario?.let { usuarioActual ->
                                FotoPerfilManager.eliminarFoto(context = context, usuarioId = usuarioActual.id)
                                fotoPerfil = null
                                SesionManager.notificarCambioFoto()
                            }
                        }
                    ) {
                        Text(text = "Quitar foto", color = Peligro, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = usuario?.nombre ?: "Mi perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(8.dp))

            usuario?.rol?.let { Insignia(texto = it.nombreLegible(), tipo = TipoInsignia.MARCA) }

            Spacer(modifier = Modifier.height(24.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Información personal",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Actualiza los datos de tu perfil",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    EtiquetaCampo("NOMBRE")
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = "Nombre",
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    EspacioCampos()

                    EtiquetaCampo("CORREO ELECTRÓNICO")
                    CampoTexto(
                        value = correo,
                        onValueChange = {},
                        readOnly = true,
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Email, contentDescription = null) },
                        mensajeError = "Es tu usuario de acceso y no se puede cambiar."
                    )

                    EspacioCampos()

                    EtiquetaCampo("ROL")
                    CampoTexto(
                        value = usuario?.rol?.nombreLegible() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    BotonPrimario(
                        texto = "Guardar cambios",
                        cargando = guardando,
                        onClick = { viewModel.actualizarPerfil(nombre = nombre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sesión",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Cierra tu sesión en este dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BotonSecundario(
                        texto = "Cerrar sesión",
                        colorTexto = Peligro,
                        onClick = { confirmandoSalida = true }
                    )
                }
            }
        }
    }

    if (confirmandoSalida) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            mensaje = "¿Quieres cerrar tu sesión en este dispositivo?",
            textoConfirmar = "Cerrar sesión",
            textoCancelar = "Cancelar",
            onConfirmar = {
                confirmandoSalida = false
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            onCancelar = { confirmandoSalida = false }
        )
    }
}
