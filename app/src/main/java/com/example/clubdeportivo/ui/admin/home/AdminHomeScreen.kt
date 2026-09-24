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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminHomeScreen() {
    DashboardContent()
}


@Composable
fun DashboardContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // Encabezado
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

        Spacer(modifier = Modifier.height(28.dp))

        // Título del dashboard
        Text(
            text = "Dashboard",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Primera fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            DashboardCard(
                emoji = "🏟️",
                cantidad = "9",
                titulo = "Canchas",
                backgroundColor = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                emoji = "👥",
                cantidad = "3",
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
                cantidad = "3",
                titulo = "Miembros",
                backgroundColor = Color(0xFFFFFBEB),
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                emoji = "🗓️",
                cantidad = "2",
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
                    color = Color(0xFF111827)
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
        if (confirmada) {
            Color(0xFFDCFCE7)
        } else {
            Color(0xFFFEF3C7)
        }

    val colorTexto =
        if (confirmada) {
            Color(0xFF16A34A)
        } else {
            Color(0xFFD97706)
        }

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