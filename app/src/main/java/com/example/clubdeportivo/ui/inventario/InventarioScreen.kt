package com.example.clubdeportivo.ui.inventario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.ui.components.BotonIcono
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel = viewModel()
) {
    val articulos by viewModel.articulos.observeAsState(emptyList())
    val cargando by viewModel.cargando.observeAsState(true)
    val mensaje by viewModel.mensaje.observeAsState()
    val filtroDeporte by viewModel.filtroDeporte.observeAsState("Todos")
    val mostrarModal by viewModel.mostrarModal.observeAsState(false)
    val articuloEnEdicion by viewModel.articuloEnEdicion.observeAsState()
    val articulosFiltrados = if (filtroDeporte == "Todos") {
        articulos
    } else {
        articulos.filter { it.deporte == filtroDeporte }
    }

    var articuloAEliminar by remember { mutableStateOf<ArticuloInventario?>(null) }
    val deportes by viewModel.deportes.observeAsState(viewModel.obtenerDeportes())
    val listState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            BotonFlotanteAgregar(
                texto = "Agregar artículo",
                visible = listState.botonFlotanteVisible(),
                onClick = { viewModel.abrirModalAgregar() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Un encargado solo tiene el deporte de su área: no hay nada que filtrar.
            if (deportes.size > 1) {
                PestanasPildora(
                    opciones = deportes,
                    seleccionada = deportes.indexOf(filtroDeporte).coerceAtLeast(0),
                    onSeleccion = { viewModel.filtrarPorDeporte(deportes[it]) }
                )
            }

            if (cargando && articulos.isEmpty()) {
                FullScreenLoading()
            } else if (articulosFiltrados.isEmpty()) {
                EmptyState(
                    mensaje = "No hay artículos en esta categoría todavía.",
                    icono = Icons.Outlined.Inventory2
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 4.dp, bottom = 16.dp),
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
            deportes = deportes.filterNot { it == "Todos" },
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
        DialogoConfirmacion(
            titulo = "Eliminar artículo",
            mensaje = "¿Seguro que deseas eliminar \"${articulo.nombre}\" del inventario? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            textoCancelar = "Cancelar",
            onConfirmar = {
                viewModel.eliminarArticulo(articulo)
                articuloAEliminar = null
            },
            onCancelar = { articuloAEliminar = null }
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

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BurbujaTexto(texto = articulo.icono, tamano = 48.dp, tamanoTexto = 24)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = articulo.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = articulo.deporte,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }

                IconButton(onClick = onEditar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar ${articulo.nombre}",
                        tint = TextoSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar ${articulo.nombre}",
                        tint = Peligro,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (stockBajo) {
                    Insignia(
                        texto = "Stock bajo (mín. ${articulo.stockMinimo})",
                        tipo = TipoInsignia.ALERTA,
                        icono = Icons.Filled.Warning,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Text(
                        text = "Stock mínimo: ${articulo.stockMinimo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

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
        BotonIcono(
            icono = Icons.Filled.Remove,
            descripcion = "Quitar una unidad",
            onClick = onMenos,
            enabled = cantidad > 0
        )

        Text(
            text = cantidad.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 32.dp)
        )

        BotonIcono(
            icono = Icons.Filled.Add,
            descripcion = "Agregar una unidad",
            onClick = onMas
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        onCerrar = onCerrar,
        pie = {
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
    ) {
        EtiquetaCampo("NOMBRE DEL ARTÍCULO")
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = "Ej: Raquetas",
            capitalizacion = KeyboardCapitalization.Sentences,
            isError = nombreRepetido,
            mensajeError = if (nombreRepetido) "Ya existe un artículo con ese nombre en el inventario" else null
        )

        EspacioCampos()

        EtiquetaCampo("DEPORTE")
        ExposedDropdownMenuBox(expanded = mostrarDropdown, onExpandedChange = { mostrarDropdown = it }) {
            CampoTexto(
                value = deporteSeleccionado.takeIf { it.isNotBlank() }?.let { "${Deportes.emojiDe(it)} $it" } ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarDropdown) }
            )
            ExposedDropdownMenu(expanded = mostrarDropdown, onDismissRequest = { mostrarDropdown = false }) {
                deportes.forEach { deporte ->
                    DropdownMenuItem(
                        text = { Text("${Deportes.emojiDe(deporte)} $deporte") },
                        onClick = {
                            deporteSeleccionado = deporte
                            mostrarDropdown = false
                        }
                    )
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
                    tipoTeclado = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            }
        }
    }
}
