package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.ui.components.BotonTonal
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.ExitoSuave
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PeligroSuave
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas

/** Lista de reservas de visitantes que esperan una respuesta del personal: aprobar o rechazar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PorAprobarScreen(viewModel: AprobacionesViewModel = viewModel()) {
    val pendientes = viewModel.pendientes
    var aRechazar by remember { mutableStateOf<Reserva?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.cargar() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                pendientes == null -> FullScreenLoading()
                pendientes.isEmpty() -> EmptyState(
                    mensaje = "No hay reservas esperando aprobación.",
                    icono = Icons.Outlined.TaskAlt
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendientes, key = { it.id }) { reserva ->
                        TarjetaPorAprobar(
                            reserva = reserva,
                            ocupada = reserva.id in viewModel.enProceso,
                            onAprobar = { viewModel.aprobar(reserva) },
                            onRechazar = { aRechazar = reserva }
                        )
                    }
                }
            }
        }
    }

    aRechazar?.let { reserva ->
        DialogoConfirmacion(
            titulo = "Rechazar reserva",
            mensaje = "¿Rechazar la reserva de ${reserva.usuarioNombre.ifBlank { "este visitante" }} del " +
                "${Fechas.legible(reserva.fecha)} a las ${reserva.horaInicio}? Se cancela y el lugar queda libre.",
            textoConfirmar = "Rechazar",
            textoCancelar = "Volver",
            onConfirmar = {
                viewModel.rechazar(reserva)
                aRechazar = null
            },
            onCancelar = { aRechazar = null }
        )
    }
}

@Composable
private fun TarjetaPorAprobar(reserva: Reserva, ocupada: Boolean, onAprobar: () -> Unit, onRechazar: () -> Unit) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BurbujaTexto(texto = Deportes.emojiDe(reserva.deporte), tamano = 48.dp, tamanoTexto = 24)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reserva.usuarioNombre.ifBlank { "Visitante" },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = Deportes.titulo(reserva.deporte, reserva.areaNombre),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Fechas.legible(reserva.fecha)} · ${reserva.horaInicio}–${reserva.horaFin} · " +
                            if (reserva.personas == 1) "1 persona" else "${reserva.personas} personas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                Insignia(texto = "Pendiente", tipo = TipoInsignia.ALERTA)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BotonTonal(
                    texto = "Aprobar",
                    icono = Icons.Filled.Check,
                    fondo = ExitoSuave,
                    color = Exito,
                    onClick = { if (!ocupada) onAprobar() },
                    modifier = Modifier.weight(1f)
                )
                BotonTonal(
                    texto = "Rechazar",
                    icono = Icons.Filled.Close,
                    fondo = PeligroSuave,
                    color = Peligro,
                    onClick = { if (!ocupada) onRechazar() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
