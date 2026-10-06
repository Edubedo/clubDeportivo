package com.example.clubdeportivo.ui.inventario

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel = viewModel()
) {
    val articulos by viewModel.articulos.observeAsState(emptyList())
    val filtroDeporte by viewModel.filtroDeporte.observeAsState("Todos")
    val mostrarModal by viewModel.mostrarModal.observeAsState(false)
    val articuloEnEdicion by viewModel.articuloEnEdicion.observeAsState()
    val historial by viewModel.historial.observeAsState(emptyList())
    val articulosFiltrados = if (filtroDeporte == "Todos") {
        articulos
    } else {
        articulos.filter { it.deporte == filtroDeporte }
    }

    var articuloAEliminar by remember { mutableStateOf<ArticuloInventario?>(null) }
    var mostrarHistorial by remember { mutableStateOf(false) }

    // 🚀 Scaffolding con el botón flotante real flotando por encima del contenido y la barra inferior
    Scaffold(
        containerColor = Color(0xFFEAF1F8),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.abrirModalAgregar() },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Artículo", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF1E2E4F),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFEAF1F8))
        ) {

            Spacer(modifier = Modifier.height(16.dp))

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
                        targetValue = if (esSeleccionado) Color(0xFFD6E4FE) else Color.White
                    )
                    val colorTexto by animateColorAsState(
                        targetValue = if (esSeleccionado) Color(0xFF1E2E4F) else Color(0xFF31487A)
                    )
//CLB-J2B3KF
                    Surface(
                        modifier = Modifier.size(width = 130.dp, height = 44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = colorFondo,
                        onClick = { viewModel.filtrarPorDeporte(deporte) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deporte,
                                fontSize = 13.sp,
                                fontWeight = if (esSeleccionado) FontWeight.SemiBold else FontWeight.Medium,
                                color = colorTexto
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (articulosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay artículos en esta categoría todavía",
                        fontSize = 14.sp,
                        color = Color(0xFF31487A)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    // 🌟 Espacio inferior amplio (96.dp) para que la lista nunca quede oculta tras el FAB
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articulosFiltrados, key = { it.id }) { articulo ->
                        ArticuloCard(
                            articulo = articulo,
                            onMas = { viewModel.incrementarCantidad(articulo) },
                            onMenos = { viewModel.decrementarCantidad(articulo) },
                            onEditar = { viewModel.abrirModalEditar(articulo) },
                            onEliminar = { articuloAEliminar = articulo }
                        )
                    }
                }
            }
        }
    }

    if (mostrarModal) {
        ModalArticulo(
            articuloEnEdicion = articuloEnEdicion,
            deportes = viewModel.obtenerDeportes().filterNot { it == "Todos" },
            existeNombre = { nombre, id -> viewModel.existeNombre(nombre, id) },
            onGuardar = { id, nombre, deporte, cantidad, stockMinimo ->
                if (id != null) {
                    viewModel.editarArticulo(id, nombre, deporte, cantidad, stockMinimo)
                } else {
                    viewModel.agregarArticulo(nombre, deporte, cantidad, stockMinimo)
                }
            },
            onCerrar = { viewModel.cerrarModalAgregar() }
        )
    }

    articuloAEliminar?.let { articulo ->
        AlertDialog(
            onDismissRequest = { articuloAEliminar = null },
            title = { Text("Eliminar artículo") },
            text = { Text("¿Seguro que deseas eliminar \"${articulo.nombre}\" del inventario? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarArticulo(articulo)
                    articuloAEliminar = null
                }) {
                    Text("Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { articuloAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarHistorial) {
        ModalHistorialActividad(
            historial = historial,
            onCerrar = { mostrarHistorial = false }
        )
    }
}

@Composable
fun ArticuloCard(
    articulo: ArticuloInventario,
    onMas: () -> Unit,
    onMenos: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val stockBajo = articulo.cantidad <= articulo.stockMinimo

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = articulo.icono, fontSize = 28.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = articulo.nombre,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF192338),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = articulo.deporte,
                            fontSize = 12.sp,
                            color = Color(0xFF31487A)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar ${articulo.nombre}",
                            tint = Color(0xFF31487A)
                        )
                    }
                    IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar ${articulo.nombre}",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (stockBajo) "⚠ Stock bajo (mín. ${articulo.stockMinimo})" else "Stock mínimo: ${articulo.stockMinimo}",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = if (stockBajo) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (stockBajo) Color(0xFFEF4444) else Color(0xFF31487A)
                )

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
            enabled = cantidad > 0,
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEAF1F8), RoundedCornerShape(10.dp))
        ) {
            Text(
                text = "−",
                fontSize = 18.sp,
                color = if (cantidad > 0) Color(0xFF192338) else Color(0xFFD1D5DB)
            )
        }

        Text(
            text = cantidad.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF192338),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 30.dp)
        )

        IconButton(
            onClick = onMas,
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEAF1F8), RoundedCornerShape(10.dp))
        ) {
            Text(text = "+", fontSize = 18.sp, color = Color(0xFF192338))
        }
    }
}

@Composable
private fun ModalArticulo(
    articuloEnEdicion: ArticuloInventario?,
    deportes: List<String>,
    existeNombre: (nombre: String, excluirId: String?) -> Boolean,
    onGuardar: (id: String?, nombre: String, deporte: String, cantidad: Int, stockMinimo: Int) -> Boolean,
    onCerrar: () -> Unit
) {
    val enEdicion = articuloEnEdicion != null
    var nombre by remember(articuloEnEdicion) { mutableStateOf(articuloEnEdicion?.nombre ?: "") }
    var deporteSeleccionado by remember(articuloEnEdicion) {
        mutableStateOf(articuloEnEdicion?.deporte ?: deportes.firstOrNull() ?: "")
    }
    var cantidad by remember(articuloEnEdicion) {
        mutableStateOf((articuloEnEdicion?.cantidad ?: 1).toString())
    }
    var stockMinimo by remember(articuloEnEdicion) {
        mutableStateOf((articuloEnEdicion?.stockMinimo ?: 1).toString())
    }
    var mostrarDropdown by remember { mutableStateOf(false) }

    val nombreRepetido = nombre.isNotBlank() && existeNombre(nombre, articuloEnEdicion?.id)

    DialogoFormulario(
        titulo = if (enEdicion) "Editar artículo" else "Agregar artículo",
        onCerrar = onCerrar
    ) {
        EtiquetaCampo("NOMBRE DEL ARTÍCULO")
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = "Ej: Raquetas",
            isError = nombreRepetido,
            mensajeError = if (nombreRepetido) "Ya existe un artículo con ese nombre en el inventario" else null
        )

        EspacioCampos()

        EtiquetaCampo("DEPORTE")
        Box(modifier = Modifier.fillMaxWidth()) {
            CampoTexto(
                value = deporteSeleccionado,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { Text("▼") }
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
                                color = if (esSeleccionado) Color(0xFFD6E4FE) else Color.White,
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
                                    Text(text = Deportes.emojiDe(deporte))
                                    Text(
                                        text = deporte,
                                        fontSize = 14.sp,
                                        color = if (esSeleccionado) Color(0xFF1E2E4F) else Color(0xFF192338),
                                        fontWeight = if (esSeleccionado) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        EspacioCampos()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                EtiquetaCampo(if (enEdicion) "CANTIDAD" else "CANTIDAD INICIAL")
                CampoTexto(
                    value = cantidad,
                    onValueChange = { cantidad = it.filter(Char::isDigit).take(5) },
                    tipoTeclado = KeyboardType.Number
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                EtiquetaCampo("STOCK MÍNIMO")
                CampoTexto(
                    value = stockMinimo,
                    onValueChange = { stockMinimo = it.filter(Char::isDigit).take(5) },
                    tipoTeclado = KeyboardType.Number
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrimario(
            texto = if (enEdicion) "Guardar cambios" else "Agregar",
            enabled = nombre.isNotBlank() && deporteSeleccionado.isNotBlank() && !nombreRepetido,
            onClick = {
                onGuardar(
                    articuloEnEdicion?.id,
                    nombre.trim(),
                    deporteSeleccionado,
                    cantidad.toIntOrNull() ?: 1,
                    stockMinimo.toIntOrNull() ?: 1
                )
            }
        )
    }
}

@Composable
private fun ModalHistorialActividad(
    historial: List<ActividadInventario>,
    onCerrar: () -> Unit
) {
    val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    DialogoFormulario(titulo = "Actividad reciente", onCerrar = onCerrar) {
        if (historial.isEmpty()) {
            Text(
                text = "Todavía no hay movimientos registrados en el inventario.",
                fontSize = 14.sp,
                color = Color(0xFF31487A),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            historial.forEach { actividad ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = actividad.descripcion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF192338)
                    )
                    Text(
                        text = formato.format(Date(actividad.timestamp)),
                        fontSize = 12.sp,
                        color = Color(0xFF31487A)
                    )
                }
            }
        }
    }
}