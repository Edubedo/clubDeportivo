package com.example.clubdeportivo.ui.membresia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val fondoPantalla = Color(0xFFF8FAFD)
private val colorVerde = Color(0xFF10B981)
private val rolesDeAdministracion = setOf(Rol.SUPERADMIN, Rol.ADMIN)

@Composable
fun MembresiaScreen(viewModel: MembresiaViewModel = viewModel()) {
    val esAdministrador = SesionManager.usuarioActual?.rol in rolesDeAdministracion
    if (esAdministrador) {
        MembresiasAdminScreen(viewModel)
    } else {
        MembresiaSocioScreen(viewModel)
    }
}

// =====================================================================
// Vista del socio: solo lectura de su propia membresía (comportamiento original)
// =====================================================================

@Composable
private fun MembresiaSocioScreen(viewModel: MembresiaViewModel) {
    val cargando by viewModel.cargando.observeAsState(false)
    val titulo by viewModel.titulo.observeAsState("")
    val mensaje by viewModel.mensaje.observeAsState()
    val detalle by viewModel.detalle.observeAsState()
    val beneficios by viewModel.beneficios.observeAsState(emptyList())
    val integrantes by viewModel.integrantes.observeAsState(emptyList())

    if (cargando) {
        FullScreenLoading()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                mensaje?.let {
                    Text(
                        text = it,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                detalle?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (beneficios.isNotEmpty()) {
            Text(
                text = "Qué incluye",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Column(modifier = Modifier.padding(top = 8.dp)) {
                beneficios.forEach { beneficio ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(beneficio)
                    }
                }
            }
        }

        if (integrantes.isNotEmpty()) {
            Text(
                text = "Integrantes familiares",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Column(modifier = Modifier.padding(top = 8.dp)) {
                integrantes.forEach { integrante ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("${integrante.nombre} (${integrante.parentesco})")
                    }
                }
            }
        }

        CatalogoPlanesYPaquetes()
    }
}

// =====================================================================
// Catálogo de referencia: planes individuales, paquetes familiares y
// visita sencilla, con los precios y beneficios definidos en Catalogos.
// =====================================================================

@Composable
private fun CatalogoPlanesYPaquetes() {
    Text(
        text = "Planes y paquetes disponibles",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF111827),
        modifier = Modifier.padding(top = 32.dp)
    )
    Text(
        text = "Individual, familiar o visita sencilla: elige lo que más te convenga.",
        fontSize = 13.sp,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
    )

    Text(
        text = "MEMBRESÍAS INDIVIDUALES",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlanIndividual.entries.forEach { plan ->
            TarjetaPlanCatalogo(
                titulo = Catalogos.nombrePlanIndividual(plan),
                precio = Catalogos.precioPlanIndividual(plan),
                detalles = Catalogos.beneficiosPlanIndividual(plan)
            )
        }
    }

    Text(
        text = "PAQUETES FAMILIARES",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Catalogos.paquetesFamiliares.forEach { paquete ->
            TarjetaPlanCatalogo(
                titulo = "Paquete ${paquete.nombre}",
                precio = paquete.precioMensual,
                detalles = listOf(paquete.descripcion, "Hasta ${paquete.maxIntegrantes} integrantes")
            )
        }
    }

    Text(
        text = "VISITA SENCILLA",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Visita sencilla",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "Pago único por día, sin reservaciones recurrentes.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "$%.0f".format(Catalogos.PRECIO_VISITA),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colorVerde
            )
        }
    }
}

@Composable
private fun TarjetaPlanCatalogo(titulo: String, precio: Double, detalles: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "$%.0f/mes".format(precio),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorVerde
                )
            }
            if (detalles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                detalles.forEach { detalle ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = colorVerde,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 6.dp)
                        )
                        Text(detalle, fontSize = 13.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}

// =====================================================================
// Vista del administrador: alta, edición e integrantes familiares
// =====================================================================

@Composable
private fun MembresiasAdminScreen(viewModel: MembresiaViewModel) {
    val cargando by viewModel.cargando.observeAsState(false)
    val membresias by viewModel.membresiasAdmin.observeAsState(emptyList())
    val socios by viewModel.socios.observeAsState(emptyList())
    val mostrarModal by viewModel.mostrarModalCrear.observeAsState(false)
    val itemEnEdicion by viewModel.membresiaEnEdicion.observeAsState()
    val mensaje by viewModel.mensajeAdmin.observeAsState()

    var itemParaIntegrantes by remember { mutableStateOf<MembresiaAdminItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.onMensajeAdminMostrado()
        }
    }

    // Si la lista cambió (ej. se agregó un integrante), mantener el diálogo de
    // integrantes sincronizado con los datos frescos en vez de mostrar copia vieja.
    LaunchedEffect(membresias) {
        itemParaIntegrantes?.let { actual ->
            itemParaIntegrantes = membresias.find { it.membresia.id == actual.membresia.id }
        }
    }

    Scaffold(
        containerColor = fondoPantalla,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.abrirModalCrear() },
                containerColor = colorVerde
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Nueva membresía", tint = Color.White)
                    Text("Nueva membresía", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        when {
            cargando && membresias.isEmpty() -> FullScreenLoading(modifier = Modifier.padding(innerPadding))
            membresias.isEmpty() -> EmptyState(
                mensaje = "No hay membresías registradas todavía. Crea la primera con \"Nueva membresía\".",
                modifier = Modifier.padding(innerPadding)
            )
            else -> Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Text(
                    text = "Membresías y paquetes familiares",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(membresias, key = { it.membresia.id }) { item ->
                        MembresiaCardAdmin(
                            item = item,
                            onEditar = { viewModel.abrirModalEditar(item) },
                            onGestionarIntegrantes = { itemParaIntegrantes = item }
                        )
                    }
                }
            }
        }
    }

    if (mostrarModal) {
        ModalMembresia(
            socios = socios,
            itemExistente = itemEnEdicion,
            onGuardar = { usuarioId, tipo, plan, paqueteFamiliarId, estado, fechaInicio, fechaVencimiento ->
                viewModel.guardarMembresia(usuarioId, tipo, plan, paqueteFamiliarId, estado, fechaInicio, fechaVencimiento)
            },
            onCerrar = { viewModel.cerrarModal() }
        )
    }

    itemParaIntegrantes?.let { item ->
        ModalIntegrantes(
            item = item,
            onAgregar = { nombre, parentesco -> viewModel.agregarIntegrante(item.membresia.id, nombre, parentesco) },
            onEliminar = { integranteId -> viewModel.eliminarIntegrante(integranteId) },
            onCerrar = { itemParaIntegrantes = null }
        )
    }
}

private fun descripcionPlan(item: MembresiaAdminItem): String = when (item.membresia.tipo) {
    TipoMembresia.INDIVIDUAL -> item.membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: "Individual"
    TipoMembresia.FAMILIAR -> item.membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it)?.nombre }
        ?.let { "Paquete $it" } ?: "Paquete familiar"
    TipoMembresia.VISITA -> "Visita"
}

private fun estiloEstado(estado: EstadoMembresia): Pair<Color, Color> = when (estado) {
    EstadoMembresia.ACTIVA -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
    EstadoMembresia.VENCIDA -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    EstadoMembresia.SUSPENDIDA -> Color(0xFFFEF3C7) to Color(0xFFD97706)
}

@Composable
private fun MembresiaCardAdmin(
    item: MembresiaAdminItem,
    onEditar: () -> Unit,
    onGestionarIntegrantes: () -> Unit
) {
    val (fondoEstado, textoEstado) = estiloEstado(item.membresia.estado)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nombreSocio,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = descripcionPlan(item),
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = fondoEstado) {
                    Text(
                        text = item.membresia.estado.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textoEstado,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar membresía",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vigente del ${item.membresia.fechaInicio} al ${item.membresia.fechaVencimiento} · $%.0f/mes"
                    .format(item.membresia.precio),
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )

            if (item.membresia.tipo == TipoMembresia.FAMILIAR) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onGestionarIntegrantes)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = colorVerde,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.integrantes.size} integrante(s) · Gestionar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorVerde
                    )
                }
            }
        }
    }
}

// --- Formulario de alta/edición de membresía ---

@Composable
private fun coloresDeCampo() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF111827),
    unfocusedTextColor = Color(0xFF111827),
    disabledTextColor = Color(0xFF111827),
    focusedBorderColor = colorVerde,
    cursorColor = colorVerde
)

private fun fechaAMillis(fecha: String): Long? = try {
    LocalDate.parse(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (e: Exception) {
    null
}

private fun millisAFecha(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalMembresia(
    socios: List<Usuario>,
    itemExistente: MembresiaAdminItem?,
    onGuardar: (
        usuarioId: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String
    ) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = itemExistente != null

    var usuarioSeleccionado by remember {
        mutableStateOf(socios.find { it.id == itemExistente?.membresia?.usuarioId } ?: socios.firstOrNull())
    }
    var tipo by remember { mutableStateOf(itemExistente?.membresia?.tipo ?: TipoMembresia.INDIVIDUAL) }
    var plan by remember { mutableStateOf(itemExistente?.membresia?.plan ?: PlanIndividual.NORMAL) }
    var paquete by remember {
        mutableStateOf(
            itemExistente?.membresia?.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it) }
                ?: Catalogos.paquetesFamiliares.first()
        )
    }
    var estado by remember { mutableStateOf(itemExistente?.membresia?.estado ?: EstadoMembresia.ACTIVA) }
    var fechaInicio by remember { mutableStateOf(itemExistente?.membresia?.fechaInicio ?: Fechas.hoy()) }
    var fechaVencimiento by remember { mutableStateOf(itemExistente?.membresia?.fechaVencimiento ?: Fechas.sumarDias(365)) }

    var expandidoSocio by remember { mutableStateOf(false) }
    var expandidoTipo by remember { mutableStateOf(false) }
    var expandidoPlan by remember { mutableStateOf(false) }
    var expandidoEstado by remember { mutableStateOf(false) }

    val puedeGuardar = usuarioSeleccionado != null && fechaInicio.isNotBlank() && fechaVencimiento.isNotBlank()

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (esEdicion) "Editar membresía" else "+ Nueva membresía",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    IconButton(onClick = onCerrar) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampoMembresia("SOCIO")
                ExposedDropdownMenuBox(
                    expanded = expandidoSocio && !esEdicion,
                    onExpandedChange = { if (!esEdicion) expandidoSocio = it }
                ) {
                    OutlinedTextField(
                        value = usuarioSeleccionado?.nombre ?: "Sin socios registrados",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !esEdicion,
                        trailingIcon = { if (!esEdicion) Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresDeCampo()
                    )
                    ExposedDropdownMenu(
                        expanded = expandidoSocio && !esEdicion,
                        onDismissRequest = { expandidoSocio = false }
                    ) {
                        socios.forEach { socio ->
                            DropdownMenuItem(
                                text = { Text(socio.nombre) },
                                onClick = { usuarioSeleccionado = socio; expandidoSocio = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampoMembresia("TIPO DE MEMBRESÍA")
                ExposedDropdownMenuBox(
                    expanded = expandidoTipo,
                    onExpandedChange = { expandidoTipo = it }
                ) {
                    OutlinedTextField(
                        value = if (tipo == TipoMembresia.INDIVIDUAL) "Individual" else "Familiar",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresDeCampo()
                    )
                    ExposedDropdownMenu(
                        expanded = expandidoTipo,
                        onDismissRequest = { expandidoTipo = false }
                    ) {
                        DropdownMenuItem(text = { Text("Individual") }, onClick = { tipo = TipoMembresia.INDIVIDUAL; expandidoTipo = false })
                        DropdownMenuItem(text = { Text("Familiar") }, onClick = { tipo = TipoMembresia.FAMILIAR; expandidoTipo = false })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (tipo == TipoMembresia.INDIVIDUAL) {
                    EtiquetaCampoMembresia("PLAN")
                    ExposedDropdownMenuBox(expanded = expandidoPlan, onExpandedChange = { expandidoPlan = it }) {
                        OutlinedTextField(
                            value = Catalogos.nombrePlanIndividual(plan),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresDeCampo()
                        )
                        ExposedDropdownMenu(expanded = expandidoPlan, onDismissRequest = { expandidoPlan = false }) {
                            PlanIndividual.entries.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text("${Catalogos.nombrePlanIndividual(opcion)} · $%.0f/mes".format(Catalogos.precioPlanIndividual(opcion))) },
                                    onClick = { plan = opcion; expandidoPlan = false }
                                )
                            }
                        }
                    }
                } else {
                    EtiquetaCampoMembresia("PAQUETE FAMILIAR")
                    ExposedDropdownMenuBox(expanded = expandidoPlan, onExpandedChange = { expandidoPlan = it }) {
                        OutlinedTextField(
                            value = "${paquete.nombre} · hasta ${paquete.maxIntegrantes} integrantes",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresDeCampo()
                        )
                        ExposedDropdownMenu(expanded = expandidoPlan, onDismissRequest = { expandidoPlan = false }) {
                            Catalogos.paquetesFamiliares.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text("${opcion.nombre} · $%.0f/mes".format(opcion.precioMensual)) },
                                    onClick = { paquete = opcion; expandidoPlan = false }
                                )
                            }
                        }
                    }
                }

                if (esEdicion) {
                    Spacer(modifier = Modifier.height(20.dp))
                    EtiquetaCampoMembresia("ESTADO")
                    ExposedDropdownMenuBox(expanded = expandidoEstado, onExpandedChange = { expandidoEstado = it }) {
                        OutlinedTextField(
                            value = estado.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresDeCampo()
                        )
                        ExposedDropdownMenu(expanded = expandidoEstado, onDismissRequest = { expandidoEstado = false }) {
                            EstadoMembresia.entries.forEach { opcion ->
                                DropdownMenuItem(text = { Text(opcion.name) }, onClick = { estado = opcion; expandidoEstado = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampoMembresia("FECHA INICIO")
                        CampoFechaMembresia(fecha = fechaInicio, onFechaSeleccionada = { fechaInicio = it })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampoMembresia("FECHA VENCIMIENTO")
                        CampoFechaMembresia(fecha = fechaVencimiento, onFechaSeleccionada = { fechaVencimiento = it })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val socio = usuarioSeleccionado ?: return@Button
                        onGuardar(
                            socio.id,
                            tipo,
                            if (tipo == TipoMembresia.INDIVIDUAL) plan else null,
                            if (tipo == TipoMembresia.FAMILIAR) paquete.id else null,
                            estado,
                            fechaInicio,
                            fechaVencimiento
                        )
                    },
                    enabled = puedeGuardar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorVerde)
                ) {
                    Text(
                        text = if (esEdicion) "Guardar cambios" else "Crear membresía",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFechaMembresia(fecha: String, onFechaSeleccionada: (String) -> Unit) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fecha,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Elegir") },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha", tint = Color(0xFF94A3B8))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = coloresDeCampo()
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { mostrarCalendario = true },
            color = Color.Transparent
        ) {}
    }

    if (mostrarCalendario) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = fechaAMillis(fecha)
                ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { onFechaSeleccionada(millisAFecha(it)) }
                    mostrarCalendario = false
                }) {
                    Text("Aceptar", color = colorVerde, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

@Composable
private fun EtiquetaCampoMembresia(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// --- Gestión de integrantes familiares ---

@Composable
private fun ModalIntegrantes(
    item: MembresiaAdminItem,
    onAgregar: (nombre: String, parentesco: String) -> Unit,
    onEliminar: (integranteId: String) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }

    val maxIntegrantes = item.membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it)?.maxIntegrantes } ?: Int.MAX_VALUE
    val cupoLleno = item.integrantes.size >= maxIntegrantes

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Integrantes familiares",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "${item.nombreSocio} · ${item.integrantes.size}/$maxIntegrantes",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (item.integrantes.isEmpty()) {
                    Text(
                        text = "Todavía no hay integrantes agregados.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(item.integrantes, key = { it.id }) { integrante ->
                            FilaIntegrante(integrante = integrante, onEliminar = { onEliminar(integrante.id) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (cupoLleno) {
                    Text(
                        text = "Este paquete ya alcanzó su cupo máximo de integrantes.",
                        fontSize = 12.sp,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    EtiquetaCampoMembresia("NOMBRE")
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = { Text("Ej: María Pérez") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresDeCampo()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EtiquetaCampoMembresia("PARENTESCO")
                    OutlinedTextField(
                        value = parentesco,
                        onValueChange = { parentesco = it },
                        placeholder = { Text("Ej: Cónyuge, Hijo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresDeCampo()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onAgregar(nombre.trim(), parentesco.trim())
                            nombre = ""
                            parentesco = ""
                        },
                        enabled = nombre.isNotBlank() && parentesco.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorVerde)
                    ) {
                        Text("Agregar integrante", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaIntegrante(integrante: IntegranteFamiliar, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFD), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(integrante.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1F2937))
                Text(integrante.parentesco, fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
        IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Quitar integrante", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
        }
    }
}
