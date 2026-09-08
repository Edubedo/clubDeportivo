package com.example.clubdeportivo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.InitialsAvatar

private data class AccesoRapido(val titulo: String, val icono: ImageVector, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    onIrAreas: () -> Unit,
    onIrReservas: () -> Unit,
    onIrTorneos: () -> Unit,
    onIrMembresia: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val usuario by viewModel.usuario.observeAsState()

    val accesos = listOf(
        AccesoRapido("Áreas del club", Icons.Filled.Place, onIrAreas),
        AccesoRapido("Mis reservas", Icons.Filled.EventAvailable, onIrReservas),
        AccesoRapido("Torneos", Icons.Filled.EmojiEvents, onIrTorneos),
        AccesoRapido("Mi membresía", Icons.Filled.Workspaces, onIrMembresia),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(nombre = usuario?.nombre ?: "?")
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "¡Hola, ${usuario?.nombre ?: "socio"}!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = usuario?.rol?.nombreLegible() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(accesos) { acceso ->
            Card(
                onClick = acceso.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Icon(
                        imageVector = acceso.icono,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = acceso.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
