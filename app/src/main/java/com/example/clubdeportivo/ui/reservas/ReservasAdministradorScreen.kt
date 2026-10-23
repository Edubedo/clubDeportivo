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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasAdministradorScreen(
    viewModel: ReservasAdministradorViewModel = viewModel()
) {
    val ui by viewModel.ui.observeAsState(ReservasAdministradorUi())
    var dropdownAbierto by remember { mutableStateOf(false) }
    val areaSeleccionada = ui.areas.firstOrNull { it.id == ui.areaIdSeleccionada }
    val reservas = ui.reservas.filter { ui.areaIdSeleccionada == null || it.areaId == ui.areaIdSeleccionada }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = MargenPantalla)) {
        EncabezadoPantalla(titulo = "Reservas")

        if (ui.cargando && ui.areas.isEmpty()) {
            FullScreenLoading()
            return
        }

        ExposedDropdownMenuBox(
            expanded = dropdownAbierto,
            onExpandedChange = { dropdownAbierto = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = areaSeleccionada?.let { Deportes.titulo(it.tipo, it.nombre) } ?: "Todas las áreas",
                onValueChange = {},
                readOnly = true,
                label = { Text("Área") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownAbierto) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = dropdownAbierto,
                onDismissRequest = { dropdownAbierto = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todas las áreas") },
                    onClick = {
                        viewModel.seleccionarArea(null)
                        dropdownAbierto = false
                    }
                )
                ui.areas.forEach { area ->
                    DropdownMenuItem(
                        text = { Text(Deportes.titulo(area.tipo, area.nombre)) },
                        onClick = {
                            viewModel.seleccionarArea(area.id)
                            dropdownAbierto = false
                        }
                    )
                }
            }
        }

        when {
            ui.cargando -> FullScreenLoading()
            ui.error != null -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(mensaje = ui.error.orEmpty(), icono = Icons.Outlined.EventBusy)
                TextButton(onClick = viewModel::cargar) { Text("Reintentar") }
            }
            reservas.isEmpty() -> EmptyState(
                mensaje = if (areaSeleccionada == null) "No hay reservas registradas." else
                    "No hay reservas para ${areaSeleccionada.nombre}.",
                icono = Icons.Outlined.EventBusy
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservas, key = { it.id }) { reserva ->
                    TarjetaReservaAdministrador(reserva)
                }
            }
        }
    }
}

@Composable
private fun TarjetaReservaAdministrador(reserva: Reserva) {
    val nombre = reserva.usuarioNombre.ifBlank { "Usuario" }
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BurbujaTexto(texto = nombre.firstOrNull()?.uppercase() ?: "?", tamano = 44.dp, tamanoTexto = 18)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Fechas.legible(reserva.fecha)} · ${reserva.horaInicio}–${reserva.horaFin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                when (reserva.estado) {
                    EstadoReserva.CONFIRMADA -> Insignia("Confirmada", TipoInsignia.EXITO, icono = Icons.Filled.Check)
                    EstadoReserva.PENDIENTE_APROBACION ->
                        Insignia("En revisión", TipoInsignia.ALERTA, icono = Icons.Filled.Schedule)
                    else -> Insignia(reserva.estado.name, TipoInsignia.NEUTRO)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${Deportes.emojiDe(reserva.deporte)} ${reserva.areaNombre.ifBlank { reserva.deporte }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            if (reserva.personas > 1 || reserva.carril != null) {
                Text(
                    text = listOfNotNull(
                        if (reserva.personas > 1) "${reserva.personas} personas" else null,
                        reserva.carril?.let { "Carril $it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
