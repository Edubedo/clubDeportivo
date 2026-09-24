package com.example.clubdeportivo.ui.inventario

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel = viewModel()
) {
    val articulos by viewModel.articulos.observeAsState(emptyList())
    val filtroDeporte by viewModel.filtroDeporte.observeAsState("Todos")
    val mostrarModal by viewModel.mostrarModal.observeAsState(false)
    val articulosFiltrados = viewModel.obtenerArticulosFiltrados()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFD))
                .padding(bottom = 80.dp)
        ) {
            // Encabezado
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Club Deportivo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "Admin",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título Inventario
            Text(
                text = "Inventario",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs/Chips horizontales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.obtenerDeportes().forEach { deporte ->
                    val esSeleccionado = filtroDeporte == deporte
                    val colorFondo by animateColorAsState(
                        targetValue = if (esSeleccionado) Color(0xFFD1FAE5) else Color.White
                    )
                    val colorTexto by animateColorAsState(
                        targetValue = if (esSeleccionado) Color(0xFF10B981) else Color(0xFFB0B9C6)
                    )

                    Surface(
                        modifier = Modifier.size(width = 130.dp, height = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = colorFondo,
                        onClick = { viewModel.filtrarPorDeporte(deporte) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deporte,
                                fontSize = 14.sp,
                                fontWeight = if (esSeleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                color = colorTexto
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lista de artículos
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(articulosFiltrados) { articulo ->
                    ArticuloCard(
                        articulo = articulo,
                        onMas = { viewModel.incrementarCantidad(articulo) },
                        onMenos = { viewModel.decrementarCantidad(articulo) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Botón flotante
        FloatingActionButton(
            onClick = { viewModel.abrirModalAgregar() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Color(0xFF10B981)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Agregar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Artículo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Modal para agregar artículos
    if (mostrarModal) {
        ModalAgregarArticulo(
            deportes = viewModel.obtenerDeportes().filterNot { it == "Todos" },
            onAgregar = { nombre, deporte, cantidad, stockMinimo ->
                viewModel.agregarArticulo(nombre, deporte, cantidad, stockMinimo)
            },
            onCerrar = { viewModel.cerrarModalAgregar() }
        )
    }
}

@Composable
fun ArticuloCard(
    articulo: ArticuloInventario,
    onMas: () -> Unit,
    onMenos: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icono y nombre
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = articulo.icono, fontSize = 32.sp)

                Column {
                    Text(
                        text = articulo.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = articulo.deporte,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Controles de cantidad
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotonesCantidad(
                    cantidad = articulo.cantidad,
                    onMas = onMas,
                    onMenos = onMenos
                )
            }
        }
    }
}

@Composable
fun BotonesCantidad(
    cantidad: Int,
    onMas: () -> Unit,
    onMenos: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onMenos,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
        ) {
            Text(text = "−", fontSize = 20.sp, color = Color(0xFF64748B))
        }

        Text(
            text = cantidad.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier.width(30.dp)
        )

        IconButton(
            onClick = onMas,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
        ) {
            Text(text = "+", fontSize = 20.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun ModalAgregarArticulo(
    deportes: List<String>,
    onAgregar: (String, String, Int, Int) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var deporteSeleccionado by remember { mutableStateOf(deportes.firstOrNull() ?: "") }
    var cantidad by remember { mutableStateOf("1") }
    var stockMinimo by remember { mutableStateOf("1") }
    var mostrarDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Encabezado del modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "+ Agregar artículo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    IconButton(onClick = onCerrar) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Campo nombre
                Text(
                    text = "NOMBRE DEL ARTÍCULO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Ej: Raquetas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dropdown deporte
                Text(
                    text = "DEPORTE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = deporteSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Text("▼") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        color = Color.Transparent,
                        onClick = { mostrarDropdown = !mostrarDropdown }
                    ) {}

                    if (mostrarDropdown) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                deportes.forEach { deporte ->
                                    val esSeleccionado = deporteSeleccionado == deporte
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        color = if (esSeleccionado) Color(0xFFD1FAE5) else Color.White,
                                        onClick = {
                                            deporteSeleccionado = deporte
                                            mostrarDropdown = false
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = when (deporte) {
                                                    "Baloncesto" -> "🏀"
                                                    "Voleibol" -> "🏐"
                                                    "Tenis" -> "🎾"
                                                    "Fútbol" -> "⚽"
                                                    "Natación" -> "🏊"
                                                    else -> "📦"
                                                }
                                            )
                                            Text(
                                                text = deporte,
                                                fontSize = 14.sp,
                                                color = if (esSeleccionado) Color(0xFF10B981) else Color(0xFF111827),
                                                fontWeight = if (esSeleccionado) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cantidad inicial y stock mínimo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CANTIDAD INICIAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = cantidad,
                            onValueChange = { cantidad = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STOCK MÍNIMO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = stockMinimo,
                            onValueChange = { stockMinimo = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Agregar
                Button(
                    onClick = {
                        if (nombre.isNotBlank() && deporteSeleccionado.isNotBlank()) {
                            onAgregar(
                                nombre,
                                deporteSeleccionado,
                                cantidad.toIntOrNull() ?: 1,
                                stockMinimo.toIntOrNull() ?: 1
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text(
                        text = "Agregar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
