package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.personal.PersonalNotificacionesSheet

private val TextoPrincipal = Color(0xFF192338)
private val TextoSecundario = Color(0xFF31487A)

// 🎨 Definición de colores solicitados para los estados:
private val ColorVerdeTexto = Color(0xFF137333)
private val ColorVerdeFondo = Color(0xFFE6F4EA)

private val ColorNaranjaTexto = Color(0xFFB45309)
private val ColorNaranjaFondo = Color(0xFFFEF3C7)

private val ColorRojoTexto = Color(0xFFC5221F)
private val ColorRojoFondo = Color(0xFFFCE8E6)

@Composable
fun ReservasEncargadoScreen(
    viewModel: ReservasEncargadoViewModel = viewModel()
) {
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val asistencias by viewModel.asistencias.observeAsState(emptyMap())

    val areaTrabajo = viewModel.areaTrabajo
    var mostrarNotificaciones by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF1F8))
            .verticalScroll(rememberScrollState())
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reservas en mi área",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = areaTrabajo.ifBlank { "Sin área asignada" },
                        fontSize = 13.sp,
                        color = TextoSecundario,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Button(
                    onClick = { mostrarNotificaciones = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD6E4FE),
                        contentColor = Color(0xFF1E2E4F)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("📢 Notif.", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (cargando) {
                Text(text = "Cargando reservas...", color = TextoSecundario)
            } else if (error != null) {
                Text(text = error ?: "", color = ColorRojoTexto)
            } else if (reservas.isEmpty()) {
                Text(text = "No hay reservas vigentes en $areaTrabajo.", color = TextoSecundario)
            } else {
                reservas.forEach { reserva ->
                    val nombre = reserva.usuarioNombre.ifBlank { "Usuario" }

                    ReservaEncargadoCard(
                        inicial = nombre.firstOrNull()?.uppercase() ?: "?",
                        nombre = nombre,
                        fecha = reserva.fecha,
                        horario = "${reserva.horaInicio}–${reserva.horaFin}",
                        area = "🎾 ${reserva.deporte} — ${reserva.areaNombre}",
                        asistencia = asistencias[reserva.id],
                        onAsistio = {
                            viewModel.registrarAsistencia(
                                reservaId = reserva.id,
                                usuarioId = reserva.usuarioId,
                                asistencia = "ASISTIO"
                            )
                        },
                        onNoAsistio = {
                            viewModel.registrarAsistencia(
                                reservaId = reserva.id,
                                usuarioId = reserva.usuarioId,
                                asistencia = "NO_ASISTIO"
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (mostrarNotificaciones) {
        PersonalNotificacionesSheet(
            onCerrar = { mostrarNotificaciones = false }
        )
    }
}

@Composable
private fun ReservaEncargadoCard(
    inicial: String,
    nombre: String,
    fecha: String,
    horario: String,
    area: String,
    asistencia: String?,
    onAsistio: () -> Unit,
    onNoAsistio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier,
                    shape = CircleShape,
                    color = ColorVerdeFondo
                ) {
                    Text(
                        text = inicial,
                        color = ColorVerdeTexto,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "$fecha · $horario",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }

                // Estado "Agendó" en verde
                Etiqueta(texto = "Agendó", fondo = ColorVerdeFondo, textoColor = ColorVerdeTexto)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = ColorNaranjaFondo
            ) {
                Text(
                    text = area,
                    color = ColorNaranjaTexto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (asistencia) {
                "ASISTIO" -> {
                    // Confirmado / Asistió -> VERDE
                    Etiqueta(texto = "✓ Asistió", fondo = ColorVerdeFondo, textoColor = ColorVerdeTexto)
                }
                "NO_ASISTIO" -> {
                    // No asistió -> ROJO
                    Etiqueta(texto = "✕ No se presentó", fondo = ColorRojoFondo, textoColor = ColorRojoTexto)
                }
                else -> {
                    // Pendiente -> NARANJA
                    Etiqueta(texto = "⏳ Pendiente de asistencia", fondo = ColorNaranjaFondo, textoColor = ColorNaranjaTexto)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CONFIRMAR ASISTENCIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoSecundario
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = onAsistio,
                            shape = RoundedCornerShape(10.dp),
                            color = ColorVerdeFondo
                        ) {
                            Text(
                                text = "✓ Asistió",
                                color = ColorVerdeTexto,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        Surface(
                            onClick = onNoAsistio,
                            shape = RoundedCornerShape(10.dp),
                            color = ColorRojoFondo
                        ) {
                            Text(
                                text = "✕ No vino",
                                color = ColorRojoTexto,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(
    texto: String,
    fondo: Color,
    textoColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = fondo
    ) {
        Text(
            text = texto,
            color = textoColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}