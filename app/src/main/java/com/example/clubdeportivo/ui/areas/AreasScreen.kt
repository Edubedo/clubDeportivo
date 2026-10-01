package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca

/** Colores (borde, burbuja) por deporte, para que cada tarjeta se distinga como en el mockup. */
private val paleta = listOf(
    Color(0xFFFDBA74) to Color(0xFFFFEDD5),
    Color(0xFFC4B5FD) to Color(0xFFEDE9FE),
    Color(0xFFF9A8D4) to Color(0xFFFCE7F3),
    Color(0xFF93C5FD) to Color(0xFFDBEAFE),
    Color(0xFF6EE7B7) to Color(0xFFD1FAE5),
    Color(0xFFFCD34D) to Color(0xFFFEF3C7)
)

private fun colorDeDeporte(tipo: String): Pair<Color, Color> = paleta[(tipo.hashCode() and Int.MAX_VALUE) % paleta.size]

/** Gestión de áreas: lista, alta, edición y baja (mockup "Gestión de Canchas"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(viewModel: AreasViewModel = viewModel()) {
    val areas by viewModel.areas.observeAsState(emptyList())
    val reservasHoy by viewModel.reservasHoy.observeAsState(emptyMap())
    val cargando by viewModel.cargando.observeAsState(false)
    val mensaje by viewModel.mensaje.observeAsState()

    var areaEnEdicion by remember { mutableStateOf<Area?>(null) }
    var mostrarFormulario by remember { mutableStateOf(false) }
    var areaAEliminar by remember { mutableStateOf<Area?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFFF8FAFD),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    areaEnEdicion = null
                    mostrarFormulario = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar cancha", fontWeight = FontWeight.Bold) },
                containerColor = VerdeMarca,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                text = "Gestión de canchas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            PullToRefreshBox(
                isRefreshing = cargando && areas.isNotEmpty(),
                onRefresh = { viewModel.cargarAreas() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    cargando && areas.isEmpty() -> FullScreenLoading()
                    areas.isEmpty() -> EmptyState("No hay áreas registradas. Agrega la primera con \"Agregar cancha\".")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(areas, key = { it.id }) { area ->
                            AreaCard(
                                area = area,
                                reservasHoy = reservasHoy[area.id] ?: 0,
                                onEditar = {
                                    areaEnEdicion = area
                                    mostrarFormulario = true
                                },
                                onEliminar = { areaAEliminar = area }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        AreaFormDialog(
            areaExistente = areaEnEdicion,
            deportesExistentes = (areas.map { it.tipo to Deportes.emojiDe(it.tipo, it.emoji) } +
                Deportes.predefinidos.map { it to Deportes.emojiDe(it) }).distinctBy { it.first.lowercase() },
            nombreRepetido = { nombre, tipo -> viewModel.nombreRepetido(nombre, tipo, areaEnEdicion?.id) },
            onCerrar = { mostrarFormulario = false },
            onGuardar = { nombre, tipo, capacidad, emoji ->
                viewModel.guardarArea(areaEnEdicion, nombre, tipo, capacidad, emoji)
                mostrarFormulario = false
            }
        )
    }

    areaAEliminar?.let { area ->
        AlertDialog(
            onDismissRequest = { areaAEliminar = null },
            title = { Text("Eliminar cancha") },
            text = { Text("¿Seguro que quieres eliminar \"${Deportes.titulo(area.tipo, area.nombre)}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarArea(area)
                    areaAEliminar = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { areaAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AreaCard(area: Area, reservasHoy: Int, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val (borde, burbuja) = colorDeDeporte(area.tipo)
    var menuAbierto by remember { mutableStateOf(false) }
    val enMantenimiento = area.disponibilidad == DisponibilidadArea.MANTENIMIENTO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, borde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(burbuja),
                contentAlignment = Alignment.Center
            ) {
                Text(text = Deportes.emojiDe(area.tipo, area.emoji), fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Deportes.titulo(area.tipo, area.nombre),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$reservasHoy reservas hoy · cupo ${area.capacidad}",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (enMantenimiento) {
                    Text(
                        text = "EN MANTENIMIENTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = Color(0xFF64748B))
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuAbierto = false; onEditar() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuAbierto = false; onEliminar() }
                    )
                }
            }
        }
    }
}

/** Formulario de alta/edición de cancha (mockup "+ Agregar Cancha": deporte existente o nuevo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaFormDialog(
    areaExistente: Area?,
    deportesExistentes: List<Pair<String, String>>,
    nombreRepetido: (nombre: String, tipo: String) -> Boolean,
    onCerrar: () -> Unit,
    onGuardar: (nombre: String, tipo: String, capacidad: Int, emoji: String) -> Unit
) {
    val esEdicion = areaExistente != null
    var modoNuevo by remember { mutableStateOf(false) }
    var deporteElegido by remember { mutableStateOf(areaExistente?.tipo ?: "") }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var deporteNuevo by remember { mutableStateOf("") }
    var emojiNuevo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf(areaExistente?.nombre ?: "") }
    var capacidad by remember { mutableStateOf((areaExistente?.capacidad ?: 10).toString()) }

    val tipo = if (modoNuevo) deporteNuevo.trim() else deporteElegido
    val repetido = nombre.isNotBlank() && tipo.isNotBlank() && nombreRepetido(nombre, tipo)
    val puedeGuardar = tipo.isNotBlank() && nombre.isNotBlank() && !repetido &&
        (!modoNuevo || emojiNuevo.isNotBlank()) && (capacidad.toIntOrNull() ?: 0) > 0

    DialogoFormulario(
        titulo = if (esEdicion) "Editar cancha" else "Agregar cancha",
        onCerrar = onCerrar
    ) {
        PestanasPildora(
            opciones = listOf("Deporte existente", "Nuevo deporte"),
            seleccionada = if (modoNuevo) 1 else 0,
            onSeleccion = { modoNuevo = it == 1 },
            margenHorizontal = 0.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!modoNuevo) {
            EtiquetaCampo("DEPORTE")
            ExposedDropdownMenuBox(expanded = dropdownAbierto, onExpandedChange = { dropdownAbierto = it }) {
                CampoTexto(
                    value = deportesExistentes.firstOrNull { it.first == deporteElegido }
                        ?.let { "${it.second} ${it.first}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = "—",
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownAbierto) }
                )
                DropdownMenu(expanded = dropdownAbierto, onDismissRequest = { dropdownAbierto = false }) {
                    deportesExistentes.forEach { (deporte, emoji) ->
                        DropdownMenuItem(
                            text = { Text("$emoji $deporte") },
                            onClick = {
                                deporteElegido = deporte
                                dropdownAbierto = false
                            }
                        )
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(2f)) {
                    EtiquetaCampo("NOMBRE DEL DEPORTE")
                    CampoTexto(value = deporteNuevo, onValueChange = { deporteNuevo = it }, placeholder = "Ej: Pádel")
                }
                Column(modifier = Modifier.weight(1f)) {
                    EtiquetaCampo("EMOJI")
                    CampoTexto(value = emojiNuevo, onValueChange = { emojiNuevo = it.take(4) }, placeholder = "🏆")
                }
            }
        }

        EspacioCampos()

        EtiquetaCampo("NOMBRE DE LA CANCHA")
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = "Ej: Cancha C",
            isError = repetido,
            mensajeError = if (repetido) "Ya existe un área con ese nombre en este deporte" else null
        )

        EspacioCampos()

        EtiquetaCampo("CUPO MÁXIMO (PERSONAS)")
        CampoTexto(
            value = capacidad,
            onValueChange = { capacidad = it.filter(Char::isDigit).take(4) },
            tipoTeclado = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrimario(
            texto = if (esEdicion) "Guardar cambios" else "Agregar cancha",
            enabled = puedeGuardar,
            onClick = {
                val emoji = if (modoNuevo) emojiNuevo.trim() else areaExistente?.emoji.orEmpty()
                onGuardar(nombre.trim(), tipo, capacidad.toIntOrNull() ?: 1, emoji)
            }
        )
    }
}
