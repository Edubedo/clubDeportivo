package com.example.clubdeportivo.ui.torneos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    val torneoEnEdicion by viewModel.torneoEnEdicion.observeAsState()
    val errorFormulario by viewModel.errorFormulario.observeAsState()

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            esAdministrador -> TorneosAdminContent(
                torneos = torneos,
                modifier = Modifier.padding(innerPadding),
                onEditar = { viewModel.abrirModalEditar(it) }
            )
            else -> TorneosSocioContent(
                torneos = torneos,
                modifier = Modifier.padding(innerPadding),
                onInscribirse = { viewModel.inscribirse(it) }
            )
        }
    }

    if (mostrarModalCrear) {
        ModalTorneo(
            areas = areas,
            torneoExistente = torneoEnEdicion,
            error = errorFormulario,
            onGuardar = { id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, horaInicio, horaFin ->
                viewModel.guardarTorneo(id, nombre, disciplina, areaId, fechaInicio, fechaFin, cupoMaximo, horaInicio, horaFin)
            },
            onCerrar = { viewModel.cerrarModalCrear() }
        )
    }
}

/** Vista de administración: encabezado de marca + tarjetas con cupo y avance, sin botón de inscripción. */
@Composable
private fun TorneosAdminContent(torneos: List<Torneo>, modifier: Modifier = Modifier, onEditar: (Torneo) -> Unit) {
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
                items(torneos, key = { it.id }) { torneo ->
                    TorneoCardAdmin(torneo = torneo, onEditar = { onEditar(torneo) })
                }
            }
        }
    }
}

@Composable
private fun TorneoCardAdmin(torneo: Torneo, onEditar: () -> Unit) {
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
                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar torneo",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = textoRangoFecha(torneo),
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
                    color = Color(0xFF374151),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "$libres libres",
                    maxLines = 1,
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

/**
 * "2026-10-05 – 2026-10-20 · 09:00–18:00": el torneo ocupa el área en ese horario CADA día del rango.
 * Sin horas (torneos creados antes de este campo) se muestra solo el rango de fechas.
 */
private fun textoRangoFecha(torneo: Torneo): String {
    val fechas = if (torneo.fechaInicio == torneo.fechaFin) torneo.fechaInicio else "${torneo.fechaInicio} – ${torneo.fechaFin}"
    val horas = if (torneo.horaInicio.isNotBlank() && torneo.horaFin.isNotBlank()) " · ${torneo.horaInicio}–${torneo.horaFin}" else ""
    return fechas + horas
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
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
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

private val disciplinasDisponibles = Deportes.predefinidos

private fun manianaEnMillis(): Long =
    LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun fechaAMillis(fecha: String): Long? = try {
    LocalDate.parse(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (e: Exception) {
    null
}

private fun millisAFecha(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalTorneo(
    areas: List<Area>,
    torneoExistente: Torneo?,
    error: String?,
    onGuardar: (id: String?, nombre: String, disciplina: String, areaId: String, fechaInicio: String, fechaFin: String, cupoMaximo: Int, horaInicio: String, horaFin: String) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = torneoExistente != null

    var nombre by remember { mutableStateOf(torneoExistente?.nombre ?: "") }
    // La disciplina y el área van ligadas: solo se ofrecen las áreas del deporte elegido.
    val areaInicial = areas.find { it.id == torneoExistente?.areaId }
    var disciplina by remember {
        mutableStateOf(areaInicial?.tipo ?: torneoExistente?.disciplina ?: disciplinasDisponibles.first())
    }
    var area by remember {
        mutableStateOf(areaInicial ?: areas.firstOrNull { it.tipo == disciplina })
    }
    val areasDeLaDisciplina = areas.filter { it.tipo == disciplina }
    var fechaInicio by remember { mutableStateOf(torneoExistente?.fechaInicio ?: "") }
    var fechaFin by remember { mutableStateOf(torneoExistente?.fechaFin ?: "") }
    var horaInicio by remember { mutableStateOf(torneoExistente?.horaInicio?.ifBlank { "09:00" } ?: "09:00") }
    var horaFin by remember { mutableStateOf(torneoExistente?.horaFin?.ifBlank { "18:00" } ?: "18:00") }
    var cupoMaximo by remember { mutableStateOf((torneoExistente?.cupoMaximo ?: 16).toString()) }
    var mostrarDropdownDisciplina by remember { mutableStateOf(false) }
    var mostrarDropdownArea by remember { mutableStateOf(false) }

    val puedeGuardar = nombre.isNotBlank() && area != null && fechaInicio.isNotBlank() && fechaFin.isNotBlank() &&
        horaInicio.isNotBlank() && horaFin.isNotBlank() && (cupoMaximo.toIntOrNull() ?: 0) > 0

    DialogoFormulario(
        titulo = if (esEdicion) "Editar torneo" else "Nuevo torneo",
        onCerrar = onCerrar
    ) {
        Column {
                EtiquetaCampo("NOMBRE DEL TORNEO")
                CampoTexto(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Ej: Copa Otoño de Fútbol"
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
                                if (opcion != disciplina) {
                                    disciplina = opcion
                                    area = areas.firstOrNull { it.tipo == opcion }
                                }
                                mostrarDropdownDisciplina = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("ÁREA")
                DropdownSeleccionable(
                    textoMostrado = area?.nombre ?: "Sin áreas de este deporte",
                    expandido = mostrarDropdownArea,
                    onExpandirCambiado = { mostrarDropdownArea = it }
                ) {
                    areasDeLaDisciplina.forEach { opcion ->
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
                        CampoFecha(fecha = fechaInicio, onFechaSeleccionada = { fechaInicio = it })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo("FECHA FIN")
                        CampoFecha(fecha = fechaFin, onFechaSeleccionada = { fechaFin = it })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo("HORA INICIO")
                        CampoHora(hora = horaInicio, onHoraSeleccionada = { horaInicio = it })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo("HORA FIN")
                        CampoHora(hora = horaFin, onHoraSeleccionada = { horaFin = it })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("CUPO MÁXIMO")
                CampoTexto(
                    value = cupoMaximo,
                    onValueChange = { cupoMaximo = it.filter { c -> c.isDigit() }.take(4) },
                    tipoTeclado = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                BotonPrimario(
                    texto = if (esEdicion) "Guardar cambios" else "Crear torneo",
                    onClick = {
                        val areaSeleccionada = area ?: return@BotonPrimario
                        onGuardar(
                            torneoExistente?.id,
                            nombre,
                            disciplina,
                            areaSeleccionada.id,
                            fechaInicio,
                            fechaFin,
                            cupoMaximo.toIntOrNull() ?: 0,
                            horaInicio,
                            horaFin
                        )
                    },
                    enabled = puedeGuardar
                )
        }
    }
}

/** Campo de fecha de solo lectura que abre un calendario; no permite elegir hoy ni fechas pasadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(fecha: String, onFechaSeleccionada: (String) -> Unit) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        CampoTexto(
            value = fecha,
            onValueChange = {},
            readOnly = true,
            placeholder = "Elegir",
            trailingIcon = {
                Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha", tint = Color(0xFF94A3B8))
            }
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
            initialSelectedDateMillis = fechaAMillis(fecha) ?: manianaEnMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= manianaEnMillis()
            }
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

/** Horario del club: de 5:00 a.m. a 10:00 p.m. */
private val horasClub = (5..22).toList()

/** A la hora de cierre (22) solo se permite el minuto en punto. */
private fun minutosPara(hora: Int): List<Int> = if (hora >= 22) listOf(0) else (0..55 step 5).toList()

/** Campo de hora de solo lectura que abre un selector de ruedas (como poner una alarma), limitado al horario del club. */
@Composable
private fun CampoHora(hora: String, onHoraSeleccionada: (String) -> Unit) {
    var mostrarSelector by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        CampoTexto(
            value = hora,
            onValueChange = {},
            readOnly = true,
            placeholder = "Elegir",
            trailingIcon = {
                Icon(imageVector = Icons.Filled.Schedule, contentDescription = "Elegir hora", tint = Color(0xFF94A3B8))
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { mostrarSelector = true },
            color = Color.Transparent
        ) {}
    }

    if (mostrarSelector) {
        val (horaInicial, minutoInicial) = horaAPartes(hora)
        var horaElegida by remember { mutableStateOf(horaInicial.coerceIn(5, 22)) }
        var minutoElegido by remember { mutableStateOf(minutoInicial) }

        Dialog(onDismissRequest = { mostrarSelector = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .width(240.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Elegir hora", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text(
                        text = "Horario del club: 5:00 a. m. – 10:00 p. m.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RuedaTiempo(
                            valores = horasClub,
                            valorSeleccionado = horaElegida,
                            onValorSeleccionado = { nuevaHora ->
                                horaElegida = nuevaHora
                                if (minutoElegido !in minutosPara(nuevaHora)) minutoElegido = 0
                            },
                            formatear = { "%02d".format(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ":",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        key(horaElegida >= 22) {
                            RuedaTiempo(
                                valores = minutosPara(horaElegida),
                                valorSeleccionado = minutoElegido,
                                onValorSeleccionado = { minutoElegido = it },
                                formatear = { "%02d".format(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { mostrarSelector = false }) {
                            Text("Cancelar", color = Color(0xFF94A3B8))
                        }
                        TextButton(onClick = {
                            onHoraSeleccionada(partesAHora(horaElegida, minutoElegido))
                            mostrarSelector = false
                        }) {
                            Text("Aceptar", color = colorVerde, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/** Columna deslizable estilo "poner una alarma": el valor centrado (resaltado) es el elegido. */
@Composable
private fun RuedaTiempo(
    valores: List<Int>,
    valorSeleccionado: Int,
    onValorSeleccionado: (Int) -> Unit,
    formatear: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val alturaItem = 40.dp
    val indiceInicial = valores.indexOf(valorSeleccionado).coerceAtLeast(0)
    val estadoLista = rememberLazyListState(indiceInicial)
    val flingBehavior = rememberSnapFlingBehavior(estadoLista)

    LaunchedEffect(estadoLista, valores) {
        snapshotFlow {
            val info = estadoLista.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@snapshotFlow null
            val centro = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - centro) }?.index
        }.collect { indice ->
            val valor = indice?.let { valores.getOrNull(it) }
            if (valor != null && valor != valorSeleccionado) onValorSeleccionado(valor)
        }
    }

    Box(modifier = modifier.height(alturaItem * 3), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(alturaItem)
                .background(Color(0xFFD1FAE5), RoundedCornerShape(10.dp))
        )
        LazyColumn(
            state = estadoLista,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = alturaItem),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(valores) { _, valor ->
                val seleccionado = valor == valorSeleccionado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(alturaItem),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatear(valor),
                        fontSize = if (seleccionado) 20.sp else 16.sp,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (seleccionado) Color(0xFF111827) else Color(0xFFB0B9C6)
                    )
                }
            }
        }
    }
}

private fun horaAPartes(hora: String): Pair<Int, Int> {
    val partes = hora.split(":")
    val horaValor = (partes.getOrNull(0)?.toIntOrNull() ?: 9).coerceIn(5, 22)
    val minutoValor = partes.getOrNull(1)?.toIntOrNull() ?: 0
    return horaValor to minutoValor
}

private fun partesAHora(hora: Int, minuto: Int): String = "%02d:%02d".format(hora, minuto)

@Composable
private fun DropdownSeleccionable(
    textoMostrado: String,
    expandido: Boolean,
    onExpandirCambiado: (Boolean) -> Unit,
    opciones: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CampoTexto(
            value = textoMostrado,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Text("▼") }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onExpandirCambiado(!expandido) },
            color = Color.Transparent
        ) {}

        // Menú emergente (no una lista dentro del diálogo): no se corta y hace scroll si hay muchas áreas.
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { onExpandirCambiado(false) },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .heightIn(max = 320.dp),
            containerColor = Color.White
        ) {
            opciones()
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
