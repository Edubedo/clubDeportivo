package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme

private data class CanchaItem(
    val id: String,
    val deporte: String,
    val emoji: String,
    val nombreCancha: String,
    val reservasHoy: Int,
    val responsable: String? = null
)

private val deportesDisponibles = listOf(
    "Baloncesto" to "🏀",
    "Tenis" to "🎾",
    "Fútbol" to "⚽",
    "Natación" to "🏊",
)

/** Cicla entre los "container" del tema en vez de colores fijos, para que se vea bien en claro y oscuro. */
@Composable
private fun colorParaIndice(indice: Int): Color {
    val colores = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.surfaceVariant,
    )
    return colores[indice % colores.size]
}

@Composable
fun MisReservasScreen() {
    val canchas = remember {
        mutableStateListOf(
            CanchaItem("1", "Baloncesto", "🏀", "Cancha A", 0),
            CanchaItem("2", "Baloncesto", "🏀", "Cancha B", 0),
            CanchaItem("3", "Tenis", "🎾", "Cancha A", 2, responsable = "Carlos Mendez"),
            CanchaItem("4", "Tenis", "🎾", "Cancha B", 0, responsable = "Carlos Mendez"),
            CanchaItem("5", "Natación", "🏊", "Alberca A", 0),
            CanchaItem("6", "Fútbol", "⚽", "Cancha A", 0),
        )
    }

    // null = modal cerrado. Un CanchaItem = editando esa cancha. Un "nuevo" marcador = agregando.
    var canchaEnEdicion by remember { mutableStateOf<CanchaItem?>(null) }
    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var canchaAEliminar by remember { mutableStateOf<CanchaItem?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoAgregar = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar Cancha") },
                containerColor = Color(0xFF22C55E),
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Gestión de Áreas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(canchas, key = { it.id }) { cancha ->
                    val indice = deportesDisponibles.indexOfFirst { it.first == cancha.deporte }.coerceAtLeast(0)
                    CanchaRow(
                        cancha = cancha,
                        indiceColor = indice,
                        onEditar = { canchaEnEdicion = cancha },
                        onEliminar = { canchaAEliminar = cancha }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    // Modal de "Agregar Cancha" (nueva)
    if (mostrarDialogoAgregar) {
        CanchaFormDialog(
            titulo = "+ Agregar Cancha",
            textoBoton = "Agregar cancha",
            deportesExistentes = canchas.map { it.deporte to it.emoji }.distinct(),
            deporteInicial = null,
            nombreCanchaInicial = "",
            onDismiss = { mostrarDialogoAgregar = false },
            onGuardar = { deporte, emoji, nombreCancha ->
                canchas.add(
                    CanchaItem(
                        id = "${System.currentTimeMillis()}",
                        deporte = deporte,
                        emoji = emoji,
                        nombreCancha = nombreCancha,
                        reservasHoy = 0
                    )
                )
                mostrarDialogoAgregar = false
            }
        )
    }

    // Modal de "Editar Cancha" (reutiliza el mismo formulario, precargado)
    canchaEnEdicion?.let { cancha ->
        CanchaFormDialog(
            titulo = "Editar Cancha",
            textoBoton = "Guardar cambios",
            deportesExistentes = canchas.map { it.deporte to it.emoji }.distinct(),
            deporteInicial = cancha.deporte to cancha.emoji,
            nombreCanchaInicial = cancha.nombreCancha,
            onDismiss = { canchaEnEdicion = null },
            onGuardar = { deporte, emoji, nombreCancha ->
                val index = canchas.indexOfFirst { it.id == cancha.id }
                if (index != -1) {
                    canchas[index] = cancha.copy(
                        deporte = deporte,
                        emoji = emoji,
                        nombreCancha = nombreCancha
                    )
                }
                canchaEnEdicion = null
            }
        )
    }

    // Confirmación de eliminar
    canchaAEliminar?.let { cancha ->
        AlertDialog(
            onDismissRequest = { canchaAEliminar = null },
            title = { Text("Eliminar cancha") },
            text = { Text("¿Seguro que quieres eliminar \"${cancha.deporte} — ${cancha.nombreCancha}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        canchas.removeAll { it.id == cancha.id }
                        canchaAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { canchaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CanchaRow(
    cancha: CanchaItem,
    indiceColor: Int,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val fondo = colorParaIndice(indiceColor)
    val titulo = "${cancha.deporte} — ${cancha.nombreCancha}"
    var menuAbierto by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(fondo),
                contentAlignment = Alignment.Center
            ) {
                Text(text = cancha.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, fontWeight = FontWeight.Bold)
                Text(
                    text = "${cancha.reservasHoy} reservas hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                cancha.responsable?.let { responsable ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(responsable, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(top = 6.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEditar()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEliminar()
                        }
                    )
                }
            }
        }
    }
}

private enum class ModoDeporte { EXISTENTE, NUEVO }

/**
 * Formulario de cancha, reutilizado tanto para "Agregar" como para "Editar".
 * Si [deporteInicial] y [nombreCanchaInicial] vienen cargados, el formulario
 * arranca precargado (modo edición); si no, arranca vacío (modo alta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanchaFormDialog(
    titulo: String,
    textoBoton: String,
    deportesExistentes: List<Pair<String, String>>, // nombre a emoji
    deporteInicial: Pair<String, String>?,
    nombreCanchaInicial: String,
    onDismiss: () -> Unit,
    onGuardar: (deporte: String, emoji: String, nombreCancha: String) -> Unit
) {
    var modo by remember { mutableStateOf(ModoDeporte.EXISTENTE) }
    var deporteSeleccionado by remember { mutableStateOf(deporteInicial) }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var nombreDeporteNuevo by remember { mutableStateOf("") }
    var emojiNuevo by remember { mutableStateOf("") }
    var nombreCancha by remember { mutableStateOf(nombreCanchaInicial) }

    val puedeGuardar = when (modo) {
        ModoDeporte.EXISTENTE -> deporteSeleccionado != null && nombreCancha.isNotBlank()
        ModoDeporte.NUEVO -> nombreDeporteNuevo.isNotBlank() && emojiNuevo.isNotBlank() && nombreCancha.isNotBlank()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Deporte existente", "Nuevo deporte").forEachIndexed { index, texto ->
                        SegmentedButton(
                            selected = modo.ordinal == index,
                            onClick = { modo = ModoDeporte.entries[index] },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                        ) {
                            Text(texto)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (modo) {
                    ModoDeporte.EXISTENTE -> {
                        ExposedDropdownMenuBox(
                            expanded = dropdownAbierto,
                            onExpandedChange = { dropdownAbierto = it }
                        ) {
                            OutlinedTextField(
                                value = deporteSeleccionado?.let { "${it.second} ${it.first}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Deporte") },
                                placeholder = { Text("—") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownAbierto)
                                }
                            )
                            DropdownMenu(
                                expanded = dropdownAbierto,
                                onDismissRequest = { dropdownAbierto = false }
                            ) {
                                (deportesExistentes + deportesDisponibles).distinct().forEach { (nombre, emoji) ->
                                    DropdownMenuItem(
                                        text = { Text("$emoji $nombre") },
                                        onClick = {
                                            deporteSeleccionado = nombre to emoji
                                            dropdownAbierto = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ModoDeporte.NUEVO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                OutlinedTextField(
                                    value = nombreDeporteNuevo,
                                    onValueChange = { nombreDeporteNuevo = it },
                                    label = { Text("Nombre del deporte") },
                                    placeholder = { Text("Ej: Pádel") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = emojiNuevo,
                                    onValueChange = { emojiNuevo = it },
                                    label = { Text("Emoji") },
                                    placeholder = { Text("🏆") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nombreCancha,
                    onValueChange = { nombreCancha = it },
                    label = { Text("Nombre de la cancha") },
                    placeholder = { Text("Ej: Cancha C") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        when (modo) {
                            ModoDeporte.EXISTENTE -> deporteSeleccionado?.let {
                                onGuardar(it.first, it.second, nombreCancha)
                            }
                            ModoDeporte.NUEVO -> onGuardar(nombreDeporteNuevo, emojiNuevo, nombreCancha)
                        }
                    },
                    enabled = puedeGuardar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(textoBoton)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MisReservasScreenPreview() {
    ClubDeportivoTheme {
        MisReservasScreen()
    }
}