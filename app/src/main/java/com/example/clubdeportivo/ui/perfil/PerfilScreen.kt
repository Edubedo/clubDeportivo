package com.example.clubdeportivo.ui.perfil

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.io.File
import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.SelectorIdioma
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
import com.example.clubdeportivo.ui.components.tipoInsigniaRol
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.ModoTema
import com.example.clubdeportivo.ui.theme.PreferenciaTema
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val usuario by viewModel.usuario.observeAsState()
    val guardando by viewModel.guardando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val subiendoFoto by viewModel.subiendoFoto.observeAsState(false)
    val context = LocalContext.current
    val fotoPerfil = usuario?.fotoUrl

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.cambiarFoto(context.contentResolver, uri)
    }

    var mostrarOpcionesFoto by remember { mutableStateOf(false) }
    // rememberSaveable: Android puede cerrar la app mientras la cámara está abierta y hay que recordar dónde quedó la foto.
    var uriCamara by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { tomada ->
        val uri = uriCamara
        if (tomada && uri != null) viewModel.cambiarFoto(context.contentResolver, uri)
    }
    val abrirCamara = {
        try {
            val archivo = File(context.cacheDir, "fotos").apply { mkdirs() }.let { File(it, "perfil_camara.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            uriCamara = uri
            camara.launch(uri)
        } catch (e: Exception) {
            viewModel.mostrarMensaje(context.getString(R.string.perfil_camara_error))
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
                        contentDescription = stringResource(R.string.foto_perfil),
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !subiendoFoto) { mostrarOpcionesFoto = true },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.clickable(enabled = !subiendoFoto) { mostrarOpcionesFoto = true }) {
                        InitialsAvatar(nombre = usuario?.nombre ?: "?", size = 96.dp)
                    }
                }
                if (subiendoFoto) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Marca, strokeWidth = 3.dp)
                }
            }

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { selectorFoto.launch("image/*") }
                ) {
                    Text(
                        text = if (fotoPerfil == null) stringResource(R.string.agregar_foto) else stringResource(R.string.cambiar_foto),
                        color = Marca,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (fotoPerfil != null) {
                    TextButton(enabled = !subiendoFoto, onClick = { viewModel.quitarFoto() }) {
                        Text(text = stringResource(R.string.quitar_foto), color = Peligro, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = usuario?.nombre ?: stringResource(R.string.perfil_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(8.dp))

            usuario?.rol?.let { Insignia(texto = it.nombreLegible(), tipo = tipoInsigniaRol(it)) }

            Spacer(modifier = Modifier.height(24.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.perfil_info_personal),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = stringResource(R.string.perfil_actualiza_datos),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    EtiquetaCampo(stringResource(R.string.perfil_nombre_etiqueta))
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = stringResource(R.string.perfil_nombre_placeholder),
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    EspacioCampos()

                    EtiquetaCampo(stringResource(R.string.perfil_correo_etiqueta))
                    CampoTexto(
                        value = correo,
                        onValueChange = {},
                        readOnly = true,
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Email, contentDescription = null) },
                        mensajeError = stringResource(R.string.perfil_correo_fijo)
                    )

                    EspacioCampos()

                    EtiquetaCampo(stringResource(R.string.perfil_rol_etiqueta))
                    CampoTexto(
                        value = usuario?.rol?.nombreLegible() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    BotonPrimario(
                        texto = stringResource(R.string.guardar_cambios),
                        cargando = guardando,
                        onClick = { viewModel.actualizarPerfil(nombre = nombre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                val contexto = LocalContext.current
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.apariencia_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )
                    Text(
                        text = stringResource(R.string.apariencia_descripcion),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    PestanasPildora(
                        opciones = ModoTema.entries.map { stringResource(it.etiqueta) },
                        seleccionada = PreferenciaTema.modo.ordinal,
                        onSeleccion = { PreferenciaTema.elegir(contexto, ModoTema.entries[it]) },
                        margenHorizontal = 0.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.idioma_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )
                    Text(
                        text = stringResource(R.string.idioma_descripcion),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    SelectorIdioma()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaNotificaciones()

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.sesion_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = stringResource(R.string.sesion_descripcion),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BotonSecundario(
                        texto = stringResource(R.string.logout_button),
                        colorTexto = Peligro,
                        conReflejo = false,
                        onClick = { confirmandoSalida = true }
                    )
                }
            }
        }
    }

    if (mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesFoto = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Superficie,
            title = { Text(stringResource(R.string.foto_perfil), style = MaterialTheme.typography.titleLarge, color = TextoPrincipal) },
            text = {
                Column {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            mostrarOpcionesFoto = false
                            abrirCamara()
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Marca)
                        Text(
                            stringResource(R.string.tomar_foto),
                            color = TextoPrincipal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            mostrarOpcionesFoto = false
                            selectorFoto.launch("image/*")
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Marca)
                        Text(
                            stringResource(R.string.elegir_galeria),
                            color = TextoPrincipal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarOpcionesFoto = false }) {
                    Text(stringResource(R.string.cancelar), color = TextoSecundario, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (confirmandoSalida) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.logout_button),
            mensaje = stringResource(R.string.logout_confirmar),
            textoConfirmar = stringResource(R.string.logout_button),
            textoCancelar = stringResource(R.string.cancelar),
            conReflejo = false,
            onConfirmar = {
                confirmandoSalida = false
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            onCancelar = { confirmandoSalida = false }
        )
    }
}
