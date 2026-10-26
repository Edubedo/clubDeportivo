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
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.model.etiquetaArea

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
                    opciones = viewModel.areas.map { etiquetaArea(it.nombre) },
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
                    mensaje = Textos.get("there_are_no_areas_for_your"),
                    icono = Icons.Outlined.EventBusy
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) {
                        item {
                            Insignia(texto = Textos.get("is_under_maintenance_it_doesn_t", etiquetaArea(area.nombre)), tipo = TipoInsignia.MANTENIMIENTO)
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
                        estado.torneo != null -> Textos.get("tournament", estado.torneo.nombre)
                        else -> Textos.get("of_spots_taken", estado.ocupadas, capacidad) +
                            if (fila.enRevision > 0) Textos.get("under_review", fila.enRevision) else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            when {
                estado.torneo != null -> Insignia(Textos.get("occupied_tournament"), TipoInsignia.NO_DISPONIBLE)
                estado.lleno -> Insignia(Textos.get("full_2"), TipoInsignia.NO_DISPONIBLE)
                estado.pocosLugares -> Insignia(Textos.get("few_spots_free", estado.libres), TipoInsignia.ALERTA)
                else -> Insignia(Textos.get("available"), TipoInsignia.DISPONIBLE)
            }
        }
    }
}
