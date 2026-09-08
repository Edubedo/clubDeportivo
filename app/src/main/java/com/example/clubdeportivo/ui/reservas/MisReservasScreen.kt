package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import kotlinx.coroutines.launch

@Composable
fun MisReservasScreen(viewModel: MisReservasViewModel = viewModel()) {
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val mensajeCancelacion by viewModel.mensajeCancelacion.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mensajeCancelacion) {
        mensajeCancelacion?.let { mensaje ->
            scope.launch { snackbarHostState.showSnackbar(mensaje) }
            viewModel.onMensajeCancelacionMostrado()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        when {
            cargando && reservas.isEmpty() -> FullScreenLoading(modifier = Modifier.padding(innerPadding))
            reservas.isEmpty() -> EmptyState(
                mensaje = "Todavía no tienes reservas.",
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(reservas, key = { it.reservaId }) { reserva ->
                    ReservaCard(reserva = reserva, onCancelar = { viewModel.cancelar(reserva) })
                }
            }
        }
    }
}

@Composable
private fun ReservaCard(reserva: ReservaConNombreArea, onCancelar: () -> Unit) {
    val cancelable = reserva.estado == EstadoReserva.CONFIRMADA ||
        reserva.estado == EstadoReserva.PENDIENTE_APROBACION

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = reserva.nombreArea, fontWeight = FontWeight.Bold)
            Text(
                text = "${reserva.fecha} · ${reserva.horaInicio} - ${reserva.horaFin}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(reserva.estado.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = estadoColor(reserva.estado),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (cancelable) {
                    TextButton(onClick = onCancelar, modifier = Modifier.padding(start = 4.dp)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun estadoColor(estado: EstadoReserva) = when (estado) {
    EstadoReserva.CONFIRMADA -> MaterialTheme.colorScheme.primaryContainer
    EstadoReserva.PENDIENTE_APROBACION -> MaterialTheme.colorScheme.tertiaryContainer
    EstadoReserva.CANCELADA -> MaterialTheme.colorScheme.errorContainer
    EstadoReserva.FINALIZADA -> MaterialTheme.colorScheme.surfaceVariant
}
