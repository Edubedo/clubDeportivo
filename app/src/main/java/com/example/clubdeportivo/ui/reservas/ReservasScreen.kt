package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca
import com.example.clubdeportivo.ui.torneos.TorneosScreen

private val FondoPantalla = Color(0xFFF8FAFD)
private val TextoTitulo = Color(0xFF111827)
private val TextoSuave = Color(0xFF64748B)
private val ColorLleno = Color(0xFFFEE2E2)
private val ColorTorneo = Color(0xFFEDE9FE)
private val ColorPocos = Color(0xFFFEF3C7)
private val ColorLibre = Color(0xFFF1F5F9)
private val ColorPasada = Color(0xFFF8FAFC)

/** Sección "Reservas": reservar un espacio o reservar un área para un torneo. */
@Composable
fun ReservasScreen() {
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(FondoPantalla)) {
        PestanasPildora(
            opciones = listOf("Espacio", "Torneo"),
            seleccionada = pestana,
            onSeleccion = { pestana = it }
        )

        when (pestana) {
            0 -> ReservarEspacioScreen()
            1 -> TorneosScreen()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReservarEspacioScreen(viewModel: ReservasViewModel = viewModel()) {
    val ui by viewModel.ui.observeAsState(ReservaEspacioUi())
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val mensaje by viewModel.mensaje.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FondoPantalla,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (ui.cargando) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val deportes = ui.areas.map { it.tipo }.distinct()
        val areasDelDeporte = ui.areas.filter { it.tipo == ui.deporte }
        val paso = when {
            ui.deporte == null -> 1
            ui.area == null -> 2
            else -> 3
        }

        // Al elegir deporte o cancha la pantalla baja sola a la siguiente sección (posición medida en el contenido).
        val scroll = rememberScrollState()
        var canchaY by remember { mutableIntStateOf(0) }
        var horarioY by remember { mutableIntStateOf(0) }
        val margenSuperior = with(LocalDensity.current) { 8.dp.roundToPx() }

        LaunchedEffect(ui.deporte) {
            if (ui.deporte != null) {
                withFrameNanos { }
                scroll.animateScrollTo((canchaY - margenSuperior).coerceAtLeast(0))
            }
        }
        LaunchedEffect(ui.area?.id) {
            if (ui.area != null) {
                withFrameNanos { }
                scroll.animateScrollTo((horarioY - margenSuperior).coerceAtLeast(0))
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
        ) {
            Text("Reservar un espacio", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoTitulo)
            Spacer(modifier = Modifier.height(12.dp))
            Pasos(pasoActual = paso)
            Spacer(modifier = Modifier.height(20.dp))

            // 1. Deporte
            Seccion("SELECCIONA EL DEPORTE")
            if (deportes.isEmpty()) {
                Text("Todavía no hay áreas registradas.", color = TextoSuave, fontSize = 14.sp)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                deportes.forEach { deporte ->
                    val muestra = ui.areas.first { it.tipo == deporte }
                    TarjetaOpcion(
                        emoji = Deportes.emojiDe(deporte, muestra.emoji),
                        titulo = deporte,
                        detalle = ui.areas.count { it.tipo == deporte }.let { if (it == 1) "1 área" else "$it áreas" },
                        seleccionada = deporte == ui.deporte,
                        habilitada = true,
                        onClick = { viewModel.elegirDeporte(deporte) }
                    )
                }
            }

            // 2. Cancha
            if (ui.deporte != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.onGloballyPositioned { canchaY = it.positionInParent().y.toInt() }) {
                Seccion("SELECCIONA LA CANCHA")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    areasDelDeporte.forEach { area ->
                        val bloqueada = area.disponibilidad == DisponibilidadArea.MANTENIMIENTO ||
                            (viewModel.esVisitanteExterno && !area.permiteExternos)
                        TarjetaOpcion(
                            emoji = Deportes.emojiDe(area.tipo, area.emoji),
                            titulo = area.nombre,
                            detalle = when {
                                area.disponibilidad == DisponibilidadArea.MANTENIMIENTO -> "En mantenimiento"
                                bloqueada -> "No admite externos"
                                else -> "Cupo ${area.capacidad}"
                            },
                            seleccionada = area.id == ui.area?.id,
                            habilitada = !bloqueada,
                            onClick = { viewModel.elegirArea(area) }
                        )
                    }
                }
                }
            }

            // 3. Horario
            ui.area?.let { area ->
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.onGloballyPositioned { horarioY = it.positionInParent().y.toInt() }) {
                    SeccionHorario(ui = ui, area = area, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Reservas vigentes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoTitulo)
            Spacer(modifier = Modifier.height(10.dp))
            if (reservas.isEmpty()) {
                Text("No hay reservas próximas.", color = TextoSuave, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reservas.forEach { reserva ->
                        TarjetaReserva(reserva = reserva, onCancelar = { viewModel.cancelar(reserva) })
                    }
                }
            }
            // Espacio extra para que, al bajar a la siguiente sección, esta pueda llegar hasta arriba de la pantalla.
            Spacer(modifier = Modifier.height(if (ui.deporte != null) 360.dp else 24.dp))
        }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoSuave,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun Pasos(pasoActual: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("Deporte", "Cancha", "Horario").forEachIndexed { indice, nombre ->
            val numero = indice + 1
            val activo = numero <= pasoActual
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (activo) VerdeMarca else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(numero.toString(), color = if (activo) Color.White else TextoSuave, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = nombre,
                fontSize = 13.sp,
                fontWeight = if (numero == pasoActual) FontWeight.Bold else FontWeight.Normal,
                color = if (activo) TextoTitulo else TextoSuave,
                modifier = Modifier.padding(start = 6.dp, end = 10.dp)
            )
            if (numero < 3) {
                Box(modifier = Modifier.width(16.dp).height(1.dp).background(Color(0xFFCBD5E1)))
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(
    emoji: String,
    titulo: String,
    detalle: String,
    seleccionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = habilitada,
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (habilitada) Color.White else Color(0xFFF1F5F9),
        border = BorderStroke(if (seleccionada) 2.dp else 1.dp, if (seleccionada) VerdeMarca else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(emoji, fontSize = 26.sp)
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (habilitada) TextoTitulo else TextoSuave,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(detalle, fontSize = 12.sp, color = TextoSuave)
        }
    }
}

private fun etiquetaHora(hora: Int): Pair<String, String> {
    val doce = if (hora % 12 == 0) 12 else hora % 12
    return doce.toString() to if (hora < 12) "a" else "p"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionHorario(ui: ReservaEspacioUi, area: Area, viewModel: ReservasViewModel) {
    Seccion("HORARIO")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("FECHA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSuave)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.fechas.forEach { (fecha, etiqueta) ->
                    val activa = fecha == ui.fecha
                    Surface(
                        onClick = { viewModel.elegirFecha(fecha) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activa) VerdeMarca else ColorLibre
                    ) {
                        Text(
                            text = etiqueta,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activa) Color.White else TextoSuave,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Text(ui.horarioTexto, fontSize = 13.sp, color = TextoSuave, modifier = Modifier.padding(top = 14.dp))

            // Resumen de la selección
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val desde = ui.desde
                val hasta = ui.hasta
                Text(
                    text = if (desde != null && hasta != null) {
                        "%02d:00 → %02d:00 (%dh)".format(desde, hasta + 1, ui.horasElegidas)
                    } else {
                        "Toca las horas que quieres reservar"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoTitulo,
                    modifier = Modifier.weight(1f)
                )
                if (desde != null) {
                    TextButton(onClick = { viewModel.limpiarSeleccion() }) {
                        Text("Limpiar", color = TextoSuave)
                    }
                }
            }

            if (ui.horas.isEmpty()) {
                Text(
                    "No hay horarios disponibles ese día.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                val columnas = 6
                val relleno = (columnas - ui.horas.size % columnas) % columnas
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    maxItemsInEachRow = columnas,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ui.horas.forEach { celda ->
                        val elegida = ui.desde != null && ui.hasta != null && celda.hora in ui.desde..ui.hasta
                        CeldaHora(celda = celda, elegida = elegida, modifier = Modifier.weight(1f), onClick = { viewModel.tocarHora(celda.hora) })
                    }
                    repeat(relleno) { Spacer(modifier = Modifier.weight(1f)) }
                }

                Leyenda()
            }

            if (ui.desde != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PERSONAS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSuave)
                        Text(
                            "Lugares libres: ${ui.maxPersonas} de ${area.capacidad}",
                            fontSize = 12.sp,
                            color = TextoSuave
                        )
                    }
                    ContadorPersonas(
                        valor = ui.personas,
                        puedeBajar = ui.personas > 1,
                        puedeSubir = ui.personas < ui.maxPersonas,
                        onMenos = { viewModel.cambiarPersonas(-1) },
                        onMas = { viewModel.cambiarPersonas(1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            BotonPrimario(
                texto = "Confirmar reserva",
                enabled = ui.desde != null && ui.hasta != null,
                cargando = ui.enviando,
                onClick = { viewModel.confirmar() }
            )
        }
    }
}

@Composable
private fun CeldaHora(celda: HoraUi, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val estado = celda.estado
    val parcial = estado.ocupadas > 0 && !estado.lleno
    val fondo = when {
        elegida -> VerdeMarca
        estado.torneo != null -> ColorTorneo
        estado.lleno -> ColorLleno
        celda.pasada -> ColorPasada
        parcial -> ColorPocos
        else -> ColorLibre
    }
    val texto = when {
        elegida -> Color.White
        estado.torneo != null -> Color(0xFF7C3AED)
        estado.lleno -> Color(0xFFDC2626)
        celda.pasada -> Color(0xFFCBD5E1)
        else -> TextoTitulo
    }
    val (numero, sufijo) = etiquetaHora(celda.hora)
    Surface(
        onClick = onClick,
        enabled = celda.seleccionable,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        color = fondo
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(numero, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = texto, maxLines = 1)
            Text(sufijo, fontSize = 10.sp, color = texto, maxLines = 1)
        }
    }
}

@Composable
private fun Leyenda() {
    FlowRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            "Disponible" to ColorLibre,
            "Pocos lugares" to ColorPocos,
            "Cupo lleno" to ColorLleno,
            "Torneo" to ColorTorneo
        ).forEach { (nombre, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                        .padding(1.dp)
                )
                Text(nombre, fontSize = 12.sp, color = TextoSuave, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun ContadorPersonas(valor: Int, puedeBajar: Boolean, puedeSubir: Boolean, onMenos: () -> Unit, onMas: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonCirculo("−", puedeBajar, onMenos)
        Text(
            text = valor.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoTitulo,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp)
        )
        BotonCirculo("+", puedeSubir, onMas)
    }
}

@Composable
private fun BotonCirculo(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = ColorLibre
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texto, fontSize = 20.sp, color = if (habilitado) TextoSuave else Color(0xFFD1D5DB))
        }
    }
}

@Composable
private fun TarjetaReserva(reserva: ReservaListada, onCancelar: () -> Unit) {
    val pendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, if (pendiente) Color(0xFFFDE68A) else Color(0xFFBBF7D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reserva.emoji, fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        reserva.titulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoTitulo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${reserva.fecha} · ${reserva.horaInicio}–${reserva.horaFin}",
                        fontSize = 13.sp,
                        color = TextoSuave
                    )
                    Text(
                        if (reserva.personas == 1) "1 persona" else "${reserva.personas} personas",
                        fontSize = 12.sp,
                        color = TextoSuave
                    )
                }
                Text(
                    text = if (pendiente) "Pendiente" else "Confirmada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pendiente) Color(0xFFD97706) else Color(0xFF16A34A),
                    modifier = Modifier
                        .background(if (pendiente) Color(0xFFFEF3C7) else Color(0xFFDCFCE7), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Text(
                text = "Cancelar reserva",
                fontSize = 13.sp,
                color = TextoSuave,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { onCancelar() }
            )
        }
    }
}
