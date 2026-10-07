package com.example.clubdeportivo.ui.personal

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.model.DatosPersonal
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.data.model.ROLES_DE_PERSONAL
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.TextoTenue
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.FotoPerfilManager
import com.example.clubdeportivo.util.ReglasContrasena
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import com.example.clubdeportivo.ui.components.RequisitosContrasena
import com.example.clubdeportivo.ui.components.CampoBusqueda
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.rememberLazyListState

/** Tipos de personal que se pueden dar de alta por ahora. */
private val TIPOS_DE_ALTA = listOf("Instructor")
private val TURNOS = listOf("Matutino", "Vespertino")
private val ESTADOS = listOf("ACTIVO", "INACTIVO")
private const val SIN_AREA = "Sin área asignada"

@Composable
fun PersonalScreen(viewModel: PersonalViewModel = viewModel()) {
    val personal by viewModel.personal.observeAsState(emptyList())
    val areas by viewModel.areas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(true)
    val guardando by viewModel.guardando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val guardadoExitoso by viewModel.guardadoExitoso.observeAsState(false)
    val avisoFormulario by viewModel.avisoFormulario.observeAsState()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Personal?>(null) }
    var busqueda by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val visibles = personal.filter {
        val texto = busqueda.trim()
        texto.isBlank() ||
            it.nombre.contains(texto, ignoreCase = true) ||
            it.email.contains(texto, ignoreCase = true) ||
            (it.areaTrabajo ?: "").contains(texto, ignoreCase = true)
    }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }
    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            mostrarFormulario = false
            viewModel.limpiarAvisoFormulario()
            viewModel.onGuardadoProcesado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FondoApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            BotonFlotanteAgregar(
                texto = "Agregar personal",
                visible = listState.botonFlotanteVisible(),
                onClick = {
                    enEdicion = null
                    mostrarFormulario = true
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MargenPantalla)
        ) {
            EncabezadoPantalla(titulo = "Personal")

            if (personal.isNotEmpty()) {
                CampoBusqueda(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = "Buscar por nombre, correo o área"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                cargando && personal.isEmpty() -> FullScreenLoading()
                personal.isEmpty() -> EmptyState("Todavía no hay personal registrado.")
                visibles.isEmpty() -> EmptyState("Nadie coincide con la búsqueda.", icono = Icons.Default.Search)
                else -> LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(visibles, key = { it.id }) { persona ->
                        PersonalCard(
                            persona = persona,
                            onEditClick = {
                                enEdicion = persona
                                mostrarFormulario = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioPersonalDialog(
            areas = areas,
            personaAEditar = enEdicion,
            guardando = guardando,
            aviso = avisoFormulario,
            onGuardar = { datos -> viewModel.guardar(enEdicion, datos) },
            onRestablecerContrasena = { viewModel.enviarRestablecimiento(it) },
            onCerrar = {
                mostrarFormulario = false
                viewModel.limpiarAvisoFormulario()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPersonalDialog(
    areas: List<String>,
    personaAEditar: Personal?,
    guardando: Boolean,
    aviso: String?,
    onGuardar: (DatosPersonal) -> Unit,
    onRestablecerContrasena: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val esEdicion = personaAEditar != null

    var nombre by remember { mutableStateOf(personaAEditar?.nombre ?: "") }
    var telefono by remember { mutableStateOf(personaAEditar?.telefono ?: "") }
    var correo by remember { mutableStateOf(personaAEditar?.email ?: "") }
    var estado by remember { mutableStateOf(personaAEditar?.estado ?: "ACTIVO") }
    var rol by remember {
        mutableStateOf(
            personaAEditar?.rol?.let { if (it == Rol.SUPERADMIN) Rol.ADMIN else if (it in ROLES_DE_PERSONAL) it else Rol.AYUDANTE_AREA }
                ?: Rol.AYUDANTE_AREA
        )
    }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val idFoto = remember { personaAEditar?.id ?: java.util.UUID.randomUUID().toString() }
    var fotoPath by remember {
        mutableStateOf<String?>(
            personaAEditar?.let { FotoPerfilManager.obtenerFoto(context, it.id)?.absolutePath ?: it.fotoUrl }
        )
    }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoPath = FotoPerfilManager.guardarFoto(context = context, uri = uri, usuarioId = idFoto)?.absolutePath
        }
    }

    val tipos = (TIPOS_DE_ALTA + listOfNotNull(personaAEditar?.tipoPersonal?.takeIf { it.isNotBlank() })).distinct()
    var tipo by remember { mutableStateOf(personaAEditar?.tipoPersonal?.takeIf { it.isNotBlank() } ?: tipos[0]) }
    var turno by remember { mutableStateOf(personaAEditar?.turno?.takeIf { it in TURNOS } ?: TURNOS[0]) }

    val opcionesArea = listOf(SIN_AREA) + (areas + listOfNotNull(personaAEditar?.areaTrabajo)).distinct()
    var area by remember { mutableStateOf(personaAEditar?.areaTrabajo ?: SIN_AREA) }
    var areaExpandida by remember { mutableStateOf(false) }

    var contrasenaVisible by remember { mutableStateOf(false) }

    DialogoFormulario(
        titulo = if (esEdicion) "Editar personal" else "Agregar personal",
        onCerrar = onCerrar,
        pie = {
            (mensajeError ?: aviso)?.let {
                Text(
                    text = it,
                    color = if (mensajeError == null && it.startsWith("Enviamos")) Exito else Peligro,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            BotonPrimario(
                texto = if (esEdicion) "Guardar cambios" else "Agregar personal",
                cargando = guardando,
                onClick = {
                    val error = when {
                        nombre.isBlank() -> "Escribe el nombre."
                        telefono.isNotBlank() && telefono.count { it.isDigit() } < 10 -> "El teléfono debe tener al menos 10 dígitos."
                        !esEdicion && !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> "Escribe un correo válido."
                        !esEdicion && ReglasContrasena.primerError(contrasena) != null -> ReglasContrasena.primerError(contrasena)
                        !esEdicion && contrasena != confirmarContrasena -> "Las contraseñas no coinciden."
                        else -> null
                    }
                    mensajeError = error
                    if (error == null) {
                        onGuardar(
                            DatosPersonal(
                                nombre = nombre,
                                email = correo,
                                telefono = telefono,
                                estado = estado,
                                rol = rol,
                                tipoPersonal = tipo,
                                turno = turno,
                                areaTrabajo = area.takeIf { it != SIN_AREA },
                                fotoUrl = fotoPath,
                                contrasena = contrasena
                            )
                        )
                    }
                }
            )
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(FondoApp)
                    .clickable { selectorFoto.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (fotoPath != null) {
                    AsyncImage(
                        model = fotoPath,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Foto", tint = TextoTenue)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { selectorFoto.launch("image/*") }) {
                Text(
                    text = if (fotoPath == null) "Subir foto de perfil" else "Cambiar foto",
                    color = Marca,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        EspacioCampos()

        EtiquetaCampo("NOMBRE COMPLETO")
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = "Juan Pérez",
            capitalizacion = KeyboardCapitalization.Words
        )

        EspacioCampos()

        EtiquetaCampo("TELÉFONO")
        CampoTexto(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15) },
            placeholder = "312 000 0000",
            tipoTeclado = KeyboardType.Phone
        )

        EspacioCampos()

        EtiquetaCampo(if (esEdicion) "CORREO (ES SU USUARIO DE ACCESO)" else "CORREO (SERÁ SU USUARIO DE ACCESO)")
        CampoTexto(
            value = correo,
            onValueChange = { correo = it },
            placeholder = "correo@gmail.com",
            tipoTeclado = KeyboardType.Email,
            readOnly = esEdicion,
            imeAction = if (esEdicion) ImeAction.Done else ImeAction.Next
        )

        EspacioCampos()

        EtiquetaCampo("ROL DE ACCESO")
        PestanasPildora(
            opciones = ROLES_DE_PERSONAL.map { it.nombreLegible() },
            seleccionada = ROLES_DE_PERSONAL.indexOf(rol).coerceAtLeast(0),
            onSeleccion = { rol = ROLES_DE_PERSONAL[it] },
            margenHorizontal = 0.dp
        )
        Text(
            text = if (rol == Rol.ADMIN) "Acceso total: dashboard, personal, áreas, reservas, membresías y precios."
            else "Ve las reservas de su área, las áreas y las membresías; no cambia precios ni administra personal.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )

        EspacioCampos()

        EtiquetaCampo("ESTADO")
        PestanasPildora(
            opciones = listOf("Activo", "Inactivo"),
            seleccionada = ESTADOS.indexOf(estado).coerceAtLeast(0),
            onSeleccion = { estado = ESTADOS[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("TIPO DE PERSONAL")
        PestanasPildora(
            opciones = tipos,
            seleccionada = tipos.indexOf(tipo).coerceAtLeast(0),
            onSeleccion = { tipo = tipos[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("TURNO")
        PestanasPildora(
            opciones = TURNOS,
            seleccionada = TURNOS.indexOf(turno).coerceAtLeast(0),
            onSeleccion = { turno = TURNOS[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("ÁREA DE TRABAJO")
        ExposedDropdownMenuBox(expanded = areaExpandida, onExpandedChange = { areaExpandida = it }) {
            CampoTexto(
                value = area,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpandida) }
            )
            ExposedDropdownMenu(expanded = areaExpandida, onDismissRequest = { areaExpandida = false }) {
                opcionesArea.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = { area = opcion; areaExpandida = false }
                    )
                }
            }
        }

        EspacioCampos()

        if (esEdicion) {
            EtiquetaCampo("CONTRASEÑA")
            BotonSecundario(
                texto = "Enviar correo para cambiarla",
                onClick = { onRestablecerContrasena(correo) }
            )
        } else {
            EtiquetaCampo("CONTRASEÑA")
            CampoTexto(
                value = contrasena,
                onValueChange = { contrasena = it },
                placeholder = "Crea una contraseña segura",
                esContrasena = !contrasenaVisible,
                trailingIcon = {
                    IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                        Icon(
                            imageVector = if (contrasenaVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            RequisitosContrasena(contrasena = contrasena)

            EspacioCampos()

            EtiquetaCampo("CONFIRMAR CONTRASEÑA")
            CampoTexto(
                value = confirmarContrasena,
                onValueChange = { confirmarContrasena = it },
                placeholder = "Repite la contraseña",
                esContrasena = !contrasenaVisible,
                imeAction = ImeAction.Done,
                isError = confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena,
                mensajeError = if (confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena) "Las contraseñas no coinciden" else null
            )
        }

    }
}

@Composable
private fun PersonalCard(persona: Personal, onEditClick: () -> Unit) {
    val context = LocalContext.current
    val fotoPerfil = remember(persona.id) {
        FotoPerfilManager.obtenerFoto(context, persona.id)?.absolutePath ?: persona.fotoUrl
    }
    val activo = persona.estado.uppercase() == "ACTIVO"

    val detalle = listOf(persona.tipoPersonal, persona.turno).filter { it.isNotBlank() }.joinToString(" · ")

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MarcaSuave),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfil != null) {
                        AsyncImage(
                            model = fotoPerfil,
                            contentDescription = "Foto de ${persona.nombre}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = persona.nombre.take(1).uppercase().ifEmpty { "?" },
                            color = Marca,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = persona.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detalle.isNotEmpty()) {
                        Text(text = detalle, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                    }
                    Text(
                        text = persona.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (persona.telefono.isNotBlank()) {
                        Text(text = persona.telefono, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    if (persona.fechaIngreso.isNotBlank()) {
                        Text(
                            text = "Ingreso: ${Fechas.legible(persona.fechaIngreso)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }

                Insignia(
                    texto = if (activo) "Activo" else "Inactivo",
                    tipo = if (activo) TipoInsignia.EXITO else TipoInsignia.ALERTA
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Insignia(
                        texto = persona.areaTrabajo ?: SIN_AREA,
                        tipo = TipoInsignia.NEUTRO,
                        icono = Icons.Default.Place,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Insignia(
                        texto = persona.rol.nombreLegible(),
                        tipo = TipoInsignia.NEUTRO,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                TextButton(onClick = onEditClick) {
                    Text(text = "Editar", color = Marca, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}