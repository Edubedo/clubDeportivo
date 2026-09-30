package com.example.clubdeportivo.ui.membresia

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia

private val filtros = listOf<Pair<String, EstadoMembresia?>>(
    "Todas" to null,
    "Activas" to EstadoMembresia.ACTIVA,
    "Vencidas" to EstadoMembresia.VENCIDA,
    "Suspendidas" to EstadoMembresia.SUSPENDIDA
)

/** Miembros del club: buscar, registrar (con código único), editar, renovar y suspender. */
@Composable
internal fun MiembrosTab(
    viewModel: MembresiasAdminViewModel = viewModel(),
    preciosViewModel: PreciosViewModel = viewModel()
) {
    val membresias by viewModel.membresias.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(true)
    val guardando by viewModel.guardando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val errorFormulario by viewModel.errorFormulario.observeAsState()
    val guardadoExitoso by viewModel.guardadoExitoso.observeAsState(0)
    val registroReciente by viewModel.registroReciente.observeAsState()
    val precios by preciosViewModel.editados.observeAsState(emptyMap())

    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(0) }
    var mostrarFormulario by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<MembresiaDetalle?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }
    // Cada guardado exitoso cierra el formulario (el primer valor es el inicial, no un guardado).
    var guardadosVistos by remember { mutableStateOf(guardadoExitoso) }
    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso != guardadosVistos) {
            guardadosVistos = guardadoExitoso
            mostrarFormulario = false
            enEdicion = null
        }
    }

    val hoy = remember { Fechas.hoy() }
    val visibles = membresias
        .filter { detalle ->
            val estado = ReglasMembresia.estadoEfectivo(detalle.membresia, hoy)
            filtros[filtro].second.let { it == null || it == estado }
        }
        .filter { detalle ->
            val texto = busqueda.trim()
            texto.isBlank() || detalle.personas.any {
                it.nombre.contains(texto, ignoreCase = true) || it.codigo.contains(ReglasMembresia.normalizarCodigo(texto).takeIf { c -> c.length > 3 } ?: texto, ignoreCase = true)
            }
        }
        .sortedBy { it.titular?.nombre?.lowercase() ?: "" }

    Scaffold(
        containerColor = FondoPantalla,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    enEdicion = null
                    viewModel.limpiarErrorFormulario()
                    mostrarFormulario = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar miembro", fontWeight = FontWeight.Bold) },
                containerColor = VerdeMarca,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                "Miembros del club",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoTitulo,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                CampoTexto(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = "Buscar por nombre o código",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextoSuave) }
                )
            }
            PestanasPildora(
                opciones = filtros.map { it.first },
                seleccionada = filtro,
                onSeleccion = { filtro = it }
            )

            when {
                cargando && membresias.isEmpty() -> FullScreenLoading()
                visibles.isEmpty() -> EmptyState(
                    if (membresias.isEmpty()) "Todavía no hay miembros registrados. Agrega el primero con \"Registrar miembro\"."
                    else "Ningún miembro coincide con la búsqueda."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibles, key = { it.membresia.id }) { detalle ->
                        TarjetaMiembro(
                            detalle = detalle,
                            estado = ReglasMembresia.estadoEfectivo(detalle.membresia, hoy),
                            onEditar = {
                                viewModel.limpiarErrorFormulario()
                                enEdicion = detalle
                                mostrarFormulario = true
                            },
                            onRenovar = { viewModel.renovar(detalle) },
                            onCambiarEstado = { viewModel.cambiarEstado(detalle, it) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        MembresiaFormDialog(
            existente = enEdicion,
            precios = precios,
            error = errorFormulario,
            guardando = guardando,
            onGuardar = { tipo, plan, paqueteId, personas, estado ->
                val editando = enEdicion
                if (editando == null) viewModel.registrar(tipo, plan, paqueteId, personas)
                else viewModel.actualizar(editando, plan, paqueteId, personas, estado)
            },
            onCerrar = {
                mostrarFormulario = false
                enEdicion = null
            }
        )
    }

    registroReciente?.let { CodigosDialog(detalle = it, onCerrar = { viewModel.cerrarRegistroReciente() }) }
}

@Composable
private fun TarjetaMiembro(
    detalle: MembresiaDetalle,
    estado: EstadoMembresia,
    onEditar: () -> Unit,
    onRenovar: () -> Unit,
    onCambiarEstado: (EstadoMembresia) -> Unit
) {
    val membresia = detalle.membresia
    val titular = detalle.titular
    var menuAbierto by remember { mutableStateOf(false) }
    val (colorFondo, colorTexto) = when (estado) {
        EstadoMembresia.ACTIVA -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        EstadoMembresia.VENCIDA -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        EstadoMembresia.SUSPENDIDA -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    }

    Card(
        onClick = onEditar,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titular?.nombre ?: "Sin nombre",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoTitulo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = titular?.codigo?.takeIf { it.isNotBlank() } ?: "Sin código (registro anterior)",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (titular?.codigo.isNullOrBlank()) TextoSuave else VerdeMarca,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "${ReglasMembresia.etiquetaPlan(membresia)} · ${dinero(membresia.precio)}" +
                        if (detalle.personas.size > 1) " · ${detalle.personas.size} personas" else "",
                    fontSize = 13.sp,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = when (estado) {
                            EstadoMembresia.ACTIVA -> "Activa"
                            EstadoMembresia.VENCIDA -> "Vencida"
                            EstadoMembresia.SUSPENDIDA -> "Suspendida"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorTexto,
                        modifier = Modifier
                            .background(colorFondo, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Text(
                        text = if (membresia.tipo == TipoMembresia.VISITA) "Válida ${membresia.fechaVencimiento}" else "Vence ${membresia.fechaVencimiento}",
                        fontSize = 12.sp,
                        color = TextoSuave,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Column {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = TextoSuave)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuAbierto = false; onEditar() }
                    )
                    if (membresia.tipo != TipoMembresia.VISITA) {
                        DropdownMenuItem(
                            text = { Text("Renovar un mes") },
                            leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                            onClick = { menuAbierto = false; onRenovar() }
                        )
                    }
                    when (estado) {
                        EstadoMembresia.ACTIVA -> DropdownMenuItem(
                            text = { Text("Suspender") },
                            leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) },
                            onClick = { menuAbierto = false; onCambiarEstado(EstadoMembresia.SUSPENDIDA) }
                        )
                        EstadoMembresia.SUSPENDIDA -> DropdownMenuItem(
                            text = { Text("Reactivar") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = { menuAbierto = false; onCambiarEstado(EstadoMembresia.ACTIVA) }
                        )
                        EstadoMembresia.VENCIDA -> Unit
                    }
                }
            }
        }
    }
}

private val parentescoPorDefecto = "Integrante"

/**
 * Alta o edición de una membresía con sus personas. Misma estructura que los demás formularios: diálogo con X fija,
 * selectores en píldora, etiquetas en mayúsculas, campos estándar y un solo botón principal.
 */
@Composable
private fun MembresiaFormDialog(
    existente: MembresiaDetalle?,
    precios: Map<String, Double>,
    error: String?,
    guardando: Boolean,
    onGuardar: (TipoMembresia, PlanIndividual?, Int?, List<PersonaForm>, EstadoMembresia?) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = existente != null
    val membresia = existente?.membresia
    val anteriorALosCodigos = existente?.personas?.any { it.codigo.isBlank() } == true

    var tipo by remember { mutableStateOf(membresia?.tipo ?: TipoMembresia.INDIVIDUAL) }
    var plan by remember { mutableStateOf(membresia?.plan ?: PlanIndividual.NORMAL) }
    var paqueteId by remember { mutableStateOf(membresia?.paqueteFamiliarId ?: Catalogos.paquetesFamiliares.first().id) }
    var estado by remember { mutableStateOf(membresia?.estado ?: EstadoMembresia.ACTIVA) }

    val personas = remember {
        mutableStateListOf<PersonaForm>().apply {
            if (existente != null) {
                addAll(existente.personas.map { PersonaForm(it.codigo.ifBlank { null }, it.nombre, it.telefono, it.correo, it.parentesco) })
            } else {
                add(PersonaForm(null, "", "", "", MiembroClub.PARENTESCO_TITULAR))
            }
        }
    }

    val planSeleccionado = if (tipo == TipoMembresia.INDIVIDUAL) plan else null
    val paqueteSeleccionado = if (tipo == TipoMembresia.FAMILIAR) paqueteId else null
    val precioVigente = ClavesPrecio.deMembresia(tipo, planSeleccionado, paqueteSeleccionado, precios)
    val maxPersonas = ReglasMembresia.maxPersonas(tipo, paqueteSeleccionado)

    DialogoFormulario(
        titulo = if (esEdicion) "Editar membresía" else "Registrar miembro",
        onCerrar = onCerrar
    ) {
        if (!esEdicion) {
            EtiquetaCampo("TIPO DE MEMBRESÍA")
            PestanasPildora(
                opciones = listOf("Individual", "Familiar", "Visita"),
                seleccionada = tipo.ordinal,
                onSeleccion = {
                    tipo = TipoMembresia.entries[it]
                    // Al cambiar de tipo quedan solo el titular y, si aplica, los integrantes que caben.
                    while (personas.size > ReglasMembresia.maxPersonas(tipo, paqueteId)) personas.removeAt(personas.lastIndex)
                },
                margenHorizontal = 0.dp
            )
            EspacioCampos()
        }

        when (tipo) {
            TipoMembresia.INDIVIDUAL -> {
                EtiquetaCampo("PLAN")
                PestanasPildora(
                    opciones = listOf("Niños", "Normal", "Deluxe"),
                    seleccionada = plan.ordinal,
                    onSeleccion = { plan = PlanIndividual.entries[it] },
                    margenHorizontal = 0.dp
                )
                EspacioCampos()
            }
            TipoMembresia.FAMILIAR -> {
                EtiquetaCampo("PAQUETE")
                PestanasPildora(
                    opciones = Catalogos.paquetesFamiliares.map { it.nombre },
                    seleccionada = Catalogos.paquetesFamiliares.indexOfFirst { it.id == paqueteId }.coerceAtLeast(0),
                    onSeleccion = {
                        paqueteId = Catalogos.paquetesFamiliares[it].id
                        while (personas.size > ReglasMembresia.maxPersonas(tipo, paqueteId)) personas.removeAt(personas.lastIndex)
                    },
                    margenHorizontal = 0.dp
                )
                EspacioCampos()
            }
            TipoMembresia.VISITA -> Unit
        }

        EtiquetaCampo("PRECIO")
        Text(
            text = "${dinero(precioVigente)} ${if (tipo == TipoMembresia.VISITA) "por visita" else "al mes"}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeMarca
        )
        if (esEdicion && membresia != null && membresia.precio != precioVigente) {
            Text(
                "Esta membresía se registró a ${dinero(membresia.precio)}; pasa al precio vigente al renovarla o cambiar de plan.",
                fontSize = 12.sp,
                color = TextoSuave,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        EspacioCampos()

        if (anteriorALosCodigos) {
            EtiquetaCampo("PERSONAS")
            Text(
                "Esta membresía se registró antes de los códigos de acceso, por eso sus personas no se pueden editar aquí. Puedes cambiar su plan, renovarla o suspenderla.",
                fontSize = 13.sp,
                color = TextoSuave
            )
            existente?.personas?.forEach { Text("• ${it.nombre} (${it.parentesco})", fontSize = 14.sp, color = TextoTitulo, modifier = Modifier.padding(top = 4.dp)) }
        } else {
            personas.forEachIndexed { indice, persona ->
                CamposPersona(
                    titulo = if (indice == 0) "TITULAR" else "INTEGRANTE $indice",
                    persona = persona,
                    conParentesco = indice > 0,
                    onCambio = { personas[indice] = it },
                    onQuitar = if (indice > 0) ({ personas.removeAt(indice) }) else null
                )
                EspacioCampos()
            }

            if (tipo == TipoMembresia.FAMILIAR && personas.size < maxPersonas) {
                TextButton(onClick = { personas.add(PersonaForm(null, "", "", "", parentescoPorDefecto)) }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = VerdeMarca)
                    Text(
                        "  Agregar integrante (${personas.size} de $maxPersonas)",
                        color = VerdeMarca,
                        fontWeight = FontWeight.Bold
                    )
                }
                EspacioCampos()
            }
        }

        if (esEdicion && membresia != null && ReglasMembresia.estadoEfectivo(membresia, Fechas.hoy()) != EstadoMembresia.VENCIDA) {
            EtiquetaCampo("ESTADO")
            PestanasPildora(
                opciones = listOf("Activa", "Suspendida"),
                seleccionada = if (estado == EstadoMembresia.SUSPENDIDA) 1 else 0,
                onSeleccion = { estado = if (it == 1) EstadoMembresia.SUSPENDIDA else EstadoMembresia.ACTIVA },
                margenHorizontal = 0.dp
            )
            EspacioCampos()
        }

        if (error != null) {
            Text(
                text = error,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        BotonPrimario(
            texto = if (esEdicion) "Guardar cambios" else "Registrar miembro",
            cargando = guardando,
            onClick = {
                onGuardar(tipo, planSeleccionado, paqueteSeleccionado, personas.toList(), if (esEdicion) estado else null)
            }
        )
    }
}

@Composable
private fun CamposPersona(
    titulo: String,
    persona: PersonaForm,
    conParentesco: Boolean,
    onCambio: (PersonaForm) -> Unit,
    onQuitar: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            EtiquetaCampo(titulo)
            persona.codigo?.let {
                Text("Código: $it", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = VerdeMarca, fontWeight = FontWeight.SemiBold)
            }
        }
        if (onQuitar != null) {
            IconButton(onClick = onQuitar) {
                Icon(Icons.Default.Close, contentDescription = "Quitar integrante", tint = TextoSuave)
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    CampoTexto(value = persona.nombre, onValueChange = { onCambio(persona.copy(nombre = it)) }, placeholder = "Nombre completo")
    Spacer(modifier = Modifier.height(12.dp))
    CampoTexto(
        value = persona.telefono,
        onValueChange = { onCambio(persona.copy(telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15))) },
        placeholder = "Teléfono (opcional)",
        tipoTeclado = KeyboardType.Phone
    )
    Spacer(modifier = Modifier.height(12.dp))
    CampoTexto(
        value = persona.correo,
        onValueChange = { onCambio(persona.copy(correo = it)) },
        placeholder = "Correo (opcional)",
        tipoTeclado = KeyboardType.Email
    )
    if (conParentesco) {
        Spacer(modifier = Modifier.height(12.dp))
        CampoTexto(
            value = persona.parentesco,
            onValueChange = { onCambio(persona.copy(parentesco = it)) },
            placeholder = "Parentesco (Cónyuge, Hijo...)"
        )
    }
}

/** Muestra los códigos recién creados para entregarlos a cada persona. */
@Composable
private fun CodigosDialog(detalle: MembresiaDetalle, onCerrar: () -> Unit) {
    val portapapeles = LocalClipboardManager.current
    val contexto = LocalContext.current

    DialogoFormulario(titulo = "Miembro registrado", onCerrar = onCerrar) {
        Text(
            "Entrega a cada persona su código: con él entra al sistema, sin contraseña. Anótalo ahora o consúltalo después en la lista de miembros.",
            fontSize = 14.sp,
            color = TextoSuave
        )
        Spacer(modifier = Modifier.height(16.dp))
        detalle.personas.forEach { persona ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${persona.nombre} · ${persona.parentesco}", fontSize = 13.sp, color = TextoSuave, maxLines = 2)
                        Text(persona.codigo, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextoTitulo)
                    }
                    TextButton(onClick = {
                        portapapeles.setText(AnnotatedString(persona.codigo))
                        Toast.makeText(contexto, "Código copiado", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar", color = VerdeMarca, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BotonPrimario(texto = "Listo", onClick = onCerrar)
    }
}
