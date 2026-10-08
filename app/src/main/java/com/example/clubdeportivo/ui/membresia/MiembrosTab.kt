package com.example.clubdeportivo.ui.membresia

import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.MotivoSuspension
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoBusqueda
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import com.example.clubdeportivo.ui.theme.Alerta
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia

private val filtros = listOf<Pair<String, EstadoMembresia?>>(
    "Todas" to null,
    "Activas" to EstadoMembresia.ACTIVA,
    "Vencidas" to EstadoMembresia.VENCIDA,
    "Suspendidas" to EstadoMembresia.SUSPENDIDA
)

/** Miembros del club: buscar, registrar (con código único y cobro), editar, renovar y suspender con motivo. */
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
    var suspendiendo by remember { mutableStateOf<MembresiaDetalle?>(null) }
    var renovando by remember { mutableStateOf<MembresiaDetalle?>(null) }
    var restableciendo by remember { mutableStateOf<MiembroClub?>(null) }
    val listState = rememberLazyListState()

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
        .sortedWith(
            compareBy<MembresiaDetalle> { ReglasMembresia.estadoEfectivo(it.membresia, hoy) != EstadoMembresia.ACTIVA }
                .thenBy { it.titular?.nombre?.lowercase() ?: "" }
        )

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            BotonFlotanteAgregar(
                texto = "Registrar miembro",
                visible = listState.botonFlotanteVisible(),
                onClick = {
                    enEdicion = null
                    viewModel.limpiarErrorFormulario()
                    mostrarFormulario = true
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            EncabezadoPantalla(
                titulo = "Miembros del club",
                modifier = Modifier.padding(horizontal = MargenPantalla)
            )
            Column(modifier = Modifier.padding(horizontal = MargenPantalla)) {
                CampoBusqueda(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = "Buscar por nombre o código"
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
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
                            onRenovar = { renovando = detalle },
                            onSuspender = { suspendiendo = detalle },
                            onReactivar = { viewModel.cambiarEstado(detalle, EstadoMembresia.ACTIVA) }
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
            onGuardar = { tipo, plan, paqueteId, personas, estado, motivo, metodo ->
                val editando = enEdicion
                if (editando == null) viewModel.registrar(tipo, plan, paqueteId, personas, metodo)
                else viewModel.actualizar(editando, plan, paqueteId, personas, estado, motivo)
            },
            onRestablecerAcceso = { restableciendo = it },
            onCerrar = {
                mostrarFormulario = false
                enEdicion = null
            }
        )
    }

    suspendiendo?.let { detalle ->
        DialogoSuspension(
            nombre = detalle.titular?.nombre ?: "este miembro",
            onConfirmar = { motivo ->
                viewModel.cambiarEstado(detalle, EstadoMembresia.SUSPENDIDA, motivo)
                suspendiendo = null
            },
            onCancelar = { suspendiendo = null }
        )
    }

    renovando?.let { detalle ->
        val membresia = detalle.membresia
        val precio = ClavesPrecio.deMembresia(membresia.tipo, membresia.plan, membresia.paqueteFamiliarId, precios)
        DialogoRenovar(
            nombre = detalle.titular?.nombre ?: "Miembro",
            plan = ReglasMembresia.etiquetaPlan(membresia),
            precio = precio,
            vigenteHasta = Fechas.legible(ReglasMembresia.fechasDeRenovacion(membresia, hoy).second),
            onConfirmar = { metodo ->
                viewModel.renovar(detalle, metodo)
                renovando = null
            },
            onCancelar = { renovando = null }
        )
    }

    restableciendo?.let { persona ->
        DialogoConfirmacion(
            titulo = "Restablecer acceso",
            mensaje = "${persona.nombre} podrá registrarse de nuevo con su código ${persona.codigo}. " +
                "Su cuenta actual (${persona.correo.ifBlank { "la que creó" }}) dejará de funcionar. " +
                "Úsalo solo si se equivocó de correo o perdió el acceso.",
            textoConfirmar = "Restablecer",
            textoCancelar = "Cancelar",
            onConfirmar = {
                viewModel.restablecerAcceso(persona.codigo)
                restableciendo = null
            },
            onCancelar = { restableciendo = null }
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
    onSuspender: () -> Unit,
    onReactivar: () -> Unit
) {
    val membresia = detalle.membresia
    val titular = detalle.titular
    var menuAbierto by remember { mutableStateOf(false) }
    val tipoInsignia = when (estado) {
        EstadoMembresia.ACTIVA -> TipoInsignia.EXITO
        EstadoMembresia.VENCIDA -> TipoInsignia.PELIGRO
        EstadoMembresia.SUSPENDIDA -> TipoInsignia.ALERTA
    }
    val conCodigo = detalle.personas.filter { it.codigo.isNotBlank() }
    val registrados = conCodigo.count { it.cuentaCreada }

    TarjetaClub(modifier = Modifier.fillMaxWidth(), onClick = onEditar) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titular?.nombre ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = titular?.codigo?.takeIf { it.isNotBlank() } ?: "Sin código (registro anterior)",
                    style = MaterialTheme.typography.titleSmall,
                    letterSpacing = 0.5.sp,
                    color = if (titular?.codigo.isNullOrBlank()) TextoSecundario else Marca,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "${ReglasMembresia.etiquetaPlan(membresia)} · ${dinero(membresia.precio)}" +
                        if (detalle.personas.size > 1) " · ${detalle.personas.size} personas" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Insignia(
                        texto = when (estado) {
                            EstadoMembresia.ACTIVA -> "Activa"
                            EstadoMembresia.VENCIDA -> "Vencida"
                            EstadoMembresia.SUSPENDIDA -> "Suspendida"
                        },
                        tipo = tipoInsignia
                    )
                    Text(
                        text = if (membresia.tipo == TipoMembresia.VISITA) "Válida ${Fechas.legible(membresia.fechaVencimiento)}" else "Vence ${Fechas.legible(membresia.fechaVencimiento)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (estado == EstadoMembresia.SUSPENDIDA && membresia.motivoSuspension.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Alerta, modifier = Modifier.padding(top = 2.dp).height(16.dp))
                        Text(
                            text = "Motivo: ${membresia.motivoSuspension}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Alerta
                        )
                    }
                }
                if (conCodigo.isNotEmpty()) {
                    Text(
                        text = when {
                            registrados == conCodigo.size && conCodigo.size == 1 -> "Ya creó su cuenta en la app"
                            registrados == conCodigo.size -> "Todos crearon su cuenta en la app"
                            registrados == 0 && conCodigo.size == 1 -> "Aún no crea su cuenta en la app"
                            else -> "$registrados de ${conCodigo.size} crearon su cuenta en la app"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Column {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = TextoSecundario)
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
                            onClick = { menuAbierto = false; onSuspender() }
                        )
                        EstadoMembresia.SUSPENDIDA -> DropdownMenuItem(
                            text = { Text("Reactivar") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = { menuAbierto = false; onReactivar() }
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
 * selectores en píldora, etiquetas en mayúsculas, campos estándar y un solo botón principal siempre visible abajo.
 */
@Composable
private fun MembresiaFormDialog(
    existente: MembresiaDetalle?,
    precios: Map<String, Double>,
    error: String?,
    guardando: Boolean,
    onGuardar: (TipoMembresia, PlanIndividual?, Int?, List<PersonaForm>, EstadoMembresia?, String?, MetodoPago) -> Unit,
    onRestablecerAcceso: (MiembroClub) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = existente != null
    val membresia = existente?.membresia
    val anteriorALosCodigos = existente?.personas?.any { it.codigo.isBlank() } == true

    var tipo by remember { mutableStateOf(membresia?.tipo ?: TipoMembresia.INDIVIDUAL) }
    var plan by remember { mutableStateOf(membresia?.plan ?: PlanIndividual.NORMAL) }
    var paqueteId by remember { mutableStateOf(membresia?.paqueteFamiliarId ?: Catalogos.paquetesFamiliares.first().id) }
    var estado by remember { mutableStateOf(membresia?.estado ?: EstadoMembresia.ACTIVA) }
    var metodoPago by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var motivo by remember { mutableStateOf<MotivoSuspension?>(null) }
    var detalleMotivo by remember { mutableStateOf("") }

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

    val suspendiendoAhora = esEdicion && estado == EstadoMembresia.SUSPENDIDA && membresia?.estado != EstadoMembresia.SUSPENDIDA

    DialogoFormulario(
        titulo = if (esEdicion) "Editar membresía" else "Registrar miembro",
        onCerrar = onCerrar,
        pie = {
            if (error != null) {
                Text(
                    text = error,
                    color = Peligro,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            BotonPrimario(
                texto = if (esEdicion) "Guardar cambios" else "Cobrar ${dinero(precioVigente)} y registrar",
                cargando = guardando,
                enabled = !suspendiendoAhora || motivoCompleto(motivo, detalleMotivo),
                onClick = {
                    onGuardar(
                        tipo,
                        planSeleccionado,
                        paqueteSeleccionado,
                        personas.toList(),
                        if (esEdicion) estado else null,
                        if (suspendiendoAhora) motivo?.textoGuardado(detalleMotivo) else null,
                        metodoPago
                    )
                }
            )
        }
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
            color = Marca
        )
        if (esEdicion && membresia != null && membresia.precio != precioVigente) {
            Text(
                "Esta membresía se registró a ${dinero(membresia.precio)}; pasa al precio vigente al renovarla o cambiar de plan.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (!esEdicion) {
            EspacioCampos()
            EtiquetaCampo("MÉTODO DE PAGO")
            PestanasPildora(
                opciones = MetodoPago.entries.map { it.etiqueta },
                seleccionada = metodoPago.ordinal,
                onSeleccion = { metodoPago = MetodoPago.entries[it] },
                margenHorizontal = 0.dp
            )
            Text(
                text = "Al registrar se anota el cobro de ${dinero(precioVigente)}.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }

        EspacioCampos()

        if (anteriorALosCodigos) {
            EtiquetaCampo("PERSONAS")
            Text(
                "Esta membresía se registró antes de los códigos de acceso, por eso sus personas no se pueden editar aquí. Puedes cambiar su plan, renovarla o suspenderla.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            existente?.personas?.forEach { Text("• ${it.nombre} (${it.parentesco})", style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal, modifier = Modifier.padding(top = 4.dp)) }
        } else {
            personas.forEachIndexed { indice, persona ->
                val registrada = existente?.personas?.firstOrNull { it.codigo == persona.codigo && persona.codigo != null }
                CamposPersona(
                    titulo = if (indice == 0) "TITULAR" else "INTEGRANTE $indice",
                    persona = persona,
                    conParentesco = indice > 0,
                    cuentaCreada = registrada?.cuentaCreada,
                    onCambio = { personas[indice] = it },
                    onQuitar = if (indice > 0) ({ personas.removeAt(indice) }) else null,
                    onRestablecerAcceso = registrada?.takeIf { it.cuentaCreada }?.let { miembro -> { onRestablecerAcceso(miembro) } }
                )
                EspacioCampos()
            }

            if (tipo == TipoMembresia.FAMILIAR && personas.size < maxPersonas) {
                TextButton(onClick = { personas.add(PersonaForm(null, "", "", "", parentescoPorDefecto)) }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Marca)
                    Text(
                        "  Agregar integrante (${personas.size} de $maxPersonas)",
                        color = Marca,
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
            if (suspendiendoAhora) {
                Spacer(modifier = Modifier.height(4.dp))
                EtiquetaCampo("MOTIVO DE LA SUSPENSIÓN")
                SelectorMotivoSuspension(
                    seleccion = motivo,
                    onSeleccion = { motivo = it },
                    detalle = detalleMotivo,
                    onDetalle = { detalleMotivo = it }
                )
            } else if (estado == EstadoMembresia.SUSPENDIDA && !membresia.motivoSuspension.isBlank()) {
                Text(
                    text = "Motivo: ${membresia.motivoSuspension}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Alerta
                )
            }
        }
    }
}

@Composable
private fun CamposPersona(
    titulo: String,
    persona: PersonaForm,
    conParentesco: Boolean,
    /** null = persona nueva (aún sin código); true/false = ya tiene código y se sabe si creó su cuenta. */
    cuentaCreada: Boolean?,
    onCambio: (PersonaForm) -> Unit,
    onQuitar: (() -> Unit)?,
    onRestablecerAcceso: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            EtiquetaCampo(titulo)
            persona.codigo?.let {
                Text("Código: $it", style = MaterialTheme.typography.titleSmall, letterSpacing = 0.5.sp, color = Marca)
                if (cuentaCreada != null) {
                    Text(
                        text = if (cuentaCreada) "Ya creó su cuenta en la app" else "Aún no crea su cuenta en la app",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
        }
        if (onQuitar != null) {
            IconButton(onClick = onQuitar) {
                Icon(Icons.Default.Close, contentDescription = "Quitar integrante", tint = TextoSecundario)
            }
        }
    }
    if (onRestablecerAcceso != null) {
        TextButton(onClick = onRestablecerAcceso) {
            Text("Restablecer acceso", color = Marca, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    CampoTexto(
        value = persona.nombre,
        onValueChange = { onCambio(persona.copy(nombre = it)) },
        placeholder = "Nombre completo",
        capitalizacion = KeyboardCapitalization.Words
    )
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
        tipoTeclado = KeyboardType.Email,
        imeAction = if (conParentesco) ImeAction.Next else ImeAction.Done
    )
    if (conParentesco) {
        Spacer(modifier = Modifier.height(12.dp))
        CampoTexto(
            value = persona.parentesco,
            onValueChange = { onCambio(persona.copy(parentesco = it)) },
            placeholder = "Parentesco (Cónyuge, Hijo...)",
            capitalizacion = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        )
    }
}

/** Muestra los códigos recién creados para entregarlos a cada persona. */
@Composable
private fun CodigosDialog(detalle: MembresiaDetalle, onCerrar: () -> Unit) {
    val portapapeles = LocalClipboardManager.current
    val contexto = LocalContext.current

    DialogoFormulario(
        titulo = "Miembro registrado",
        onCerrar = onCerrar,
        pie = { BotonPrimario(texto = "Listo", onClick = onCerrar) }
    ) {
        Text(
            "Entrega a cada persona su código. Con él se registra en la app: Regístrate → escribe el código → crea su correo y contraseña. " +
                "Anótalo ahora o consúltalo después en la lista de miembros.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Spacer(modifier = Modifier.height(16.dp))
        detalle.personas.forEach { persona ->
            TarjetaClub(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                fondo = MarcaSuave
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${persona.nombre} · ${persona.parentesco}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, maxLines = 2)
                        Text(persona.codigo, fontSize = 24.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                    }
                    TextButton(onClick = {
                        portapapeles.setText(AnnotatedString(persona.codigo))
                        Toast.makeText(contexto, "Código copiado", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar", color = Marca, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
