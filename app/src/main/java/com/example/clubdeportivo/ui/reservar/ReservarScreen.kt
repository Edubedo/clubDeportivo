package com.example.clubdeportivo.ui.reservar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.util.Fechas

@Composable
fun ReservarScreen(
    areaId: Int,
    areaNombre: String,
    viewModel: ReservarViewModel = viewModel()
) {
    val dias = remember { Fechas.proximosDias(8) }

    var diaSeleccionadoIndex by remember { mutableStateOf(0) }
    var horaSeleccionada by remember { mutableStateOf<String?>(null) }

    val bloqueadoParaExterno by viewModel.bloqueadoParaExterno.observeAsState(false)
    val horarioTexto by viewModel.horarioTexto.observeAsState("")
    val slots by viewModel.slots.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val errorReserva by viewModel.errorReserva.observeAsState()
    val reservaConfirmada by viewModel.reservaConfirmada.observeAsState()
    val materialAsignado by viewModel.materialAsignado.observeAsState(emptyList())

    LaunchedEffect(areaId) { viewModel.cargarArea(areaId) }

    LaunchedEffect(slots) {
        horaSeleccionada = slots.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = areaNombre, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = horarioTexto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (bloqueadoParaExterno) {
            Text(
                text = "Esta área no admite reservaciones de visitantes externos.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
            return@Column
        }

        Text(
            text = "Elige un día (máximo 7 días de anticipación):",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dias.forEachIndexed { index, (fecha, etiqueta) ->
                FilterChip(
                    selected = index == diaSeleccionadoIndex,
                    onClick = {
                        diaSeleccionadoIndex = index
                        viewModel.seleccionarDia(fecha)
                    },
                    label = { Text(etiqueta) }
                )
            }
        }

        Text(
            text = "Elige un horario (mínimo 2 horas de anticipación):",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
        )

        if (slots.isEmpty()) {
            Text(
                text = "No hay horarios disponibles ese día.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slots.forEach { hora ->
                    val horaFin = "%02d:00".format(hora.substring(0, 2).toInt() + 1)
                    FilterChip(
                        selected = hora == horaSeleccionada,
                        onClick = { horaSeleccionada = hora },
                        label = { Text("$hora - $horaFin") }
                    )
                }
            }
        }

        if (errorReserva != null) {
            Text(
                text = errorReserva.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                val fecha = dias[diaSeleccionadoIndex].first
                horaSeleccionada?.let { viewModel.reservar(fecha, it) }
            },
            enabled = !cargando && slots.isNotEmpty() && horaSeleccionada != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirmar reserva")
            }
        }

        reservaConfirmada?.let { reserva ->
            val cantidadMaterial = materialAsignado.sumOf { it.cantidad }
            val detalleMaterial = when {
                reserva.esExterno -> "Tu reservación quedó PENDIENTE DE APROBACIÓN por un administrador."
                cantidadMaterial > 0 -> "Material asignado automáticamente: $cantidadMaterial unidad(es)."
                else -> "Esta área no requiere material adicional."
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Reserva para el ${reserva.fecha} de ${reserva.horaInicio} a ${reserva.horaFin}.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = detalleMaterial,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
