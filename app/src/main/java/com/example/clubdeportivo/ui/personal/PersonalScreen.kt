package com.example.clubdeportivo.ui.personal

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.model.DatosPersonal
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.data.model.ROLES_DE_PERSONAL
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca
import com.example.clubdeportivo.util.FotoPerfilManager
import java.text.SimpleDateFormat
import java.util.*

private val TIPOS_DE_ALTA = listOf("Instructor")
private val TURNOS = listOf("Matutino", "Vespertino")
private val ESTADOS = listOf("ACTIVO", "INACTIVO")
private const val SIN_AREA = "Sin área asignada"

private val TextoOscuro = Color(0xFF111827)
private val TextoGris = Color(0xFF64748B)

@Composable
fun PersonalScreen(
    onIrPerfil: () -> Unit = {},
    viewModel: PersonalViewModel = viewModel()
) {
    val personal by viewModel.personal.observeAsState(emptyList())
    val areas by viewModel.areas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(true)
    val guardando by viewModel.guardando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val guardadoExitoso by viewModel.guardadoExitoso.observeAsState(false)
    val avisoFormulario by viewModel.avisoFormulario.observeAsState()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Personal?>(null) }
    var mostrarNotificaciones by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        containerColor = Color(0xFFF9F9F9),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    enEdicion = null
                    mostrarFormulario = true
                },
                containerColor = VerdeMarca,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Personal")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Personal", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )

                // Botón de Notificaciones con el diseño azulito exacto del Dashboard
                Surface(
                    onClick = { mostrarNotificaciones = true },
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📢", fontSize = 14.sp)
                        Text(
                            text = "Notif.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                cargando && personal.isEmpty() -> FullScreenLoading()
                personal.isEmpty() -> EmptyState("Todavía no hay personal registrado.")
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(personal, key = { it.id }) { persona ->
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

    if (mostrarNotificaciones) {
        HojaNotificacionesPersonal(
            onCerrar = { mostrarNotificaciones = false }
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

    val idFoto = remember { personaAEditar?.id ?: UUID.randomUUID().toString() }
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

    DialogoFormulario(
        titulo = if (esEdicion) "Editar personal" else "Agregar personal",
        onCerrar = onCerrar
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
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
                    Icon(Icons.Default.Person, contentDescription = "Foto", tint = Color(0xFF94A3B8))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { selectorFoto.launch("image/*") }) {
                Text(
                    text = if (fotoPath == null) "Subir foto de perfil" else "Cambiar foto",
                    color = VerdeMarca,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        EspacioCampos()

        EtiquetaCampo("NOMBRE COMPLETO")
        CampoTexto(value = nombre, onValueChange = { nombre = it }, placeholder = "Juan Pérez")

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
            readOnly = esEdicion
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
            text = if (rol == Rol.ADMIN) "Acceso total: dashboard, personal, áreas, reservas y membresías."
            else "Acceso a reservas, áreas y membresías.",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
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
                placeholder = "Mín. 8 caracteres, 1 mayúscula, 1 número",
                esContrasena = true
            )

            EspacioCampos()

            EtiquetaCampo("CONFIRMAR CONTRASEÑA")
            CampoTexto(
                value = confirmarContrasena,
                onValueChange = { confirmarContrasena = it },
                placeholder = "********",
                esContrasena = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        (mensajeError ?: aviso)?.let {
            Text(
                text = it,
                color = if (mensajeError == null && it.startsWith("Enviamos")) VerdeMarca else MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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
                    !esEdicion && contrasena.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                    !esEdicion && contrasena.none { it.isUpperCase() } -> "La contraseña debe incluir al menos una letra mayúscula."
                    !esEdicion && contrasena.none { it.isDigit() } -> "La contraseña debe incluir al menos un número."
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
}

@Composable
private fun PersonalCard(persona: Personal, onEditClick: () -> Unit) {
    val context = LocalContext.current
    val fotoPerfil = remember(persona.id) {
        FotoPerfilManager.obtenerFoto(context, persona.id)?.absolutePath ?: persona.fotoUrl
    }
    val activo = persona.estado == "ACTIVO"
    val detalle = listOf(persona.tipoPersonal, persona.turno).filter { it.isNotBlank() }.joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
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
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = persona.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333))
                    if (detalle.isNotEmpty()) Text(text = detalle, fontSize = 13.sp, color = Color(0xFF94A3B8))
                    Text(text = persona.email, fontSize = 12.sp, color = Color(0xFF94A3B8))
                    if (persona.telefono.isNotBlank()) Text(text = persona.telefono, fontSize = 12.sp, color = Color(0xFF94A3B8))
                    if (persona.fechaIngreso.isNotBlank()) Text(text = "Ingreso: ${persona.fechaIngreso}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }

                Surface(shape = RoundedCornerShape(8.dp), color = if (activo) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)) {
                    Text(
                        text = persona.estado,
                        color = if (activo) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE3F2FD)) {
                        Text(
                            text = "📍 ${persona.areaTrabajo ?: SIN_AREA}",
                            color = Color(0xFF1E88E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
                        Text(
                            text = persona.rol.nombreLegible(),
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E88E5))
                ) {
                    Text(text = "Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun HojaNotificacionesPersonal(
    onCerrar: () -> Unit,
    viewModel: PersonalNotificacionesViewModel = viewModel()
) {
    val notificaciones by viewModel.notificaciones.collectAsState()
    val leidas by viewModel.leidas.collectAsState()

    var pestanaSeleccionada by remember { mutableIntStateOf(0) }
    var modoRedactar by remember { mutableStateOf(false) }

    var notificacionEditandoId by remember { mutableStateOf<String?>(null) }
    var tituloEdit by remember { mutableStateOf("") }
    var mensajeEdit by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                when {
                    notificacionEditandoId != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Editar aviso",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = { notificacionEditandoId = null }) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TÍTULO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = tituloEdit,
                            onValueChange = { tituloEdit = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdeMarca,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("MENSAJE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mensajeEdit,
                            onValueChange = { mensajeEdit = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdeMarca,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Aviso actualizado correctamente ✅", Toast.LENGTH_SHORT).show()
                                notificacionEditandoId = null
                            },
                            enabled = tituloEdit.isNotBlank() && mensajeEdit.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdeMarca)
                        ) {
                            Text("Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }

                    modoRedactar -> {
                        var titulo by remember { mutableStateOf("") }
                        var mensaje by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Redactar nuevo aviso",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = { modoRedactar = false }) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TIPO DE DESTINATARIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("Socios") },
                                shape = RoundedCornerShape(50.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VerdeMarca,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TÍTULO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Cancha en mantenimiento", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdeMarca,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("MENSAJE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mensaje,
                            onValueChange = { mensaje = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Escribe los detalles del aviso...", color = Color(0xFF94A3B8)) },
                            minLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdeMarca,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { modoRedactar = false },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("Cancelar", color = TextoGris, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    viewModel.enviarAvisoASocios(titulo, mensaje) {
                                        Toast.makeText(context, "Aviso enviado ✅", Toast.LENGTH_SHORT).show()
                                        modoRedactar = false
                                    }
                                },
                                enabled = titulo.isNotBlank() && mensaje.isNotBlank(),
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeMarca)
                            ) {
                                Text("Enviar aviso", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avisos y Notificaciones",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = onCerrar) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        SecondaryTabRow(
                            selectedTabIndex = pestanaSeleccionada,
                            modifier = Modifier,
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = TabRowDefaults.primaryContentColor,
                            indicator = {},
                            divider = {},
                            tabs = {
                                Tab(
                                    selected = pestanaSeleccionada == 0,
                                    onClick = { pestanaSeleccionada = 0 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Recibidos", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = TextoGris
                                )
                                Tab(
                                    selected = pestanaSeleccionada == 1,
                                    onClick = { pestanaSeleccionada = 1 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Mis enviados", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = TextoGris
                                )
                            })

                        Spacer(Modifier.height(20.dp))

                        if (notificaciones.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay avisos disponibles.",
                                    color = TextoGris,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(notificaciones) { notif ->
                                    val isLeida = leidas.contains(notif.id)

                                    TarjetaNotificacionGestionable(
                                        notif = notif,
                                        isLeida = isLeida,
                                        esModoEdicion = pestanaSeleccionada == 1,
                                        onEditar = {
                                            notificacionEditandoId = notif.id
                                            tituloEdit = notif.titulo
                                            mensajeEdit = notif.mensaje
                                        },
                                        onClick = { viewModel.marcarComoLeida(notif.id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { modoRedactar = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdeMarca)
                        ) {
                            Text("Redactar nuevo aviso", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaNotificacionGestionable(
    notif: Notificacion,
    isLeida: Boolean,
    esModoEdicion: Boolean,
    onEditar: () -> Unit,
    onClick: () -> Unit
) {
    val fechaFormat = remember(notif.fechaMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.fechaMillis))
    }

    var expandida by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLeida) Color(0xFFF8FAFD) else Color.White,
        border = BorderStroke(1.dp, if (isLeida) Color(0xFFE2E8F0) else VerdeMarca.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                expandida = !expandida
                if (!isLeida) onClick()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isLeida && !esModoEdicion) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(VerdeMarca, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = notif.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextoOscuro,
                        maxLines = if (expandida) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = fechaFormat, fontSize = 11.sp, color = TextoGris)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notif.mensaje,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                maxLines = if (expandida) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (esModoEdicion) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onEditar,
                        colors = ButtonDefaults.textButtonColors(contentColor = VerdeMarca)
                    ) {
                        Text("✏️ Modificar aviso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}