package com.example.clubdeportivo.ui.perfil

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
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
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.SelectorIdioma
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.components.tipoInsigniaRol
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.ModoTema
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PreferenciaTema
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.tr
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.cambiarFoto(context.contentResolver, uri)
    }
    var uriCamara by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { tomada ->
        val uri = uriCamara
        if (tomada && uri != null) viewModel.cambiarFoto(context.contentResolver, uri)
    }

    var showImageSourceOptions by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var mostrarFotoCompleta by remember { mutableStateOf(false) }

    fun abrirCamara() {
        try {
            val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
            val archivo = File(carpeta, "perfil_camara.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            uriCamara = uri
            camara.launch(uri)
        } catch (e: Exception) {
            viewModel.mostrarMensaje(tr("perfil_camara_error"))
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
            AvatarFotoPerfil(
                fotoUrl = fotoPerfil,
                nombre = usuario?.nombre ?: "?",
                cargando = subiendoFoto,
                onVerFoto = { mostrarFotoCompleta = true },
                onEditarFoto = { showImageSourceOptions = true }
            )

            Text(
                text = usuario?.nombre ?: tr("perfil_title"),
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(8.dp))

            usuario?.rol?.let { Insignia(texto = it.nombreLegible(), tipo = tipoInsigniaRol(it)) }

            Spacer(modifier = Modifier.height(24.dp))

            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = tr("perfil_info_personal"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = tr("perfil_actualiza_datos"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    EtiquetaCampo(tr("perfil_nombre_etiqueta"))
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = tr("perfil_nombre_placeholder"),
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    EspacioCampos()

                    EtiquetaCampo(tr("perfil_correo_etiqueta"))
                    CampoTexto(
                        value = correo,
                        onValueChange = {},
                        readOnly = true,
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Email, contentDescription = null) },
                        mensajeError = tr("perfil_correo_fijo")
                    )

                    EspacioCampos()

                    EtiquetaCampo(tr("perfil_rol_etiqueta"))
                    CampoTexto(
                        value = usuario?.rol?.nombreLegible() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    BotonPrimario(
                        texto = tr("guardar_cambios"),
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
                        text = tr("apariencia_titulo"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )
                    Text(
                        text = tr("apariencia_descripcion"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    PestanasPildora(
                        opciones = ModoTema.entries.map { tr(it.etiqueta) },
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
                        text = tr("idioma_titulo"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )
                    Text(
                        text = tr("idioma_descripcion"),
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
                        text = tr("sesion_titulo"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )

                    Text(
                        text = tr("sesion_descripcion"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BotonSecundario(
                        texto = tr("logout_button"),
                        colorTexto = Peligro,
                        conReflejo = false,
                        onClick = { confirmandoSalida = true }
                    )
                }
            }
        }
    }

    if (showImageSourceOptions) {
        DialogoTarjetaPerfil(
            titulo = tr("perfil_foto_dialogo_titulo"),
            onCerrar = { if (!subiendoFoto) showImageSourceOptions = false }
        ) {
            TextButton(
                enabled = !subiendoFoto,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showImageSourceOptions = false
                    abrirCamara()
                }
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Marca)
                Text(
                    tr("tomar_foto"),
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
            TextButton(
                enabled = !subiendoFoto,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showImageSourceOptions = false
                    selectorFoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Marca)
                Text(
                    tr("elegir_galeria"),
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
            if (fotoPerfil != null) {
                TextButton(
                    enabled = !subiendoFoto,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showImageSourceOptions = false
                        showDeleteConfirmDialog = true
                    }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Peligro)
                    Text(
                        tr("quitar_foto"),
                        color = Peligro,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        DialogoTarjetaPerfil(
            titulo = tr("perfil_confirmar_eliminar_foto_titulo"),
            onCerrar = { if (!subiendoFoto) showDeleteConfirmDialog = false },
            pie = {
                BotonPrimario(
                    texto = tr("perfil_confirmar_eliminar_foto"),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !subiendoFoto,
                    cargando = subiendoFoto,
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.quitarFoto()
                    }
                )
                TextButton(
                    enabled = !subiendoFoto,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(tr("cancelar"), color = TextoSecundario, fontWeight = FontWeight.SemiBold)
                }
            }
        ) {
            Text(
                tr("perfil_eliminar_foto_mensaje"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
    }

    if (mostrarFotoCompleta && fotoPerfil != null) {
        BackHandler(onBack = { mostrarFotoCompleta = false })
        Dialog(
            onDismissRequest = { mostrarFotoCompleta = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { mostrarFotoCompleta = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fotoPerfil,
                    contentDescription = tr("foto_perfil"),
                    modifier = Modifier.fillMaxSize().padding(24.dp).clickable { },
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { mostrarFotoCompleta = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("cerrar"),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (confirmandoSalida) {
        DialogoConfirmacion(
            titulo = tr("logout_button"),
            mensaje = tr("logout_confirmar"),
            textoConfirmar = tr("logout_button"),
            textoCancelar = tr("cancelar"),
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

@Composable
private fun DialogoTarjetaPerfil(
    titulo: String,
    onCerrar: () -> Unit,
    pie: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            color = Superficie
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(onClick = onCerrar) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("cerrar"),
                            tint = TextoSecundario
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    content()
                }
                if (pie != null) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 16.dp
                        )
                    ) {
                        pie()
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarFotoPerfil(
    fotoUrl: String?,
    nombre: String,
    cargando: Boolean,
    onVerFoto: () -> Unit,
    onEditarFoto: () -> Unit
) {
    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        if (fotoUrl != null) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = tr("foto_perfil"),
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !cargando, onClick = onVerFoto),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.clickable(enabled = !cargando, onClick = onEditarFoto)) {
                InitialsAvatar(nombre = nombre, size = 96.dp)
            }
        }

        IconButton(
            onClick = onEditarFoto,
            enabled = !cargando,
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Surface(shape = CircleShape, color = Marca, shadowElevation = 3.dp) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = tr("cambiar_foto"),
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
        }

        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Marca,
                strokeWidth = 3.dp
            )
        }
    }
}
