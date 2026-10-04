package com.example.clubdeportivo.ui.reservas

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel

private val Fondo = Color(0xFFF8FAFD)
private val TextoPrincipal = Color(0xFF111827)
private val TextoSecundario = Color(0xFF94A3B8)
private val Verde = Color(0xFF16A34A)
private val VerdeFondo = Color(0xFFDCFCE7)
private val Amarillo = Color(0xFFF59E0B)
private val AmarilloFondo = Color(0xFFFFFBEB)
private val Rojo = Color(0xFFEF4444)
private val RojoFondo = Color(0xFFFEE2E2)

@Composable
fun ReservasEncargadoScreen(
    viewModel: ReservasEncargadoViewModel = viewModel()
) {

    val reservas by viewModel.reservas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val asistencias by viewModel.asistencias.observeAsState(emptyMap())

    val areaTrabajo = viewModel.areaTrabajo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        Text(
            text = "Reservas en mi área",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )

        Text(
            text = areaTrabajo.ifBlank { "Sin área asignada" },
            fontSize = 14.sp,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (cargando) {

            Text(
                text = "Cargando reservas...",
                color = TextoSecundario
            )

        } else if (error != null) {

            Text(
                text = error ?: "",
                color = Rojo
            )

        } else if (reservas.isEmpty()) {

            Text(
                text = "No hay reservas vigentes en $areaTrabajo.",
                color = TextoSecundario
            )

        } else {

            reservas.forEach { reserva ->

                val nombre = reserva.usuarioNombre.ifBlank { "Usuario" }

                ReservaEncargadoCard(
                    inicial = nombre.firstOrNull()?.uppercase() ?: "?",
                    nombre = nombre,
                    fecha = reserva.fecha,
                    horario = "${reserva.horaInicio}–${reserva.horaFin}",
                    area = "🎾 ${reserva.deporte} — ${reserva.areaNombre}",

                    // Ya NO ponemos null
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier,
                    shape = CircleShape,
                    color = VerdeFondo
                ) {
                    Text(
                        text = inicial,
                        color = Verde,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {

                    Text(
                        text = nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "$fecha · $horario",
                        fontSize = 13.sp,
                        color = TextoSecundario
                    )
                }

                Etiqueta(
                    texto = "Agendó",
                    fondo = VerdeFondo,
                    textoColor = Verde
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = AmarilloFondo
            ) {
                Text(
                    text = area,
                    color = Amarillo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (asistencia) {

                "ASISTIO" -> {
                    Etiqueta(
                        texto = "✓ Asistió",
                        fondo = VerdeFondo,
                        textoColor = Verde
                    )
                }

                "NO_ASISTIO" -> {
                    Etiqueta(
                        texto = "✕ No se presentó",
                        fondo = RojoFondo,
                        textoColor = Rojo
                    )
                }

                else -> {
                    Text(
                        text = "CONFIRMAR ASISTENCIA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoSecundario
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Surface(
                            onClick = onAsistio,
                            shape = RoundedCornerShape(12.dp),
                            color = VerdeFondo
                        ) {
                            Text(
                                text = "✓ Asistió",
                                color = Verde,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(
                                    horizontal = 18.dp,
                                    vertical = 12.dp
                                )
                            )
                        }

                        Surface(
                            onClick = onNoAsistio,
                            shape = RoundedCornerShape(12.dp),
                            color = RojoFondo
                        ) {
                            Text(
                                text = "✕ No vino",
                                color = Rojo,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(
                                    horizontal = 18.dp,
                                    vertical = 12.dp
                                )
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
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
        )
    }
}