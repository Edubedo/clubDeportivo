package com.example.clubdeportivo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.tono
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.BloqueEsqueleto
import com.example.clubdeportivo.ui.components.TituloSeccion
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.ExitoSuave
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.reservas.ReservaListada
import com.example.clubdeportivo.ui.reservas.ReservasViewModel
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.data.publicaciones.Publicacion
import com.example.clubdeportivo.ui.publicaciones.PublicacionesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import com.example.clubdeportivo.util.Idioma
import com.example.clubdeportivo.util.PreferenciaIdioma
import java.util.Locale
import com.example.clubdeportivo.util.Textos

private val Tinta: Color @Composable @ReadOnlyComposable get() = tono(0xFF10233F, 0xFFE8EEF7)
private val AzulClaro: Color @Composable @ReadOnlyComposable get() = tono(0xFFE8F2FF, 0xFF1B3358)
private val Verde: Color @Composable @ReadOnlyComposable get() = tono(0xFF13835D, 0xFF6EE7A0)
private val FondoVerde: Color @Composable @ReadOnlyComposable get() = tono(0xFFE7F6EF, 0xFF0F2E1E)
private val Amarillo: Color @Composable @ReadOnlyComposable get() = tono(0xFF9A6500, 0xFFFBBF24)
private val FondoAmarillo: Color @Composable @ReadOnlyComposable get() = tono(0xFFFFF4D8, 0xFF33260A)

private data class Novedad(
    val categoria: String,
    val titulo: String,
    val descripcion: String,
    val imagen: String,
    val accion: String
)

@Composable
fun MiembroHomeScreen(
    publicacionesViewModel: PublicacionesViewModel,
    reservasViewModel: ReservasViewModel,
    onReservar: () -> Unit,
    onMembresia: () -> Unit
) {
    val weatherViewModel: WeatherViewModel = viewModel()
    var novedadAbierta by remember { mutableIntStateOf(-1) }
    var imagenCompleta by remember { mutableStateOf<String?>(null) }
    var dedoPresionado by remember { mutableStateOf(false) }
    val reservas by reservasViewModel.reservas.observeAsState(emptyList())
    val reservasActivas = reservas.filter {
        it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.PENDIENTE_APROBACION
    }
    val nombre = SesionManager.usuarioActual?.nombre?.substringBefore(" ")?.ifBlank { "Mariana" } ?: "Mariana"
    val novedades = publicacionesViewModel.visibles.map { it.aNovedad() }
    val pagerState = rememberPagerState(pageCount = { novedades.size })
    val scope = rememberCoroutineScope()
    val clima = weatherViewModel.uiState
    LaunchedEffect(novedades.size, dedoPresionado, pagerState) {
        if (novedades.size < 2) return@LaunchedEffect
        while (true) {
            delay(4_500)
            if (!dedoPresionado && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % novedades.size)
            }
        }
    }
    val fechaHoy = remember {
        val formato = if (PreferenciaIdioma.actual == Idioma.EN) {
            SimpleDateFormat("EEEE, MMMM d", Locale.US)
        } else {
            SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-MX"))
        }
        formato.format(Date()).replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargenPantalla)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fechaHoy, style = MaterialTheme.typography.labelLarge, color = TextoSecundario)
                Spacer(Modifier.height(4.dp))
                Text(Textos.get("hello", nombre), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Tinta)
            }
            WeatherWidget(state = clima, onRetry = weatherViewModel::reintentar)
        }

        Spacer(Modifier.height(24.dp))
        TituloSeccion(Textos.get("what_s_happening_at_the_club"))
        Spacer(Modifier.height(12.dp))
        if (novedades.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                Text(
                    Textos.get("when_the_club_publishes_news_for"),
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            dedoPresionado = true
                            try {
                                waitForUpOrCancellation()
                            } finally {
                                dedoPresionado = false
                            }
                        }
                    },
                pageSpacing = 12.dp
            ) { page ->
                val novedad = novedades[page]
                Card(
                    onClick = { novedadAbierta = page },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Superficie),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(if (novedad.imagen.isBlank()) Tinta else Color.Transparent)
                    ) {
                        if (novedad.imagen.isNotBlank()) {
                            AsyncImage(
                                model = novedad.imagen,
                                contentDescription = novedad.titulo,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC071426))))
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                            shape = RoundedCornerShape(50),
                            color = Superficie.copy(alpha = 0.94f)
                        ) {
                            Text(novedad.categoria, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Tinta, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(novedad.titulo, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                novedad.descripcion.substringBefore(".") + ".",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            val destacada = novedades[pagerState.currentPage.coerceIn(novedades.indices)]
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    novedades.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) Marca else Borde)
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
                TextButton(
                    onClick = { novedadAbierta = pagerState.currentPage },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        destacada.accion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion(Textos.get("your_upcoming_bookings"), modifier = Modifier.weight(1f))
            TextButton(onClick = onReservar) { Text(Textos.get("see_all"), color = Marca) }
        }
        MemberBookingCard(reserva = reservasActivas.firstOrNull(), onClick = onReservar)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onMembresia, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(Textos.get("view_my_membership"))
        }
    }

    if (novedadAbierta >= 0 && novedades.isNotEmpty()) {
        NovedadDetalle(
            novedades = novedades,
            paginaInicial = novedadAbierta,
            onCerrar = { novedadAbierta = -1 },
            onAbrirImagen = { imagenCompleta = it }
        )
    }
    imagenCompleta?.let { imagen ->
        ImagenCompleta(
            imagen = imagen,
            onCerrar = { imagenCompleta = null }
        )
    }
}

private fun Publicacion.aNovedad() = Novedad(
    categoria = Textos.get("club_news"),
    titulo = titulo,
    descripcion = descripcion + vigencia.takeIf { it.isNotBlank() }?.let { Textos.get("n_nvalid_until", Fechas.legible(it)) }.orEmpty(),
    imagen = imagenUrl,
    accion = Textos.get("view_details")
)

@Composable
private fun MemberBookingCard(reserva: ReservaListada?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (reserva == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(Textos.get("you_don_t_have_any_upcoming"), style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.SemiBold)
                Text(Textos.get("explore_the_available_spaces_to_schedu"), style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, modifier = Modifier.padding(top = 5.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(color = AzulClaro, shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Textos.get("lbl_fecha"), style = MaterialTheme.typography.labelSmall, color = Marca, fontWeight = FontWeight.Bold)
                        Text(reserva.fecha.substringAfterLast("-"), style = MaterialTheme.typography.titleLarge, color = Tinta, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(reserva.titulo, style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${Fechas.legible(reserva.fecha)} · ${reserva.horaInicio}–${reserva.horaFin}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                    reserva.carril?.let { Text(Textos.get("lane", it), style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
                    val (estadoColor, estadoFondo) = if (reserva.estado == EstadoReserva.CONFIRMADA) Verde to FondoVerde else Amarillo to FondoAmarillo
                    Surface(color = estadoFondo, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) Textos.get("request_under_review") else reserva.estado.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = estadoColor
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = Textos.get("view_my_bookings"), tint = TextoSecundario)
            }
        }
    }
}

@Composable
private fun WeatherWidget(state: WeatherUiState, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AzulClaro) {
        when (state) {
            WeatherUiState.Loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BloqueEsqueleto(Modifier.width(72.dp).height(10.dp), RoundedCornerShape(5.dp))
                    BloqueEsqueleto(Modifier.width(48.dp).height(10.dp), RoundedCornerShape(5.dp))
                }
            }
            is WeatherUiState.Success -> {
                val weatherCode = state.data.weatherCode
                val condition = when (weatherCode) {
                    0 -> "weather_clear"
                    1, 2 -> "weather_partly_cloudy"
                    3 -> "weather_cloudy"
                    45, 48 -> "weather_fog"
                    in 51..67, in 80..82 -> "weather_rain"
                    in 71..77, 85, 86 -> "weather_snow"
                    in 95..99 -> "weather_storm"
                    else -> "weather_cloudy"
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (weatherCode == 0) Icons.Filled.WbSunny else Icons.Filled.Cloud,
                        contentDescription = null,
                        tint = if (weatherCode == 0) Color(0xFFD38B13) else Marca,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = Textos.get("weather_temperature", state.data.temperatureCelsius),
                            style = MaterialTheme.typography.labelMedium,
                            color = Tinta
                        )
                        Text(Textos.get(condition), style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                    }
                }
            }
            is WeatherUiState.Error -> {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, style = MaterialTheme.typography.labelSmall, color = Tinta)
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text(Textos.get("reintentar_button"), style = MaterialTheme.typography.labelSmall, color = Marca)
                    }
                }
            }
        }
    }
}

@Composable
private fun NovedadDetalle(
    novedades: List<Novedad>,
    paginaInicial: Int,
    onCerrar: () -> Unit,
    onAbrirImagen: (String) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = paginaInicial.coerceIn(novedades.indices),
        pageCount = { novedades.size }
    )
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(14.dp), shape = RoundedCornerShape(24.dp), color = Superficie) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    Textos.get("novedad_pager_page", pagerState.currentPage + 1, novedades.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(8.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val novedad = novedades[page]
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (novedad.imagen.isNotBlank()) {
                            AsyncImage(
                                model = novedad.imagen,
                                contentDescription = novedad.titulo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onAbrirImagen(novedad.imagen) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(novedad.categoria, style = MaterialTheme.typography.labelMedium, color = Marca, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(novedad.titulo, style = MaterialTheme.typography.headlineSmall, color = Tinta, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(novedad.descripcion, style = MaterialTheme.typography.bodyLarge, color = TextoSecundario)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCerrar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Peligro)
                ) {
                    Text(Textos.get("close"))
                }
            }
        }
    }
}

@Composable
private fun ImagenCompleta(imagen: String, onCerrar: () -> Unit) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagen,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            TextButton(
                onClick = onCerrar,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text(Textos.get("close"), color = Color.White)
            }
        }
    }
}

@Composable
fun EncargadoHomeScreen(
    publicacionesViewModel: PublicacionesViewModel,
    reservasViewModel: ReservasViewModel,
    onReservas: () -> Unit
) {
    var novedadAbierta by remember { mutableIntStateOf(-1) }
    var imagenCompleta by remember { mutableStateOf<String?>(null) }
    val reservas by reservasViewModel.reservas.observeAsState(emptyList())
    val usuario = SesionManager.usuarioActual
    val nombre = usuario?.nombre?.substringBefore(" ")?.ifBlank { "Esteban" } ?: "Esteban"
    val area = usuario?.areaTrabajo?.ifBlank { "Natación" } ?: "Natación"
    val areaNormalizada = area.normalizar()
    val reservasArea = reservas.filter {
        it.deporte.normalizar() == areaNormalizada || it.titulo.normalizar().contains(areaNormalizada)
    }
    val reservasHoy = reservasArea.filter { it.fecha == Fechas.hoy() }
    val confirmadasHoy = reservasHoy.count { it.estado == EstadoReserva.CONFIRMADA }
    val pendientes = reservasArea.count { it.estado == EstadoReserva.PENDIENTE_APROBACION }
    val novedades = publicacionesViewModel.visibles.map { it.aNovedad() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargenPantalla)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text(Textos.get("my_area", area.uppercase()), style = MaterialTheme.typography.labelMedium, color = Marca, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(5.dp))
        Text(Textos.get("good_morning", nombre), style = MaterialTheme.typography.headlineSmall, color = Tinta, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (pendientes > 0) FondoAmarillo else FondoVerde),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = if (pendientes > 0) Amarillo else Verde, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        if (pendientes > 0) Textos.get("pending_requests", pendientes) else Textos.get("all_clear"),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pendientes > 0) Amarillo else Verde,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (pendientes > 0) Textos.get("review_your_area_s_requests") else Textos.get("you_have_no_pending_requests"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tinta
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion(Textos.get("today_in_your_area"))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EncargadoMetric(reservasHoy.size.toString(), Textos.get("bookings_today"), Textos.get("confirmed", confirmadasHoy), Icons.Filled.CalendarMonth, Modifier.weight(1f))
            EncargadoMetric(pendientes.toString(), Textos.get("requests"), Textos.get("pending_review"), Icons.Filled.Schedule, Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion(Textos.get("next_booking"), modifier = Modifier.weight(1f))
            TextButton(onClick = onReservas) { Text(Textos.get("schedule"), color = Marca) }
        }
        val proximaReserva = reservasArea.firstOrNull()
        if (proximaReserva == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Superficie)
            ) {
                Text(Textos.get("there_are_no_upcoming_bookings_in"), modifier = Modifier.padding(18.dp), color = TextoSecundario)
            }
        } else {
            Card(
                onClick = onReservas,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${Fechas.legible(proximaReserva.fecha)} · ${proximaReserva.horaInicio}", style = MaterialTheme.typography.titleLarge, color = Tinta, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val confirmada = proximaReserva.estado == EstadoReserva.CONFIRMADA
                        StatePill(
                            if (confirmada) Textos.get("confirmed_2") else Textos.get("request"),
                            if (confirmada) FondoVerde else FondoAmarillo,
                            if (confirmada) Verde else Amarillo
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        listOfNotNull(proximaReserva.carril?.let { Textos.get("lane", it) }, proximaReserva.titulo).joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                        color = Tinta,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        Textos.get(
                            "manual_usuario_hasta",
                            proximaReserva.usuarioNombre.ifBlank { Textos.get("user") } +
                                (if (proximaReserva.personas > 1) Textos.get("people", proximaReserva.personas - 1) else ""),
                            proximaReserva.horaFin
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion(Textos.get("club_news_2"))
        Spacer(Modifier.height(12.dp))
        if (novedades.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                Text(Textos.get("when_news_for_staff_is_published"), modifier = Modifier.padding(18.dp), color = TextoSecundario)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                novedades.forEachIndexed { index, novedad ->
                    Card(
                        onClick = { novedadAbierta = index },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Superficie),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        if (novedad.imagen.isNotBlank()) {
                            AsyncImage(
                                model = novedad.imagen,
                                contentDescription = novedad.titulo,
                                modifier = Modifier.fillMaxWidth().height(130.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.padding(15.dp)) {
                            Text(novedad.categoria, style = MaterialTheme.typography.labelMedium, color = Marca)
                            Text(novedad.titulo, style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { novedadAbierta = index },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        Textos.get("view_details"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (novedadAbierta >= 0 && novedades.isNotEmpty()) {
        NovedadDetalle(
            novedades = novedades,
            paginaInicial = novedadAbierta,
            onCerrar = { novedadAbierta = -1 },
            onAbrirImagen = { imagenCompleta = it }
        )
    }
    imagenCompleta?.let { imagen ->
        ImagenCompleta(imagen = imagen, onCerrar = { imagenCompleta = null })
    }
}

@Composable
private fun EncargadoMetric(valor: String, titulo: String, detalle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(122.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Superficie), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Marca, modifier = Modifier.size(21.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(valor, style = MaterialTheme.typography.headlineSmall, color = Tinta, fontWeight = FontWeight.Bold)
                    Text(titulo, style = MaterialTheme.typography.labelMedium, color = TextoSecundario, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(detalle, style = MaterialTheme.typography.labelSmall, color = if (detalle.startsWith("↑")) Verde else TextoSecundario)
            }
        }
    }
}

@Composable
private fun StatePill(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

private fun String.normalizar(): String = lowercase()
    .replace("á", "a")
    .replace("é", "e")
    .replace("í", "i")
    .replace("ó", "o")
    .replace("ú", "u")
