package com.example.clubdeportivo.ui.personal

import com.example.clubdeportivo.data.model.etiquetaTipoPersonal
import com.example.clubdeportivo.data.model.etiquetaTurno
import android.util.Base64
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.example.clubdeportivo.ui.components.tipoInsigniaRol
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
import com.example.clubdeportivo.util.comprimirFotoPerfil
import com.example.clubdeportivo.util.ReglasContrasena
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import com.example.clubdeportivo.ui.components.RequisitosContrasena
import com.example.clubdeportivo.ui.components.CampoBusqueda
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import com.example.clubdeportivo.ui.components.modifierReflejo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaDeporte

/** Tipos de personal que se pueden dar de alta por ahora. */
private val TIPOS_DE_ALTA = listOf("Instructor")
private val TURNOS = listOf("Matutino", "Vespertino")
private val ESTADOS = listOf("ACTIVO", "INACTIVO")
private val SIN_AREA get() = Textos.get("no_area_assigned")

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
    }.sortedBy { it.estado.uppercase() != "ACTIVO" } // estable: dentro de cada grupo se conserva el orden por nombre

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
                texto = Textos.get("add_staff"),
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
            EncabezadoPantalla(titulo = Textos.get("staff"))

            if (personal.isNotEmpty()) {
                CampoBusqueda(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = Textos.get("search_by_name_email_or_area")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                cargando && personal.isEmpty() -> FullScreenLoading()
                personal.isEmpty() -> EmptyState(Textos.get("there_is_no_staff_registered_yet"))
                visibles.isEmpty() -> EmptyState(Textos.get("nobody_matches_the_search"), icono = Icons.Default.Search)
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
            onCambiarContrasena = { persona, nueva -> viewModel.cambiarContrasena(persona, nueva) },
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
    onCambiarContrasena: (Personal, String) -> Unit,
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

    // La foto viaja dentro del perfil como data URI (ver FirebaseFotoPerfilRepository); una ruta de archivo local
    // de una versión anterior no existe en otros dispositivos, así que se descarta.
    var fotoPath by remember { mutableStateOf(personaAEditar?.fotoUrl?.takeIf { it.startsWith("data:") }) }
    val alcance = rememberCoroutineScope()
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            alcance.launch {
                val jpeg = withContext(Dispatchers.IO) { comprimirFotoPerfil(context.contentResolver, uri) }
                if (jpeg != null) {
                    fotoPath = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
                }
            }
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
        titulo = if (esEdicion) Textos.get("edit_staff") else Textos.get("add_staff"),
        onCerrar = onCerrar,
        pie = {
            (mensajeError ?: aviso)?.let {
                Text(
                    text = it,
                    color = if (mensajeError == null && it.startsWith(MENSAJE_CONTRASENA_CAMBIADA)) Exito else Peligro,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            BotonPrimario(
                texto = if (esEdicion) Textos.get("save_changes") else Textos.get("add_staff"),
                cargando = guardando,
                onClick = {
                    val error = when {
                        nombre.isBlank() -> Textos.get("enter_the_name")
                        telefono.isNotBlank() && telefono.count { it.isDigit() } < 10 -> Textos.get("the_phone_must_have_at_least")
                        rol != Rol.ADMIN && area == SIN_AREA -> Textos.get("choose_the_area_they_will_manage")
                        !esEdicion && !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> Textos.get("enter_a_valid_email")
                        !esEdicion && ReglasContrasena.primerError(contrasena) != null -> ReglasContrasena.primerError(contrasena)
                        !esEdicion && contrasena != confirmarContrasena -> Textos.get("the_passwords_don_t_match")
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
                        contentDescription = Textos.get("profile_photo"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = Textos.get("photo"), tint = TextoTenue)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                onClick = { selectorFoto.launch("image/*") }
            ) {
                Text(
                    text = if (fotoPath == null) Textos.get("upload_profile_photo") else Textos.get("change_photo"),
                    color = Marca,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        EspacioCampos()

        EtiquetaCampo(Textos.get("full_name"))
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = Textos.get("john_smith"),
            capitalizacion = KeyboardCapitalization.Words
        )

        EspacioCampos()

        EtiquetaCampo(Textos.get("phone"))
        CampoTexto(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15) },
            placeholder = "312 000 0000",
            tipoTeclado = KeyboardType.Phone
        )

        EspacioCampos()

        EtiquetaCampo(if (esEdicion) Textos.get("email_their_sign_in") else Textos.get("email_will_be_their_sign_in"))
        CampoTexto(
            value = correo,
            onValueChange = { correo = it },
            placeholder = Textos.get("email_gmail_com"),
            tipoTeclado = KeyboardType.Email,
            readOnly = esEdicion,
            imeAction = if (esEdicion) ImeAction.Done else ImeAction.Next
        )

        EspacioCampos()

        EtiquetaCampo(Textos.get("access_role"))
        PestanasPildora(
            opciones = ROLES_DE_PERSONAL.map { it.nombreLegible() },
            seleccionada = ROLES_DE_PERSONAL.indexOf(rol).coerceAtLeast(0),
            onSeleccion = { rol = ROLES_DE_PERSONAL[it] },
            margenHorizontal = 0.dp
        )
        Text(
            text = if (rol == Rol.ADMIN) Textos.get("full_access_dashboard_staff_areas_book")
            else Textos.get("only_sees_their_own_area_its"),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )

        EspacioCampos()

        EtiquetaCampo(Textos.get("lbl_estado"))
        PestanasPildora(
            opciones = listOf(Textos.get("active"), Textos.get("inactive")),
            seleccionada = ESTADOS.indexOf(estado).coerceAtLeast(0),
            onSeleccion = { estado = ESTADOS[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo(Textos.get("staff_type"))
        PestanasPildora(
            opciones = tipos.map { etiquetaTipoPersonal(it) },
            seleccionada = tipos.indexOf(tipo).coerceAtLeast(0),
            onSeleccion = { tipo = tipos[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo(Textos.get("lbl_turno"))
        PestanasPildora(
            opciones = TURNOS.map { etiquetaTurno(it) },
            seleccionada = TURNOS.indexOf(turno).coerceAtLeast(0),
            onSeleccion = { turno = TURNOS[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo(if (rol == Rol.ADMIN) Textos.get("work_area_optional") else Textos.get("area_in_charge"))
        ExposedDropdownMenuBox(expanded = areaExpandida, onExpandedChange = { areaExpandida = it }) {
            CampoTexto(
                value = etiquetaDeporte(area),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpandida) }
            )
            ExposedDropdownMenu(expanded = areaExpandida, onDismissRequest = { areaExpandida = false }) {
                opcionesArea.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(etiquetaDeporte(opcion)) },
                        onClick = { area = opcion; areaExpandida = false }
                    )
                }
            }
        }

        EspacioCampos()

        // Campos de contraseña: al crear son parte del alta; al editar sirven para que el administrador la cambie sin correo.
        val iconoVer: @Composable () -> Unit = {
            IconButton(
                onClick = { contrasenaVisible = !contrasenaVisible }
            ) {
                Icon(
                    imageVector = if (contrasenaVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (contrasenaVisible) Textos.get("hide_password") else Textos.get("show_password")
                )
            }
        }
        if (esEdicion) {
            EtiquetaCampo(Textos.get("change_password"))
            CampoTexto(
                value = contrasena,
                onValueChange = { contrasena = it },
                placeholder = Textos.get("new_password"),
                esContrasena = !contrasenaVisible,
                trailingIcon = iconoVer
            )
            if (contrasena.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                RequisitosContrasena(contrasena = contrasena)
                Spacer(modifier = Modifier.height(8.dp))
                CampoTexto(
                    value = confirmarContrasena,
                    onValueChange = { confirmarContrasena = it },
                    placeholder = Textos.get("repeat_the_new_password"),
                    esContrasena = !contrasenaVisible,
                    imeAction = ImeAction.Done,
                    isError = confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena,
                    mensajeError = if (confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena) Textos.get("the_passwords_don_t_match_2") else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                BotonSecundario(
                    texto = Textos.get("change_password_2"),
                    onClick = {
                        val error = ReglasContrasena.primerError(contrasena)
                            ?: if (contrasena != confirmarContrasena) Textos.get("the_passwords_don_t_match") else null
                        mensajeError = error
                        if (error == null && personaAEditar != null) {
                            onCambiarContrasena(personaAEditar, contrasena)
                            contrasena = ""
                            confirmarContrasena = ""
                        }
                    }
                )
            }
            Text(
                text = Textos.get("it_changes_instantly_and_no_email"),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            EtiquetaCampo(Textos.get("password"))
            CampoTexto(
                value = contrasena,
                onValueChange = { contrasena = it },
                placeholder = Textos.get("create_a_strong_password"),
                esContrasena = !contrasenaVisible,
                trailingIcon = {
                    IconButton(
                        onClick = { contrasenaVisible = !contrasenaVisible }
                    ) {
                        Icon(
                            imageVector = if (contrasenaVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (contrasenaVisible) Textos.get("hide_password") else Textos.get("show_password")
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            RequisitosContrasena(contrasena = contrasena)

            EspacioCampos()

            EtiquetaCampo(Textos.get("confirm_password"))
            CampoTexto(
                value = confirmarContrasena,
                onValueChange = { confirmarContrasena = it },
                placeholder = Textos.get("repeat_the_password"),
                esContrasena = !contrasenaVisible,
                imeAction = ImeAction.Done,
                isError = confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena,
                mensajeError = if (confirmarContrasena.isNotEmpty() && confirmarContrasena != contrasena) Textos.get("the_passwords_don_t_match_2") else null
            )
        }

    }
}

@Composable
private fun PersonalCard(persona: Personal, onEditClick: () -> Unit) {
    val fotoPerfil = persona.fotoUrl?.takeIf { it.startsWith("data:") }
    val activo = persona.estado.uppercase() == "ACTIVO"

    val detalle = listOf(etiquetaTipoPersonal(persona.tipoPersonal), etiquetaTurno(persona.turno)).filter { it.isNotBlank() }.joinToString(" · ")

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
                            contentDescription = Textos.get("photo_of", persona.nombre),
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
                            text = Textos.get("joined", Fechas.legible(persona.fechaIngreso)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }

                Insignia(
                    texto = if (activo) Textos.get("active") else Textos.get("inactive"),
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
                        texto = persona.areaTrabajo?.let(::etiquetaDeporte) ?: SIN_AREA,
                        tipo = TipoInsignia.NEUTRO,
                        icono = Icons.Default.Place,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Insignia(
                        texto = persona.rol.nombreLegible(),
                        tipo = tipoInsigniaRol(persona.rol),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                TextButton(
                    onClick = onEditClick
                ) {
                    Text(text = Textos.get("edit"), color = Marca, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}