package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.ui.components.EmptyState

private val TextoPrincipal = Color(0xFF192338)
private val TextoSecundario = Color(0xFF31487A)

@Composable
fun MisReservasScreen(
    viewModel: ReservasViewModel = viewModel()
) {
    val reservas by viewModel.reservas.observeAsState(emptyList())

    Scaffold(
        containerColor = Color(0xFFEAF1F8)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mis reservas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (reservas.isEmpty()) {
                EmptyState("No tienes reservas registradas.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(reservas, key = { it.id }) { reserva ->
                        TarjetaMisReserva(
                            reserva = reserva,
                            onCancelar = { viewModel.cancelar(reserva) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMisReserva(
    reserva: ReservaListada,
    onCancelar: () -> Unit
) {
    val esPendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION

    // 🎨 Colores dinámicos basados en la imagen de referencia
    val colorBorde = if (esPendiente) Color(0xFFFDE68A) else Color(0xFFA7F3D0)
    val colorFondoInsignia = if (esPendiente) Color(0xFFFEF3C7) else Color(0xFFE6F4EA)
    val colorTextoInsignia = if (esPendiente) Color(0xFFB45309) else Color(0xFF137333)
    val textoEstado = if (esPendiente) "Pendiente" else "Confirmada"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, colorBorde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = reserva.emoji, fontSize = 26.sp)

                    Column {
                        Text(
                            text = reserva.titulo,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${reserva.fecha} · ${reserva.horaInicio}–${reserva.horaFin}",
                            fontSize = 12.sp,
                            color = TextoSecundario
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = colorFondoInsignia
                ) {
                    Text(
                        text = textoEstado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorTextoInsignia,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            if (!esPendiente) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Asistencia confirmada por el personal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF137333)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cancelar reserva",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextoSecundario,
                modifier = Modifier.clickable { onCancelar() }
            )
        }
    }
}