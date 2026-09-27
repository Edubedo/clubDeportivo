package com.example.clubdeportivo.ui.torneos

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import kotlinx.coroutines.launch

private val fondoPantalla = Color(0xFFF8FAFD)
private val colorVerde = Color(0xFF10B981)
private val rolesDeAdministracion = setOf(Rol.SUPERADMIN, Rol.ADMIN, Rol.ADMIN_AREA)

@Composable
fun TorneosScreen(viewModel: TorneosViewModel = viewModel()) {
    val torneos by viewModel.torneos.observeAsState(emptyList())
    val areas by viewModel.areas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val mostrarModalCrear by viewModel.mostrarModalCrear.observeAsState(false)

    val esAdministrador = SesionManager.usuarioActual?.rol in rolesDeAdministracion

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = if (esAdministrador) fondoPantalla else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (esAdministrador) {
                FloatingActionButton(
                    onClick = { viewModel.abrirModalCrear() },
                    containerColor = colorVerde
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Nuevo torneo", tint = Color.White)
                        Text("Nuevo torneo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            cargando && torneos.isEmpty() -> FullScreenLoading(modifier = Modifier.padding(innerPadding))
            torneos.isEmpty() && !esAdministrador -> EmptyState(
                mensaje = "No hay torneos disponibles.",
                modifier = Modifier.padding(innerPadding)
            )
            esAdministrador -> TorneosAdminContent(torneos = torneos, modifier = Modifier.padding(innerPadding))
            else -> TorneosSocioContent(
                torneos = torneos,
                modifier = Modifier.padding(innerPadding),
                onInscribirse = { viewModel.inscribirse(it) }
            )
        }
    }

    if (mostrarModalCrear) {
        ModalNuevoTorneo(
            areas = areas,
            onCrear = { nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo ->
                viewModel.crearTorneo(nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo)
            },
            onCerrar = { viewModel.cerrarModalCrear() }
        )
    }
}

/** Vista de administración: encabezado de marca + tarjetas con cupo y avance, sin botón de inscripción. */
@Composable
private fun TorneosAdminContent(torneos: List<Torneo>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Torneos",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (torneos.isEmpty()) {
            EmptyState(mensaje = "No hay torneos todavía. Crea el primero con \"Nuevo torneo\".")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(torneos, key = { it.id }) { torneo -> TorneoCardAdmin(torneo) }
            }
        }
    }
}

@Composable
private fun TorneoCardAdmin(torneo: Torneo) {
    val estilo = estiloDisciplina(torneo.disciplina)
    val libres = (torneo.cupoMaximo - torneo.inscritos).coerceAtLeast(0)
    val progreso = if (torneo.cupoMaximo > 0) torneo.inscritos.toFloat() / torneo.cupoMaximo.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = torneo.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                EtiquetaDisciplina(estilo = estilo, disciplina = torneo.disciplina)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${torneo.fechaInicio} – ${torneo.fechaFin}",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${torneo.inscritos}/${torneo.cupoMaximo} participantes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "$libres libres",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorVerde
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = colorVerde,
                trackColor = Color(0xFFE5E7EB)
            )
        }
    }
}

private data class EstiloDisciplina(val emoji: String, val fondo: Color, val texto: Color)

private fun estiloDisciplina(disciplina: String): EstiloDisciplina = when (disciplina) {
    "Tenis" -> EstiloDisciplina("🎾", Color(0xFFFCE7F3), Color(0xFFDB2777))
    "Fútbol" -> EstiloDisciplina("⚽", Color(0xFFEFF6FF), Color(0xFF2563EB))
    "Básquetbol", "Baloncesto" -> EstiloDisciplina("🏀", Color(0xFFFFFBEB), Color(0xFFD97706))
    "Natación" -> EstiloDisciplina("🏊", Color(0xFFECFEFF), Color(0xFF0891B2))
    "Voleibol" -> EstiloDisciplina("🏐", Color(0xFFFAF5FF), Color(0xFF7C3AED))
    else -> EstiloDisciplina("🏆", Color(0xFFF3F4F6), Color(0xFF6B7280))
}

@Composable
private fun EtiquetaDisciplina(estilo: EstiloDisciplina, disciplina: String) {
    Surface(shape = RoundedCornerShape(50), color = estilo.fondo) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = estilo.emoji, fontSize = 12.sp)
            Text(text = disciplina, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = estilo.texto)
        }
    }
}

/** Vista para socios/visitantes: lista simple con progreso e inscripción, como antes. */
@Composable
private fun TorneosSocioContent(
    torneos: List<Torneo>,
    modifier: Modifier = Modifier,
    onInscribirse: (Torneo) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(torneos, key = { it.id }) { torneo ->
            TorneoCardSocio(torneo = torneo, onInscribirse = { onInscribirse(torneo) })
        }
    }
}

@Composable
private fun TorneoCardSocio(torneo: Torneo, onInscribirse: () -> Unit) {
    val cupoLleno = torneo.inscritos >= torneo.cupoMaximo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = torneo.nombre, fontWeight = FontWeight.Bold)
            Text(
                text = "${torneo.disciplina} · ${torneo.fechaInicio} a ${torneo.fechaFin}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { torneo.inscritos.toFloat() / torneo.cupoMaximo.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "${torneo.inscritos} / ${torneo.cupoMaximo}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onInscribirse,
                enabled = !cupoLleno,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(if (cupoLleno) "Cupo lleno" else "Inscribirme")
            }
        }
    }
}

private val disciplinasDisponibles = listOf("Fútbol", "Básquetbol", "Tenis", "Natación", "Voleibol")

@Composable
private fun ModalNuevoTorneo(
    areas: List<Area>,
    onCrear: (nombre: String, disciplina: String, areaId: String, fechaInicio: String, fechaFin: String, cupoMaximo: Int) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var disciplina by remember { mutableStateOf(disciplinasDisponibles.first()) }
    var area by remember { mutableStateOf(areas.firstOrNull()) }
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var cupoMaximo by remember { mutableStateOf("16") }
    var mostrarDropdownDisciplina by remember { mutableStateOf(false) }
    var mostrarDropdownArea by remember { mutableStateOf(false) }

    val puedeCrear = nombre.isNotBlank() && area != null && fechaInicio.isNotBlank() && fechaFin.isNotBlank() &&
        (cupoMaximo.toIntOrNull() ?: 0) > 0

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "+ Nuevo torneo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    IconButton(onClick = onCerrar) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("NOMBRE DEL TORNEO")
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Ej: Copa Otoño de Fútbol") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("DISCIPLINA")
                DropdownSeleccionable(
                    textoMostrado = disciplina,
                    expandido = mostrarDropdownDisciplina,
                    onExpandirCambiado = { mostrarDropdownDisciplina = it }
                ) {
                    disciplinasDisponibles.forEach { opcion ->
                        val seleccionado = disciplina == opcion
                        OpcionDropdown(
                            texto = "${estiloDisciplina(opcion).emoji} $opcion",
                            seleccionado = seleccionado,
                            onClick = {
                                disciplina = opcion
                                mostrarDropdownDisciplina = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("ÁREA")
                DropdownSeleccionable(
                    textoMostrado = area?.nombre ?: "Sin áreas disponibles",
                    expandido = mostrarDropdownArea,
                    onExpandirCambiado = { mostrarDropdownArea = it }
                ) {
                    areas.forEach { opcion ->
                        OpcionDropdown(
                            texto = opcion.nombre,
                            seleccionado = area?.id == opcion.id,
                            onClick = {
                                area = opcion
                                mostrarDropdownArea = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo("FECHA INICIO")
                        OutlinedTextField(
                            value = fechaInicio,
                            onValueChange = { fechaInicio = it },
                            placeholder = { Text("AAAA-MM-DD") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo("FECHA FIN")
                        OutlinedTextField(
                            value = fechaFin,
                            onValueChange = { fechaFin = it },
                            placeholder = { Text("AAAA-MM-DD") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("CUPO MÁXIMO")
                OutlinedTextField(
                    value = cupoMaximo,
                    onValueChange = { cupoMaximo = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val areaSeleccionada = area ?: return@Button
                        onCrear(nombre, disciplina, areaSeleccionada.id, fechaInicio, fechaFin, cupoMaximo.toIntOrNull() ?: 0)
                    },
                    enabled = puedeCrear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorVerde)
                ) {
                    Text(text = "Crear torneo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DropdownSeleccionable(
    textoMostrado: String,
    expandido: Boolean,
    onExpandirCambiado: (Boolean) -> Unit,
    opciones: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textoMostrado,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Text("▼") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onExpandirCambiado(!expandido) },
            color = Color.Transparent
        ) {}

        if (expandido) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) { opciones() }
            }
        }
    }
}

@Composable
private fun OpcionDropdown(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = if (seleccionado) Color(0xFFD1FAE5) else Color.White,
        onClick = onClick
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
            color = if (seleccionado) colorVerde else Color(0xFF111827),
            modifier = Modifier.padding(12.dp)
        )
    }
}
