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
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca
import com.example.clubdeportivo.util.FotoPerfilManager

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
            Text(
                "Personal",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
}

/**
 * Formulario de alta/edición de personal. Al crear, genera la cuenta de acceso (correo + contraseña); al editar,
 * el correo no cambia y la contraseña se restablece por correo.
 */
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
            // Los roles heredados (Superadmin, Admin de área) se muestran como los dos actuales.
            personaAEditar?.rol?.let { if (it == Rol.SUPERADMIN) Rol.ADMIN else if (it in ROLES_DE_PERSONAL) it else Rol.AYUDANTE_AREA }
                ?: Rol.AYUDANTE_AREA
        )
    }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // La cuenta (y su uid) todavía no existe al dar de alta: la foto se guarda con un id temporal.
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

    // Se conserva el tipo que ya tenga la persona aunque ya no se ofrezca para altas nuevas.
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
