package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.theme.AreaDisponibleBg
import com.example.clubdeportivo.ui.theme.AreaDisponibleText
import com.example.clubdeportivo.ui.theme.AreaMantenimientoBg
import com.example.clubdeportivo.ui.theme.AreaMantenimientoText
import com.example.clubdeportivo.ui.theme.AreaOcupadaBg
import com.example.clubdeportivo.ui.theme.AreaOcupadaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    onAreaClick: (Area) -> Unit,
    viewModel: AreasViewModel = viewModel()
) {
    val areas by viewModel.areas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)

    PullToRefreshBox(
        isRefreshing = cargando,
        onRefresh = { viewModel.cargarAreas() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (areas.isEmpty() && !cargando) {
            EmptyState(mensaje = "No hay áreas para mostrar.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(areas, key = { it.id }) { area ->
                    AreaCard(area = area, onClick = { onAreaClick(area) })
                }
            }
        }
    }
}

@Composable
private fun AreaCard(area: Area, onClick: () -> Unit) {
    val disponible = area.disponibilidad == DisponibilidadArea.DISPONIBLE
    val (fondo, texto, etiqueta) = when (area.disponibilidad) {
        DisponibilidadArea.DISPONIBLE -> Triple(AreaDisponibleBg, AreaDisponibleText, "DISPONIBLE")
        DisponibilidadArea.OCUPADA -> Triple(AreaOcupadaBg, AreaOcupadaText, "OCUPADA")
        DisponibilidadArea.MANTENIMIENTO -> Triple(AreaMantenimientoBg, AreaMantenimientoText, "MANTENIMIENTO")
    }

    Card(
        onClick = onClick,
        enabled = disponible,
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = fondo, disabledContainerColor = fondo)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = area.nombre,
                color = texto,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${area.tipo} · ${area.capacidad} personas",
                color = texto,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = etiqueta,
                color = texto,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
