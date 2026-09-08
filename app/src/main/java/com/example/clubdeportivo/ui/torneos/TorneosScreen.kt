package com.example.clubdeportivo.ui.torneos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import kotlinx.coroutines.launch

@Composable
fun TorneosScreen(viewModel: TorneosViewModel = viewModel()) {
    val torneos by viewModel.torneos.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        when {
            cargando && torneos.isEmpty() -> FullScreenLoading(modifier = Modifier.padding(innerPadding))
            torneos.isEmpty() -> EmptyState(
                mensaje = "No hay torneos disponibles.",
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(torneos, key = { it.id }) { torneo ->
                    TorneoCard(torneo = torneo, onInscribirse = { viewModel.inscribirse(torneo) })
                }
            }
        }
    }
}

@Composable
private fun TorneoCard(torneo: Torneo, onInscribirse: () -> Unit) {
    val cupoLleno = torneo.inscritos >= torneo.cupoMaximo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = torneo.nombre, fontWeight = FontWeight.Bold)
            Text(
                text = "${torneo.disciplina} · ${torneo.fechaInicio} a ${torneo.fechaFin}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { torneo.inscritos.toFloat() / torneo.cupoMaximo.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "${torneo.inscritos} / ${torneo.cupoMaximo}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onInscribirse,
                enabled = !cupoLleno,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(if (cupoLleno) "Cupo lleno" else "Inscribirme")
            }
        }
    }
}
