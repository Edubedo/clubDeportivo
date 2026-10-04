package com.example.clubdeportivo.ui.admin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button                 // NUEVO
import androidx.compose.material3.ButtonDefaults         // NUEVO
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf           // NUEVO
import androidx.compose.runtime.remember                 // NUEVO
import androidx.compose.runtime.setValue                 // NUEVO
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.components.InitialsAvatar
import com.example.clubdeportivo.util.FotoPerfilManager

@Composable
fun AdminHomeScreen(
    onIrPerfil: () -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val totalAreas by viewModel.totalAreas.observeAsState("–")
    val totalPersonal by viewModel.totalPersonal.observeAsState("–")
    val totalReservasHoy by viewModel.totalReservasHoy.observeAsState("–")
    val totalMiembros by viewModel.totalMiembros.observeAsState("–")

    DashboardContent(
        onIrPerfil = onIrPerfil,
        totalAreas = totalAreas,
        totalPersonal = totalPersonal,
        totalReservasHoy = totalReservasHoy,
        totalMiembros = totalMiembros
    )
}


@Composable
fun DashboardContent(
    onIrPerfil: () -> Unit,
    totalAreas: String = "–",
    totalPersonal: String = "–",
    totalReservasHoy: String = "–",
    totalMiembros: String = "–"
) {
    val context = LocalContext.current
    val usuario = SesionManager.usuarioActual

    // NUEVO: controla si se muestra la hoja "Enviar notificación"
    var mostrarNotificaciones by remember { mutableStateOf(false) }

    // Al cambiar la foto, este valor cambia y Compose
    // vuelve a dibujar el encabezado.
    val versionFoto = SesionManager.versionFotoPerfil

    val fotoPerfil = usuario?.id?.let { usuarioId ->
        FotoPerfilManager.obtenerFoto(
            context = context,
            usuarioId = usuarioId
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(
                    text = "Club Deportivo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "Admin",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Foto / avatar del administrador
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onIrPerfil() },
                contentAlignment = Alignment.Center
            ) {
                if (fotoPerfil != null) {
                    AsyncImage(
                        model = fotoPerfil,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    InitialsAvatar(
                        nombre = usuario?.nombre ?: "Admin",
                        size = 46.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── CAMBIO: Título del dashboard + botón Notif. ──────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Button(
                onClick = { mostrarNotificaciones = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0EDFF),
                    contentColor = Color(0xFF2F80FF)
                )
            ) {
                Text("📢 Notif.", fontWeight = FontWeight.SemiBold)
            }
        }
        // ─────────────────────────────────────────────────────────────

        Spacer(modifier = Modifier.height(20.dp))

        // Primera fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                emoji = "🏟️",
                cantidad = totalAreas,
                titulo = "Áreas",
                backgroundColor = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                emoji = "👥",
                cantidad = totalPersonal,
                titulo = "Personal",
                backgroundColor = Color(0xFFFAF5FF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segunda fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                emoji = "👤",
                cantidad = totalMiembros,
                titulo = "Miembros",
                backgroundColor = Color(0xFFFFFBEB),
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                emoji = "🗓️",
                cantidad = totalReservasHoy,
                titulo = "Reservas hoy",
                backgroundColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Reservas recientes
        Text(
            text = "Reservas recientes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReservaCard(
            emoji = "🎾",
            nombre = "Ana García",
            detalle = "Tenis · 09:00–11:00",
            estado = "Confirmada",
            confirmada = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        ReservaCard(
            emoji = "🎾",
            nombre = "Luis Pérez",
            detalle = "Tenis · 11:00–12:00",
            estado = "Confirmada",
            confirmada = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        ReservaCard(
            emoji = "🏊",
            nombre = "Luis Pérez",
            detalle = "Natación · 08:00–09:00",
            estado = "Confirmada",
            confirmada = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        ReservaCard(
            emoji = "🏀",
            nombre = "Ana García",
            detalle = "Baloncesto · 14:00–16:00",
            estado = "Pendiente",
            confirmada = false
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // NUEVO: hoja para enviar notificaciones (empleados / socios / todos)
    if (mostrarNotificaciones) {
        EnviarNotificacionSheet(
            onDismiss = { mostrarNotificaciones = false }
        )
    }
}


/*
 * Tarjeta utilizada para mostrar las estadísticas
 * del Dashboard.
 */
@Composable
fun DashboardCard(
    emoji: String,
    cantidad: String,
    titulo: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = emoji,
                fontSize = 26.sp
            )

            Column {
                Text(
                    text = cantidad,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    softWrap = false
                )

                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/*
 * Tarjeta para mostrar una reserva reciente.
 */
@Composable
fun ReservaCard(
    emoji: String,
    nombre: String,
    detalle: String,
    estado: String,
    confirmada: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp
                )

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = detalle,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            EstadoReserva(
                estado = estado,
                confirmada = confirmada
            )
        }
    }
}


/*
 * Etiqueta que indica si la reserva está
 * confirmada o pendiente.
 */
@Composable
fun EstadoReserva(
    estado: String,
    confirmada: Boolean
) {
    val colorFondo =
        if (confirmada) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)

    val colorTexto =
        if (confirmada) Color(0xFF16A34A) else Color(0xFFD97706)

    Text(
        text = estado,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colorTexto,
        modifier = Modifier
            .background(
                color = colorFondo,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    )
}