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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.BotonIcono
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.ProcesoEnCurso
import com.example.clubdeportivo.ui.components.ProtegerSalida
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TituloSeccion
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.Alerta
import com.example.clubdeportivo.ui.theme.AlertaSuave
import com.example.clubdeportivo.ui.theme.AvailabilityBorder
import com.example.clubdeportivo.ui.theme.AvailabilityContainer
import com.example.clubdeportivo.ui.theme.AvailabilityForeground
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PeligroSuave
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.StatusAmberBorder
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.TextoTenue
import com.example.clubdeportivo.ui.theme.UnavailableBorder
import com.example.clubdeportivo.ui.theme.UnavailableContainer
import com.example.clubdeportivo.ui.theme.UnavailableForeground
import com.example.clubdeportivo.ui.torneos.TorneosScreen
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.esCliente
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaDeporte
import com.example.clubdeportivo.data.model.etiquetaArea

@Composable
fun ReservasScreen(initialTab: Int = 0) {
    val rol = SesionManager.usuarioActual?.rol
    val esCliente = rol?.esCliente() == true
    val esAdmin = rol?.esAdministrador() == true
    val puedeAprobar = rol?.esAdministrador() == true || rol?.esEncargado() == true
    var pestana by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val opciones = when {
        esCliente -> listOf(Textos.get("space"), Textos.get("tournament_2"), Textos.get("my_bookings"))
        esAdmin -> listOf(Textos.get("space"), Textos.get("tournament_2"), Textos.get("bookings"), Textos.get("requests"))
        puedeAprobar -> listOf(Textos.get("space"), Textos.get("tournament_2"), Textos.get("requests"))
        else -> listOf(Textos.get("space"), Textos.get("tournament_2"))
    }

    Column(modifier = Modifier.fillMaxSize().background(FondoApp)) {
        PestanasPildora(
            opciones = opciones,
            seleccionada = pestana.coerceAtMost(opciones.lastIndex),
            onSeleccion = { nueva -> if (nueva != pestana) ProcesoEnCurso.intentar { pestana = nueva } }
        )

        when (pestana.coerceAtMost(opciones.lastIndex)) {
            0 -> ReservarEspacioScreen()
            1 -> TorneosScreen()
            2 -> when {
                esCliente -> MisReservasScreen()
                esAdmin -> ReservasAdministradorScreen()
                puedeAprobar -> PorAprobarScreen()
                else -> TorneosScreen()
            }
            3 -> if (esAdmin) PorAprobarScreen() else TorneosScreen()
            else -> if (puedeAprobar) PorAprobarScreen() else TorneosScreen()
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
    val esperas by viewModel.esperas.observeAsState(emptyList())
    val ofertaEspera by viewModel.ofertaEspera.observeAsState()
    val permiteEspera = SesionManager.usuarioActual?.rol?.esCliente() == true

    // Al volver a la pantalla (por ejemplo, desde la notificación) se revisa si ya le tocó en la lista de espera.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescarEsperas() }

    ofertaEspera?.let { hora ->
        ui.area?.let { area ->
            DialogoListaEspera(
                area = area,
                fecha = viewModel.fechas.firstOrNull { it.first == ui.fecha }?.second ?: Fechas.legible(ui.fecha),
                hora = hora,
                onConfirmar = { personas -> viewModel.unirseAEspera(personas) },
                onCancelar = { viewModel.cerrarOfertaEspera() }
            )
        }
    }

    // Con horas ya elegidas hay una reserva a medias: salirse pide confirmación y, si se acepta, la descarta.
    ProtegerSalida(
        activo = ui.desde != null && !ui.enviando,
        mensaje = "salir_reserva_mensaje",
        alAbandonar = { viewModel.reiniciarProceso() }
    )

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
            FullScreenLoading(modifier = Modifier.padding(innerPadding), conMargen = true)
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
            EncabezadoPantalla(titulo = Textos.get("book_a_space"))
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

            Seccion(Textos.get("select_the_sport"))
            if (deportes.isEmpty()) {
                Text(
                    Textos.get("there_are_no_areas_yet"),
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
                            titulo = etiquetaDeporte(deporte),
                            detalle = ui.areas.count { it.tipo == deporte }.let { if (it == 1) Textos.get("n_1_area") else Textos.get("areas_2", it) },
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
                    Seccion(Textos.get("select_the_court"))

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
                                    titulo = etiquetaArea(area.nombre),
                                    detalle = when {
                                        area.disponibilidad == DisponibilidadArea.MANTENIMIENTO -> Textos.get("under_maintenance")
                                        bloqueada -> Textos.get("doesn_t_admit_visitors")
                                        else -> Textos.get("capacity", area.capacidad)
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
                    SeccionHorario(ui = ui, area = area, viewModel = viewModel, permiteEspera = permiteEspera)
                }
            }

            if (esperas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                TituloSeccion(Textos.get("espera_seccion"))
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    esperas.forEach { espera ->
                        TarjetaEspera(
                            espera = espera,
                            onReservar = { viewModel.reservarDesdeEspera(espera) },
                            onSalir = { viewModel.salirDeEspera(espera) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            TituloSeccion(Textos.get("current_bookings"))
            Spacer(modifier = Modifier.height(10.dp))
            if (reservas.isEmpty()) {
                Text(
                    Textos.get("no_upcoming_bookings"),
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
        listOf(Textos.get("sport"), Textos.get("court"), Textos.get("time")).forEachIndexed { indice, nombre ->
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
        modifier = if (seleccionada) {
            modifierReflejo(Modifier.fillMaxWidth(), enabled = true, shape = RoundedCornerShape(16.dp))
        } else {
            Modifier.fillMaxWidth()
        },
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
private fun SeccionHorario(ui: ReservaEspacioUi, area: Area, viewModel: ReservasViewModel, permiteEspera: Boolean) {
    Seccion(Textos.get("lbl_horario"))
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                Textos.get("lbl_fecha"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoSecundario,
                letterSpacing = 0.4.sp
            )
            // Todas las fechas a la vista (se acomodan en varias filas): en una sola fila con scroll la última quedaba cortada.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.fechas.forEach { (fecha, etiqueta) ->
                    val activa = fecha == ui.fecha
                    Surface(
                        onClick = { viewModel.elegirFecha(fecha) },
                        modifier = if (activa) {
                            modifierReflejo(Modifier, enabled = true, shape = RoundedCornerShape(12.dp))
                        } else {
                            Modifier
                        },
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
                        Textos.get("tap_the_hours_you_want_to")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                if (desde != null) {
                    TextButton(
                        onClick = { viewModel.limpiarSeleccion() }
                    ) {
                        Text(Textos.get("clear"), color = Marca, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (ui.horas.isEmpty()) {
                Text(
                    if (ui.errorDisponibilidad) Textos.get("the_availability_could_not_be_checked_2")
                    else Textos.get("there_are_no_times_available_that"),
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
                        CeldaHora(
                            celda = celda,
                            elegida = elegida,
                            permiteEspera = permiteEspera,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.tocarHora(celda.hora) }
                        )
                    }
                    repeat(relleno) { Spacer(modifier = Modifier.weight(1f)) }
                }

                Leyenda(mostrarEspera = permiteEspera)
                if (permiteEspera && ui.horas.any { it.esperable && !it.enEspera }) {
                    Text(
                        Textos.get("espera_pista"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (ui.desde != null) {
                Spacer(modifier = Modifier.height(16.dp))
                if (area.carriles > 0) {
                    Text(
                        Textos.get("lbl_carril"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoSecundario,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        if (ui.carrilesDisponibles.isEmpty()) Textos.get("there_are_no_free_lanes_at")
                        else Textos.get("select_an_available_lane"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..area.carriles).forEach { carril ->
                            val disponible = carril in ui.carrilesDisponibles
                            val seleccionado = carril == ui.carril
                            Surface(
                                onClick = { viewModel.elegirCarril(carril) },
                                enabled = disponible,
                                shape = RoundedCornerShape(12.dp),
                                color = if (seleccionado) Marca else if (disponible) MarcaSuave else FondoApp,
                                border = BorderStroke(1.dp, if (seleccionado) Marca else Borde),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text(
                                    Textos.get("lane_2", carril),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (seleccionado) SobreMarca else if (disponible) TextoPrincipal else TextoTenue
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            Textos.get("lbl_personas"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoSecundario,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            Textos.get("free_spots_of", ui.maxPersonas, area.capacidad),
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
    val requiereCarril = (ui.area?.carriles ?: 0) > 0
    val listo = desde != null && hasta != null && (!requiereCarril || ui.carril != null)
    val etiquetaFecha = viewModel.fechas.firstOrNull { it.first == ui.fecha }?.second ?: Fechas.legible(ui.fecha)

    Surface(color = Superficie, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(start = MargenPantalla, end = MargenPantalla, top = 12.dp, bottom = 12.dp)) {
            Text(
                text = if (listo) {
                    "$etiquetaFecha · %02d:00 → %02d:00 (%dh) · %s".format(
                        desde, hasta!! + 1, ui.horasElegidas,
                        if (ui.personas == 1) Textos.get("n_1_person") else Textos.get("people_5", ui.personas)
                    ) + (ui.carril?.let { Textos.get("lane_3", it) } ?: "")
                } else {
                    Textos.get("choose_the_hours_you_want_to")
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (listo) TextoPrincipal else TextoSecundario,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            BotonPrimario(
                texto = Textos.get("confirm_booking"),
                enabled = listo,
                cargando = ui.enviando,
                onClick = { viewModel.confirmar() }
            )
        }
    }
}

@Composable
private fun CeldaHora(celda: HoraUi, elegida: Boolean, permiteEspera: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val estado = celda.estado
    val ocupada = estado.lleno || estado.torneo != null
    // Una hora llena también se puede tocar (socios y visitantes): ofrece entrar a la lista de espera.
    val habilitada = (celda.seleccionable && !ocupada) || (permiteEspera && celda.esperable)
    val pocosLugares = !celda.pasada && estado.pocosLugares
    val fondo = when {
        elegida -> Marca
        ocupada -> UnavailableContainer
        celda.pasada -> FondoApp
        pocosLugares -> AlertaSuave
        else -> AvailabilityContainer
    }
    val texto = when {
        elegida -> SobreMarca
        ocupada -> UnavailableForeground
        celda.pasada -> TextoTenue
        pocosLugares -> Alerta
        else -> AvailabilityForeground
    }
    val (numero, sufijo) = etiquetaHora(celda.hora)
    Surface(
        onClick = onClick,
        enabled = habilitada,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        color = fondo,
        border = BorderStroke(
            if (elegida || celda.enEspera) 1.5.dp else 1.dp,
            when {
                elegida -> Marca
                celda.enEspera -> Alerta
                ocupada -> UnavailableBorder
                celda.pasada -> Borde
                pocosLugares -> StatusAmberBorder
                else -> AvailabilityBorder
            }
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(numero, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = texto, maxLines = 1)
            Text(if (celda.enEspera) ICONO_ESPERA else sufijo, fontSize = 12.sp, color = texto, maxLines = 1)
        }
    }
}

private const val ICONO_ESPERA = "⏳"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Leyenda(mostrarEspera: Boolean) {
    FlowRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Textos.get("available") to AvailabilityContainer,
            Textos.get("few_spots") to AlertaSuave,
            Textos.get("occupied_2") to UnavailableContainer,
            Textos.get("selected") to Marca,
            Textos.get("already_passed") to FondoApp
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
        if (mostrarEspera) {
            Text("$ICONO_ESPERA ${Textos.get("espera_leyenda")}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
}

/** Ofrece entrar a la lista de espera de una hora llena, con cuántas personas. */
@Composable
private fun DialogoListaEspera(area: Area, fecha: String, hora: Int, onConfirmar: (Int) -> Unit, onCancelar: () -> Unit) {
    var personas by rememberSaveable { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(20.dp),
        containerColor = Superficie,
        title = { Text(Textos.get("espera_titulo_dialogo"), style = MaterialTheme.typography.titleLarge, color = TextoPrincipal) },
        text = {
            Column {
                Text(
                    Textos.get(
                        "espera_mensaje_dialogo",
                        Deportes.titulo(area.tipo, area.nombre), fecha, "%02d:00".format(hora), "%02d:00".format(hora + 1),
                        Catalogos.MINUTOS_PRIORIDAD_LISTA_ESPERA
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Textos.get("lbl_personas"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario, letterSpacing = 0.4.sp)
                    ContadorPersonas(
                        valor = personas,
                        puedeBajar = personas > 1,
                        puedeSubir = personas < area.capacidad,
                        onMenos = { personas-- },
                        onMas = { personas++ }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(personas) }) {
                Text(Textos.get("espera_unirme"), color = Marca, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(Textos.get("cancel"), color = TextoSecundario, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/** Una hora en la que la persona está esperando lugar: su lugar en la fila o, si ya le tocó, hasta cuándo puede reservar. */
@Composable
private fun TarjetaEspera(espera: EsperaListada, onReservar: () -> Unit, onSalir: () -> Unit) {
    val entrada = espera.entrada
    val leTocaReservar = espera.limite != null
    TarjetaClub(modifier = Modifier.fillMaxWidth(), destacada = leTocaReservar, fondo = if (leTocaReservar) AlertaSuave else Superficie) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(espera.emoji, fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(espera.titulo, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${Fechas.legible(entrada.fecha)} · %02d:00–%02d:00 · %s".format(
                            entrada.hora, entrada.hora + 1,
                            if (entrada.personas == 1) Textos.get("n_1_person") else Textos.get("people_5", entrada.personas)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
            Text(
                text = when {
                    leTocaReservar -> Textos.get("espera_tu_turno", espera.limite!!)
                    espera.posicion != null -> Textos.get("espera_posicion", espera.posicion)
                    else -> Textos.get("espera_en_fila")
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (leTocaReservar) Alerta else TextoPrincipal,
                modifier = Modifier.padding(top = 10.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSalir) {
                    Text(Textos.get("espera_salir"), color = Peligro, fontWeight = FontWeight.SemiBold)
                }
                if (leTocaReservar) {
                    TextButton(onClick = onReservar) {
                        Text(Textos.get("espera_reservar"), color = Marca, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContadorPersonas(valor: Int, puedeBajar: Boolean, puedeSubir: Boolean, onMenos: () -> Unit, onMas: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonIcono(
            Icons.Filled.Remove,
            Textos.get("one_person_less"),
            onMenos,
            enabled = puedeBajar,
            conReflejo = false
        )
        Text(
            text = valor.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp)
        )
        BotonIcono(
            Icons.Filled.Add,
            Textos.get("one_more_person"),
            onMas,
            enabled = puedeSubir,
            conReflejo = false
        )
    }
}
