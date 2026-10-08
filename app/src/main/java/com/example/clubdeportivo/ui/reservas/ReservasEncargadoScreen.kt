package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.data.model.EstadoReserva
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.ui.components.BotonTonal
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.ExitoSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PeligroSuave
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas

@Composable
fun ReservasEncargadoScreen(
    viewModel: ReservasEncargadoViewModel = viewModel()
) {
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val asistencias by viewModel.asistencias.observeAsState(emptyMap())

    val areaTrabajo = viewModel.areaTrabajo

    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = "Reservas en mi área",
            subtitulo = areaTrabajo.ifBlank { "Sin área asignada" },
            modifier = Modifier.padding(horizontal = MargenPantalla)
        )
        PestanasPildora(
            opciones = listOf("Por aprobar", "Reservas", "Horarios"),
            seleccionada = pestana,
            onSeleccion = { pestana = it }
        )

        // Lo que se aprobó en la otra pestaña tiene que aparecer aquí: se vuelve a leer al entrar.
        LaunchedEffect(pestana) { if (pestana == 1) viewModel.cargarReservas() }

        if (pestana == 0) {
            PorAprobarScreen()
        } else if (pestana == 2) {
            DisponibilidadScreen()
        } else Column(modifier = Modifier.fillMaxSize().padding(horizontal = MargenPantalla)) {
        when {
            cargando -> FullScreenLoading()
            error != null -> EmptyState(mensaje = error.orEmpty(), icono = Icons.Outlined.EventBusy)
            reservas.none { it.estado == EstadoReserva.CONFIRMADA } -> EmptyState(
                mensaje = "No hay reservas confirmadas en $areaTrabajo.",
                icono = Icons.Outlined.EventBusy
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservas.filter { it.estado == EstadoReserva.CONFIRMADA }, key = { it.id }) { reserva ->
                    val nombre = reserva.usuarioNombre.ifBlank { "Usuario" }

                    ReservaEncargadoCard(
                        inicial = nombre.firstOrNull()?.uppercase() ?: "?",
                        nombre = nombre,
                        fecha = Fechas.legible(reserva.fecha),
                        horario = "${reserva.horaInicio}–${reserva.horaFin}",
                        area = "${Deportes.emojiDe(reserva.deporte)} ${reserva.deporte} — ${reserva.areaNombre}",
                        asistencia = asistencias[reserva.id],
                        // La asistencia se toma cuando la reserva ya empezó, no antes.
                        yaEmpezo = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio) <= 0,
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
                }
            }
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
    yaEmpezo: Boolean,
    onAsistio: () -> Unit,
    onNoAsistio: () -> Unit
) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BurbujaTexto(texto = inicial, tamano = 44.dp, tamanoTexto = 18)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$fecha · $horario",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }

                when (asistencia) {
                    "ASISTIO" -> Insignia("Asistió", TipoInsignia.EXITO, icono = Icons.Filled.Check)
                    "NO_ASISTIO" -> Insignia("No se presentó", TipoInsignia.PELIGRO, icono = Icons.Filled.Close)
                    else -> Insignia("Por confirmar", TipoInsignia.ALERTA, icono = Icons.Filled.Schedule)
                }
            }

            Text(
                text = area,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (asistencia == null && !yaEmpezo) {
                Text(
                    text = "La asistencia se registra cuando empieza la reserva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (asistencia == null && yaEmpezo) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BotonTonal(
                        texto = "Asistió",
                        icono = Icons.Filled.Check,
                        fondo = ExitoSuave,
                        color = Exito,
                        onClick = onAsistio,
                        modifier = Modifier.weight(1f)
                    )
                    BotonTonal(
                        texto = "No vino",
                        icono = Icons.Filled.Close,
                        fondo = PeligroSuave,
                        color = Peligro,
                        onClick = onNoAsistio,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
