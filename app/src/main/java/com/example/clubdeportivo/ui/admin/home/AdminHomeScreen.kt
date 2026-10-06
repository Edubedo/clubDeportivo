package com.example.clubdeportivo.ui.admin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

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
        totalAreas = totalAreas,
        totalPersonal = totalPersonal,
        totalReservasHoy = totalReservasHoy,
        totalMiembros = totalMiembros
    )
}

@Composable
fun DashboardContent(
    totalAreas: String = "–",
    totalPersonal: String = "–",
    totalReservasHoy: String = "–",
    totalMiembros: String = "–"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF1F8))
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF192338)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    emoji = "🏟️",
                    cantidad = totalAreas,
                    titulo = "Áreas",
                    backgroundColor = Color.White,
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    emoji = "👥",
                    cantidad = totalPersonal,
                    titulo = "Personal",
                    backgroundColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    emoji = "👤",
                    cantidad = totalMiembros,
                    titulo = "Miembros",
                    backgroundColor = Color.White,
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    emoji = "🗓️",
                    cantidad = totalReservasHoy,
                    titulo = "Reservas hoy",
                    backgroundColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Reservas recientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF192338)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ReservaCard(emoji = "🎾", nombre = "Ana García", detalle = "Tenis · 09:00–11:00", estado = "Confirmada", confirmada = true)
            Spacer(modifier = Modifier.height(10.dp))
            ReservaCard(emoji = "🎾", nombre = "Luis Pérez", detalle = "Tenis · 11:00–12:00", estado = "Confirmada", confirmada = true)
            Spacer(modifier = Modifier.height(10.dp))
            ReservaCard(emoji = "🏊", nombre = "Luis Pérez", detalle = "Natación · 08:00–09:00", estado = "Confirmada", confirmada = true)
            Spacer(modifier = Modifier.height(10.dp))
            ReservaCard(emoji = "🏀", nombre = "Ana García", detalle = "Baloncesto · 14:00–16:00", estado = "Pendiente", confirmada = false)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

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
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Column {
                Text(
                    text = cantidad,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF192338),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = titulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF31487A)
                )
            }
        }
    }
}

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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF192338)
                    )
                    Text(
                        text = detalle,
                        fontSize = 12.sp,
                        color = Color(0xFF31487A)
                    )
                }
            }

            EstadoReserva(estado = estado, confirmada = confirmada)
        }
    }
}

@Composable
fun EstadoReserva(
    estado: String,
    confirmada: Boolean
) {
    // 🎨 Tonos suaves: Verde elegante para confirmado/éxito, Rojo/Ámbar suave para pendiente o denegado
    val colorFondo = if (confirmada) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
    val colorTexto = if (confirmada) Color(0xFF137333) else Color(0xFFC5221F)

    Text(
        text = estado,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = colorTexto,
        modifier = Modifier
            .background(
                color = colorFondo,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            )
    )
}