package com.example.clubdeportivo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.components.MargenPantalla
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
import java.util.Locale

private val Tinta = Color(0xFF10233F)
private val AzulClaro = Color(0xFFE8F2FF)
private val Verde = Color(0xFF13835D)
private val FondoVerde = Color(0xFFE7F6EF)
private val Amarillo = Color(0xFF9A6500)
private val FondoAmarillo = Color(0xFFFFF4D8)

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
    var indiceNovedad by remember { mutableIntStateOf(0) }
    var novedadAbierta by remember { mutableStateOf<Novedad?>(null) }
    val reservas by reservasViewModel.reservas.observeAsState(emptyList())
    val reservasActivas = reservas.filter {
        it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.PENDIENTE_APROBACION
    }
    val nombre = SesionManager.usuarioActual?.nombre?.substringBefore(" ")?.ifBlank { "Mariana" } ?: "Mariana"
    val novedades = publicacionesViewModel.visibles
        .filter { it.imagenUrl.isNotBlank() }
        .map { it.aNovedad() }
    val fechaHoy = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-MX")).format(Date())
            .replaceFirstChar { it.uppercase() }
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
                Text("Hola, $nombre", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Tinta)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = AzulClaro) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFD38B13), modifier = Modifier.size(18.dp))
                    Text("24° · Soleado", style = MaterialTheme.typography.labelMedium, color = Tinta)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TituloSeccion("Lo que pasa en el club")
        Spacer(Modifier.height(12.dp))
        if (novedades.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                Text(
                    "Cuando el club publique una novedad para miembros, aparecerá aquí.",
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        } else {
        val destacada = novedades[indiceNovedad.coerceIn(novedades.indices)]
        Card(
            onClick = { novedadAbierta = destacada },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Superficie),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                AsyncImage(
                    model = destacada.imagen,
                    contentDescription = destacada.titulo,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                    Text(destacada.categoria, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Tinta, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(destacada.titulo, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        destacada.descripcion.substringBefore(".") + ".",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    novedades.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (indiceNovedad == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (indiceNovedad == index) Marca else Borde)
                                .clickable { indiceNovedad = index }
                        )
                    }
                    TextButton(onClick = { indiceNovedad = (indiceNovedad + 1) % novedades.size }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Siguiente", color = Marca)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente novedad", tint = Marca, modifier = Modifier.size(16.dp))
                    }
                }
                FilledTonalButton(
                    onClick = { novedadAbierta = destacada },
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = AzulClaro,
                        contentColor = Marca
                    )
                ) {
                    Text(destacada.accion, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        }

        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion("Tus próximas reservas", modifier = Modifier.weight(1f))
            TextButton(onClick = onReservar) { Text("Ver todas", color = Marca) }
        }
        MemberBookingCard(reserva = reservasActivas.firstOrNull(), onClick = onReservar)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onMembresia, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Consultar mi membresía")
        }
    }

    novedadAbierta?.let { novedad ->
        NovedadDetalle(
            novedad = novedad,
            onCerrar = { novedadAbierta = null }
        )
    }
}

private fun Publicacion.aNovedad() = Novedad(
    categoria = "Novedad del club",
    titulo = titulo,
    descripcion = descripcion + vigencia.takeIf { it.isNotBlank() }?.let { "\n\nVigente hasta: ${Fechas.legible(it)}" }.orEmpty(),
    imagen = imagenUrl,
    accion = "Ver detalles"
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
                Text("Aún no tienes reservas próximas.", style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.SemiBold)
                Text("Explora los espacios disponibles para agendar una visita.", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, modifier = Modifier.padding(top = 5.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(color = AzulClaro, shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FECHA", style = MaterialTheme.typography.labelSmall, color = Marca, fontWeight = FontWeight.Bold)
                        Text(reserva.fecha.substringAfterLast("-"), style = MaterialTheme.typography.titleLarge, color = Tinta, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(reserva.titulo, style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${Fechas.legible(reserva.fecha)} · ${reserva.horaInicio}–${reserva.horaFin}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                    reserva.carril?.let { Text("Carril $it", style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
                    val (estadoColor, estadoFondo) = if (reserva.estado == EstadoReserva.CONFIRMADA) Verde to FondoVerde else Amarillo to FondoAmarillo
                    Surface(color = estadoFondo, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) "Solicitud en revisión" else reserva.estado.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = estadoColor
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Ver mis reservas", tint = TextoSecundario)
            }
        }
    }
}

@Composable
private fun NovedadDetalle(novedad: Novedad, onCerrar: () -> Unit) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(14.dp), shape = RoundedCornerShape(24.dp), color = Superficie) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
            if (novedad.imagen.isNotBlank()) {
                AsyncImage(
                    model = novedad.imagen,
                    contentDescription = novedad.titulo,
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp)),
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
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCerrar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
            ) {
                Text("Cerrar")
            }
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
    var novedadAbierta by remember { mutableStateOf<Novedad?>(null) }
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
        Text("MI ÁREA · ${area.uppercase()}", style = MaterialTheme.typography.labelMedium, color = Marca, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(5.dp))
        Text("Buen día, $nombre", style = MaterialTheme.typography.headlineSmall, color = Tinta, fontWeight = FontWeight.Bold)

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
                        if (pendientes > 0) "$pendientes solicitudes pendientes" else "Todo en orden",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pendientes > 0) Amarillo else Verde,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (pendientes > 0) "Revisa las solicitudes de tu área" else "No tienes solicitudes pendientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tinta
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion("Hoy en tu área")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EncargadoMetric(reservasHoy.size.toString(), "Reservas hoy", "$confirmadasHoy confirmadas", Icons.Filled.CalendarMonth, Modifier.weight(1f))
            EncargadoMetric(pendientes.toString(), "Solicitudes", "Pendientes de revisar", Icons.Filled.Schedule, Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion("Próxima reserva", modifier = Modifier.weight(1f))
            TextButton(onClick = onReservas) { Text("Agenda", color = Marca) }
        }
        val proximaReserva = reservasArea.firstOrNull()
        if (proximaReserva == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Superficie)
            ) {
                Text("No hay reservas próximas en tu área.", modifier = Modifier.padding(18.dp), color = TextoSecundario)
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
                            if (confirmada) "Confirmada" else "Solicitud",
                            if (confirmada) FondoVerde else FondoAmarillo,
                            if (confirmada) Verde else Amarillo
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        listOfNotNull(proximaReserva.carril?.let { "Carril $it" }, proximaReserva.titulo).joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                        color = Tinta,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${proximaReserva.usuarioNombre.ifBlank { "Usuario" }}${if (proximaReserva.personas > 1) " + ${proximaReserva.personas - 1} personas" else ""} · hasta ${proximaReserva.horaFin}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion("Novedades del club")
        Spacer(Modifier.height(12.dp))
        if (novedades.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                Text("Cuando se publique una novedad para el personal, aparecerá aquí.", modifier = Modifier.padding(18.dp), color = TextoSecundario)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                novedades.forEach { novedad ->
                    Card(
                        onClick = { novedadAbierta = novedad },
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
                                FilledTonalButton(
                                    onClick = { novedadAbierta = novedad },
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = AzulClaro,
                                        contentColor = Marca
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Ver detalles")
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Marca, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    novedadAbierta?.let { NovedadDetalle(it) { novedadAbierta = null } }
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
