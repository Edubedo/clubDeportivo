package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esPersonal
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.BotonIcono
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TituloSeccion
import com.example.clubdeportivo.ui.theme.Alerta
import com.example.clubdeportivo.ui.theme.AlertaSuave
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PeligroSuave
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.TextoTenue
import com.example.clubdeportivo.ui.torneos.TorneosScreen
import com.example.clubdeportivo.util.Fechas

@Composable
fun ReservasScreen() {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    // Solo el personal aprueba las reservas de visitantes.
    val puedeAprobar = SesionManager.usuarioActual?.rol?.esPersonal() == true
    val opciones = if (puedeAprobar) listOf("Espacio", "Torneo", "Por aprobar") else listOf("Espacio", "Torneo")

    Column(modifier = Modifier.fillMaxSize().background(FondoApp)) {
        PestanasPildora(
            opciones = opciones,
            seleccionada = pestana.coerceAtMost(opciones.lastIndex),
            onSeleccion = { pestana = it }
        )

        when (pestana) {
            0 -> ReservarEspacioScreen()
            1 -> TorneosScreen()
            else -> PorAprobarScreen()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReservarEspacioScreen(viewModel: ReservasViewModel = viewModel()) {
    val ui by viewModel.ui.observeAsState(ReservaEspacioUi())
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val mensaje by viewModel.mensaje.observeAsState()
    val restriccion by viewModel.restriccion.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FondoApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val area = ui.area
            if (!ui.cargando && area != null) {
                BarraConfirmacion(ui = ui, viewModel = viewModel)
            }
        }
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
                .padding(horizontal = MargenPantalla)
        ) {
            EncabezadoPantalla(titulo = "Reservar un espacio")
            restriccion?.let { motivo ->
                TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = AlertaSuave) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Alerta, modifier = Modifier.size(20.dp))
                        Text(text = motivo, style = MaterialTheme.typography.bodyMedium, color = Alerta)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Pasos(pasoActual = paso)
            Spacer(modifier = Modifier.height(20.dp))

            Seccion("SELECCIONA EL DEPORTE")
            if (deportes.isEmpty()) {
                Text(
                    "Todavía no hay áreas registradas.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Cuadrícula de deportes: 2 columnas simétricas
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                deportes.forEach { deporte ->
                    val muestra = ui.areas.first { it.tipo == deporte }
                    Box(modifier = Modifier.weight(1f)) {
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
            }

            if (ui.deporte != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.onGloballyPositioned { canchaY = it.positionInParent().y.toInt() }) {
                    Seccion("SELECCIONA LA CANCHA")

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        areasDelDeporte.forEach { area ->
                            val bloqueada = area.disponibilidad == DisponibilidadArea.MANTENIMIENTO ||
                                    (viewModel.esVisitanteExterno && !area.permiteExternos)
                            Box(modifier = Modifier.weight(1f)) {
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
            }

            ui.area?.let { area ->
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.onGloballyPositioned { horarioY = it.positionInParent().y.toInt() }) {
                    SeccionHorario(ui = ui, area = area, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            TituloSeccion("Reservas vigentes")
            Spacer(modifier = Modifier.height(10.dp))
            if (reservas.isEmpty()) {
                Text(
                    "No hay reservas próximas.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reservas.forEach { reserva ->
                        TarjetaReservaCliente(reserva = reserva, onCancelar = { viewModel.cancelar(reserva) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoSecundario,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** Indicador de avance: los pasos hechos llevan una palomita, el actual va resaltado y los que faltan en gris. */
@Composable
private fun Pasos(pasoActual: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        listOf("Deporte", "Cancha", "Horario").forEachIndexed { indice, nombre ->
            val numero = indice + 1
            val hecho = numero < pasoActual
            val actual = numero == pasoActual
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (hecho || actual) Marca else Borde),
                contentAlignment = Alignment.Center
            ) {
                if (hecho) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = SobreMarca, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = numero.toString(),
                        color = if (actual) SobreMarca else TextoTenue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = nombre,
                fontSize = 12.sp,
                fontWeight = if (actual) FontWeight.Bold else FontWeight.Medium,
                color = if (hecho || actual) TextoPrincipal else TextoTenue,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp)
            )
            if (numero < 3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(1.dp)
                        .background(if (hecho) Marca else BordeCampo)
                )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = when {
            !habilitada -> FondoApp
            seleccionada -> MarcaSuave
            else -> Superficie
        },
        border = BorderStroke(if (seleccionada) 2.dp else 1.dp, if (seleccionada) Marca else Borde)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 28.sp)
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = if (habilitada) TextoPrincipal else TextoTenue,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = if (habilitada) TextoSecundario else TextoTenue
            )
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
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "FECHA",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoSecundario,
                letterSpacing = 0.4.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.fechas.forEach { (fecha, etiqueta) ->
                    val activa = fecha == ui.fecha
                    Surface(
                        onClick = { viewModel.elegirFecha(fecha) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activa) Marca else Superficie,
                        border = if (activa) null else BorderStroke(1.dp, BordeCampo)
                    ) {
                        Text(
                            text = etiqueta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activa) SobreMarca else TextoSecundario,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Text(
                ui.horarioTexto,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 14.dp)
            )

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
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                if (desde != null) {
                    TextButton(onClick = { viewModel.limpiarSeleccion() }) {
                        Text("Limpiar", color = Marca, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (ui.horas.isEmpty()) {
                Text(
                    if (ui.errorDisponibilidad) "No se pudo consultar la disponibilidad. Elige la fecha otra vez para reintentar."
                    else "No hay horarios disponibles ese día.",
                    color = Peligro,
                    style = MaterialTheme.typography.bodyMedium,
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
                        Text(
                            "PERSONAS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoSecundario,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            "Lugares libres: ${ui.maxPersonas} de ${area.capacidad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
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

        }
    }
}

/** Barra fija con el resumen de lo elegido y el botón de confirmar, siempre a la vista. */
@Composable
private fun BarraConfirmacion(ui: ReservaEspacioUi, viewModel: ReservasViewModel) {
    val desde = ui.desde
    val hasta = ui.hasta
    val listo = desde != null && hasta != null
    val etiquetaFecha = viewModel.fechas.firstOrNull { it.first == ui.fecha }?.second ?: Fechas.legible(ui.fecha)

    Surface(color = Superficie, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(start = MargenPantalla, end = MargenPantalla, top = 12.dp, bottom = 12.dp)) {
            Text(
                text = if (listo) {
                    "$etiquetaFecha · %02d:00 → %02d:00 (%dh) · %s".format(
                        desde, hasta!! + 1, ui.horasElegidas,
                        if (ui.personas == 1) "1 persona" else "${ui.personas} personas"
                    )
                } else {
                    "Elige las horas que quieres reservar"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (listo) TextoPrincipal else TextoSecundario,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            BotonPrimario(
                texto = "Confirmar reserva",
                enabled = listo,
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
        elegida -> Marca
        estado.torneo != null -> MarcaSuave
        estado.lleno -> PeligroSuave
        celda.pasada -> FondoApp
        parcial -> AlertaSuave
        else -> Superficie
    }
    val texto = when {
        elegida -> SobreMarca
        estado.torneo != null -> Marca
        estado.lleno -> Peligro
        celda.pasada -> TextoTenue
        else -> TextoPrincipal
    }
    val (numero, sufijo) = etiquetaHora(celda.hora)
    // Siempre se puede tocar: si la hora no se puede reservar, el aviso explica por qué.
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        color = fondo,
        border = BorderStroke(1.dp, if (elegida) Marca else BordeCampo)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(numero, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = texto, maxLines = 1)
            Text(sufijo, fontSize = 12.sp, color = texto, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Leyenda() {
    FlowRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            "Disponible" to Superficie,
            "Pocos lugares" to AlertaSuave,
            "Cupo lleno" to PeligroSuave,
            "Torneo" to MarcaSuave,
            "Ya pasó" to FondoApp
        ).forEach { (nombre, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, BordeCampo, CircleShape)
                )
                Text(
                    nombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ContadorPersonas(valor: Int, puedeBajar: Boolean, puedeSubir: Boolean, onMenos: () -> Unit, onMas: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonIcono(Icons.Filled.Remove, "Una persona menos", onMenos, enabled = puedeBajar)
        Text(
            text = valor.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp)
        )
        BotonIcono(Icons.Filled.Add, "Una persona más", onMas, enabled = puedeSubir)
    }
}
