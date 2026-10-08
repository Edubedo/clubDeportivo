package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

/** Disponibilidad hora por hora del área: cuánto cupo hay, cuánto está en revisión y qué horas bloquea un torneo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponibilidadScreen(viewModel: DisponibilidadViewModel = viewModel()) {
    val area = viewModel.area

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { viewModel.cargar() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewModel.areas.size > 1) {
                PestanasPildora(
                    opciones = viewModel.areas.map { it.nombre },
                    seleccionada = viewModel.areas.indexOfFirst { it.id == area?.id }.coerceAtLeast(0),
                    onSeleccion = { viewModel.elegirArea(viewModel.areas[it]) }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MargenPantalla, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.fechas.forEach { (fecha, etiqueta) ->
                    val activa = fecha == viewModel.fecha
                    Surface(
                        onClick = { viewModel.elegirFecha(fecha) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activa) Marca else Superficie,
                        border = if (activa) null else BorderStroke(1.dp, BordeCampo)
                    ) {
                        Text(
                            text = etiqueta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activa) SobreMarca else TextoSecundario,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            when {
                viewModel.cargando && viewModel.filas.isEmpty() -> FullScreenLoading()
                viewModel.error != null -> EmptyState(mensaje = viewModel.error.orEmpty(), icono = Icons.Outlined.EventBusy)
                area == null -> EmptyState(
                    mensaje = "No hay áreas de tu deporte registradas, o todavía no tienes un área asignada.",
                    icono = Icons.Outlined.EventBusy
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) {
                        item {
                            Insignia(texto = "${area.nombre} está en mantenimiento: no admite reservas nuevas", tipo = TipoInsignia.ALERTA)
                        }
                    }
                    items(viewModel.filas, key = { it.hora }) { fila -> FilaHoraCard(fila, area.capacidad) }
                }
            }
        }
    }
}

@Composable
private fun FilaHoraCard(fila: FilaDisponibilidad, capacidad: Int) {
    val estado = fila.estado
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%02d:00 – %02d:00".format(fila.hora, fila.hora + 1),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal
                )
                Text(
                    text = when {
                        estado.torneo != null -> "Torneo: ${estado.torneo.nombre}"
                        else -> "${estado.ocupadas} de $capacidad lugares ocupados" +
                            if (fila.enRevision > 0) " (${fila.enRevision} en revisión)" else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            when {
                estado.torneo != null -> Insignia("Torneo", TipoInsignia.MARCA)
                estado.lleno -> Insignia("Lleno", TipoInsignia.PELIGRO)
                estado.ocupadas > 0 -> Insignia("${estado.libres} libres", TipoInsignia.ALERTA)
                else -> Insignia("Libre", TipoInsignia.EXITO)
            }
        }
    }
}
