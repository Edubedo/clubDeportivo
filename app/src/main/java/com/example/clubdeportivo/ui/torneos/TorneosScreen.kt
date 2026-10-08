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
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.IconoDesplegable
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val rolesDeAdministracion = setOf(Rol.SUPERADMIN, Rol.ADMIN, Rol.ADMIN_AREA, Rol.AYUDANTE_AREA)

@Composable
fun TorneosScreen(viewModel: TorneosViewModel = viewModel()) {
    val torneos by viewModel.torneos.observeAsState(emptyList())
    val areas by viewModel.areas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()
    val mostrarModalCrear by viewModel.mostrarModalCrear.observeAsState(false)
    val torneoEnEdicion by viewModel.torneoEnEdicion.observeAsState()
    val inscritos by viewModel.inscritos.observeAsState(emptySet())
    val errorFormulario by viewModel.errorFormulario.observeAsState()
    var torneoPorEliminar by remember { mutableStateOf<Torneo?>(null) }

    val esAdministrador = SesionManager.usuarioActual?.rol in rolesDeAdministracion

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FondoApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (esAdministrador) {
                BotonFlotanteAgregar(
                    texto = "Nuevo torneo",
                    visible = listState.botonFlotanteVisible(),
                    onClick = { viewModel.abrirModalCrear() }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                cargando && torneos.isEmpty() -> FullScreenLoading(modifier = Modifier.fillMaxSize())
                torneos.isEmpty() && !esAdministrador -> EmptyState(
                    mensaje = "No hay torneos disponibles.",
                    icono = Icons.Filled.EmojiEvents
                )
                esAdministrador -> TorneosAdminContent(
                    torneos = torneos,
                    listState = listState,
                    onEditar = { viewModel.abrirModalEditar(it) },
                    onEliminar = { torneoPorEliminar = it }
                )
                else -> TorneosSocioContent(
                    // Un torneo que ya terminó no admite inscripciones: no se muestra.
                    torneos = torneos.filter { it.fechaFin >= Fechas.hoy() },
                    inscritos = inscritos,
                    onInscribirse = { viewModel.inscribirse(it) }
                )
            }
        }
    }

    torneoPorEliminar?.let { torneo ->
        DialogoConfirmacion(
            titulo = "Eliminar torneo",
            mensaje = "Se eliminará \"${torneo.nombre}\"" +
                (if (torneo.inscritos > 0) " y sus ${torneo.inscritos} inscripciones" else "") +
                ". El área quedará libre para reservas. Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminarTorneo(torneo)
                torneoPorEliminar = null
            },
            onCancelar = { torneoPorEliminar = null }
        )
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

@Composable
private fun TorneosAdminContent(
    torneos: List<Torneo>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onEditar: (Torneo) -> Unit,
    onEliminar: (Torneo) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoPantalla(titulo = "Torneos", modifier = Modifier.padding(horizontal = MargenPantalla))

        if (torneos.isEmpty()) {
            EmptyState(
                mensaje = "No hay torneos todavía. Crea el primero con \"Nuevo torneo\".",
                icono = Icons.Filled.EmojiEvents
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(torneos, key = { it.id }) { torneo ->
                    TorneoCardAdmin(torneo = torneo, onEditar = { onEditar(torneo) }, onEliminar = { onEliminar(torneo) })
                }
            }
        }
    }
}

@Composable
private fun TorneoCardAdmin(torneo: Torneo, onEditar: () -> Unit, onEliminar: () -> Unit) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = torneo.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                EtiquetaDisciplina(disciplina = torneo.disciplina)
                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar torneo",
                        tint = TextoSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onEliminar,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar torneo",
                        tint = Peligro,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DatosTorneo(torneo)
        }
    }
}

/** Fechas, cupo y barra de avance de inscripciones; igual en la vista del personal y en la del socio. */
@Composable
private fun DatosTorneo(torneo: Torneo) {
    val libres = (torneo.cupoMaximo - torneo.inscritos).coerceAtLeast(0)
    val progreso = if (torneo.cupoMaximo > 0) torneo.inscritos.toFloat() / torneo.cupoMaximo.toFloat() else 0f

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = textoRangoFecha(torneo),
        style = MaterialTheme.typography.bodySmall,
        color = TextoSecundario
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${torneo.inscritos}/${torneo.cupoMaximo} participantes",
            style = MaterialTheme.typography.labelMedium,
            color = TextoPrincipal,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = "$libres libres",
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Marca
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { progreso },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = Marca,
        trackColor = Borde
    )
}

private fun textoRangoFecha(torneo: Torneo): String {
    val fechas = if (torneo.fechaInicio == torneo.fechaFin) Fechas.legible(torneo.fechaInicio)
    else "${Fechas.legible(torneo.fechaInicio)} – ${Fechas.legible(torneo.fechaFin)}"
    val horas = if (torneo.horaInicio.isNotBlank() && torneo.horaFin.isNotBlank()) " · ${torneo.horaInicio}–${torneo.horaFin}" else ""
    return fechas + horas
}

/** Todas las disciplinas se ven igual: lo que las distingue es el emoji, no el color. */
@Composable
private fun EtiquetaDisciplina(disciplina: String) {
    Insignia(texto = "${Deportes.emojiDe(disciplina)} $disciplina", tipo = TipoInsignia.MARCA)
}

@Composable
private fun TorneosSocioContent(
    torneos: List<Torneo>,
    inscritos: Set<String>,
    modifier: Modifier = Modifier,
    onInscribirse: (Torneo) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(torneos, key = { it.id }) { torneo ->
            TorneoCardSocio(torneo = torneo, inscrito = torneo.id in inscritos, onInscribirse = { onInscribirse(torneo) })
        }
    }
}

@Composable
private fun TorneoCardSocio(torneo: Torneo, inscrito: Boolean, onInscribirse: () -> Unit) {
    val cupoLleno = torneo.inscritos >= torneo.cupoMaximo

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = torneo.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                EtiquetaDisciplina(disciplina = torneo.disciplina)
            }

            DatosTorneo(torneo)

            Spacer(modifier = Modifier.height(16.dp))
            BotonPrimario(
                texto = when {
                    inscrito -> "Ya estás inscrito"
                    cupoLleno -> "Cupo lleno"
                    else -> "Inscribirme"
                },
                enabled = !cupoLleno && !inscrito,
                onClick = onInscribirse
            )
        }
    }
}


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
    val areaInicial = areas.find { it.id == torneoExistente?.areaId }
    // Los deportes de las áreas del club (incluidos los que se dieron de alta después) más los de siempre.
    val disciplinasDisponibles = remember(areas) { (Deportes.predefinidos + areas.map { it.tipo.trim() }).filter { it.isNotBlank() }.distinct() }
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
    ) {
        Column {
            EtiquetaCampo("NOMBRE DEL TORNEO")
            CampoTexto(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = "Ej: Copa Otoño de Fútbol",
                capitalizacion = KeyboardCapitalization.Sentences
            )

            Spacer(modifier = Modifier.height(16.dp))

            EtiquetaCampo("DISCIPLINA")
            DropdownSeleccionable(
                textoMostrado = disciplina,
                expandido = mostrarDropdownDisciplina,
                onExpandirCambiado = { mostrarDropdownDisciplina = it }
            ) {
                disciplinasDisponibles.forEach { opcion ->
                    val seleccionado = disciplina == opcion
                    OpcionDropdown(
                        texto = "${Deportes.emojiDe(opcion)} $opcion",
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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            EtiquetaCampo("CUPO MÁXIMO")
            CampoTexto(
                value = cupoMaximo,
                onValueChange = { cupoMaximo = it.filter { c -> c.isDigit() }.take(4) },
                tipoTeclado = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        }
    }
}

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
                Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha", tint = TextoSecundario)
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
                    Text("Aceptar", color = Marca, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

private val horasClub = (5..22).toList()

private fun minutosPara(hora: Int): List<Int> = if (hora >= 22) listOf(0) else (0..55 step 5).toList()

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
                Icon(imageVector = Icons.Filled.Schedule, contentDescription = "Elegir hora", tint = TextoSecundario)
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
            Surface(shape = RoundedCornerShape(20.dp), color = Superficie) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .width(240.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Elegir hora", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                    Text(
                        text = "Horario del club: 5:00 a. m. – 10:00 p. m.",
                        fontSize = 12.sp,
                        color = TextoSecundario,
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
                            color = TextoPrincipal
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
                            Text("Cancelar", color = TextoSecundario)
                        }
                        TextButton(onClick = {
                            onHoraSeleccionada(partesAHora(horaElegida, minutoElegido))
                            mostrarSelector = false
                        }) {
                            Text("Aceptar", color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

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
                .background(MarcaSuave, RoundedCornerShape(10.dp))
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
                        fontSize = if (seleccionado) 18.sp else 16.sp,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (seleccionado) TextoPrincipal else TextoSecundario
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
            trailingIcon = { IconoDesplegable() }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onExpandirCambiado(!expandido) },
            color = Color.Transparent
        ) {}

        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { onExpandirCambiado(false) },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .heightIn(max = 320.dp),
            containerColor = Superficie
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
        color = if (seleccionado) MarcaSuave else Superficie,
        onClick = onClick
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
            color = if (seleccionado) Marca else TextoPrincipal,
            modifier = Modifier.padding(12.dp)
        )
    }
}