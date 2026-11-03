
package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.util.Fechas
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasSupervisorScreen(
    viewModel: ReservasSupervisorViewModel = viewModel()
) {
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val mensaje by viewModel.mensaje.observeAsState()

    var areaSeleccionada by rememberSaveable {
        mutableStateOf("Todas las áreas")
    }
    var estadoSeleccionado by rememberSaveable {
        mutableStateOf("Todos")
    }
    var fechaSeleccionada by rememberSaveable {
        mutableStateOf("")
    }

    var menuAreas by remember { mutableStateOf(false) }
    var menuEstados by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var reservaARechazar by remember { mutableStateOf<Reserva?>(null) }
    var motivoRechazo by remember { mutableStateOf("") }

    val areas = remember(reservas) {
        listOf("Todas las áreas") +
                reservas.map { it.areaNombre.ifBlank { it.areaId } }
                    .distinct()
                    .sorted()
    }

    val reservasFiltradas = reservas.filter { reserva ->
        val nombreArea = reserva.areaNombre.ifBlank { reserva.areaId }

        val coincideArea =
            areaSeleccionada == "Todas las áreas" ||
                    nombreArea == areaSeleccionada

        val coincideEstado = when (estadoSeleccionado) {
            "Pendientes" ->
                reserva.estado == EstadoReserva.PENDIENTE_APROBACION
            "Confirmadas" ->
                reserva.estado == EstadoReserva.CONFIRMADA
            "Rechazadas" ->
                reserva.estado == EstadoReserva.RECHAZADA
            else -> true
        }

        val coincideFecha =
            fechaSeleccionada.isBlank() ||
                    reserva.fecha == fechaSeleccionada

        coincideArea && coincideEstado && coincideFecha
    }

    LaunchedEffect(Unit) {
        viewModel.cargarReservas()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        EncabezadoPantalla(
            titulo = "Gestión de reservas",
            subtitulo = "Reservas de todas las áreas del club",
            modifier = Modifier.padding(horizontal = MargenPantalla)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MargenPantalla,
                end = MargenPantalla,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumenSupervisor(
                        titulo = "Pendientes",
                        cantidad = reservas.count {
                            it.estado == EstadoReserva.PENDIENTE_APROBACION
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ResumenSupervisor(
                        titulo = "Confirmadas",
                        cantidad = reservas.count {
                            it.estado == EstadoReserva.CONFIRMADA
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Filtrar reservas",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = menuAreas,
                    onExpandedChange = { menuAreas = it }
                ) {
                    OutlinedTextField(
                        value = areaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Área deportiva") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = menuAreas
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = menuAreas,
                        onDismissRequest = { menuAreas = false }
                    ) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area) },
                                onClick = {
                                    areaSeleccionada = area
                                    menuAreas = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = menuEstados,
                    onExpandedChange = { menuEstados = it }
                ) {
                    OutlinedTextField(
                        value = estadoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado de reserva") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = menuEstados
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = menuEstados,
                        onDismissRequest = { menuEstados = false }
                    ) {
                        listOf(
                            "Todos",
                            "Pendientes",
                            "Confirmadas",
                            "Rechazadas"
                        ).forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(estado) },
                                onClick = {
                                    estadoSeleccionado = estado
                                    menuEstados = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { mostrarCalendario = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (fechaSeleccionada.isBlank())
                                "Todas las fechas"
                            else fechaSeleccionada
                        )
                    }

                    if (fechaSeleccionada.isNotBlank()) {
                        TextButton(
                            onClick = { fechaSeleccionada = "" }
                        ) {
                            Text("Limpiar")
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (mensaje != null) {
                item {
                    Text(
                        text = mensaje.orEmpty(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (cargando) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text(
                    text = "Reservas (${reservasFiltradas.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (reservasFiltradas.isEmpty() && !cargando) {
                item {
                    Text("No se encontraron reservas con estos filtros.")
                }
            }

            items(reservasFiltradas, key = { it.id }) { reserva ->
                TarjetaReservaSupervisor(
                    reserva = reserva,
                    habilitado = !cargando,
                    onAprobar = {
                        viewModel.aprobarReserva(reserva.id)
                    },
                    onRechazar = {
                        reservaARechazar = reserva
                        motivoRechazo = ""
                    }
                )
            }
        }
    }

    if (mostrarCalendario) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            fechaSeleccionada =
                                java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                        }
                        mostrarCalendario = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarCalendario = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (reservaARechazar != null) {
        AlertDialog(
            onDismissRequest = { reservaARechazar = null },
            title = { Text("Rechazar reserva") },
            text = {
                Column {
                    Text("Escribe el motivo del rechazo:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = motivoRechazo,
                        onValueChange = { motivoRechazo = it },
                        label = { Text("Motivo") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = motivoRechazo.isNotBlank() && !cargando,
                    onClick = {
                        reservaARechazar?.let {
                            viewModel.rechazarReserva(
                                reservaId = it.id,
                                motivo = motivoRechazo
                            )
                        }
                        reservaARechazar = null
                    }
                ) {
                    Text("Rechazar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { reservaARechazar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ResumenSupervisor(
    titulo: String,
    cantidad: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = cantidad.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TarjetaReservaSupervisor(
    reserva: Reserva,
    habilitado: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = reserva.areaNombre.ifBlank { reserva.areaId },
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = reserva.usuarioNombre.ifBlank { "Miembro" }
            )

            Text(
                text = "${Fechas.legible(reserva.fecha)} · " +
                        "${reserva.horaInicio}–${reserva.horaFin}"
            )

            Text(
                text = when (reserva.estado) {
                    EstadoReserva.PENDIENTE_APROBACION -> "Pendiente"
                    EstadoReserva.CONFIRMADA -> "Confirmada"
                    EstadoReserva.RECHAZADA -> "Rechazada"
                    else -> reserva.estado.name
                },
                color = MaterialTheme.colorScheme.secondary
            )

            if (reserva.estado == EstadoReserva.PENDIENTE_APROBACION) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRechazar,
                        enabled = habilitado,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Rechazar")
                    }

                    Button(
                        onClick = onAprobar,
                        enabled = habilitado,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Aprobar")
                    }
                }
            }
        }
    }
}
