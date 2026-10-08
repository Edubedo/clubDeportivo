package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.MarcaSuave
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.clubdeportivo.ui.components.botonFlotanteVisible
import com.example.clubdeportivo.ui.components.BotonFlotanteAgregar
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
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
    var filtroDeporte by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Un encargado solo ve su área: puede ponerla en mantenimiento, pero no crear, editar ni borrar áreas.
    val esEncargado = SesionManager.usuarioActual?.rol?.esEncargado() == true

    val deportes = areas.map { it.tipo }.distinct()
    val areasVisibles = areas
        .filter { filtroDeporte == null || it.tipo == filtroDeporte }
        .sortedBy { it.disponibilidad == DisponibilidadArea.MANTENIMIENTO } // las que están en mantenimiento, al final

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FondoApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!esEncargado) {
                BotonFlotanteAgregar(
                    texto = "Agregar área",
                    visible = listState.botonFlotanteVisible(),
                    onClick = {
                        areaEnEdicion = null
                        mostrarFormulario = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (deportes.size > 1) {
                val opciones = listOf("Todos") + deportes
                PestanasPildora(
                    opciones = opciones,
                    seleccionada = opciones.indexOf(filtroDeporte ?: "Todos").coerceAtLeast(0),
                    onSeleccion = { filtroDeporte = if (it == 0) null else opciones[it] }
                )
            }
            PullToRefreshBox(
                isRefreshing = cargando && areas.isNotEmpty(),
                onRefresh = { viewModel.cargarAreas() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    cargando && areas.isEmpty() -> FullScreenLoading()
                    areas.isEmpty() -> EmptyState(
                        mensaje = if (esEncargado) "No hay áreas de tu deporte registradas, o todavía no tienes un área asignada."
                        else "No hay áreas registradas. Agrega la primera con \"Agregar área\".",
                        icono = Icons.Default.Place
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(areasVisibles, key = { it.id }) { area ->
                            AreaCard(
                                area = area,
                                reservasHoy = reservasHoy[area.id] ?: 0,
                                soloEstatus = esEncargado,
                                onEditar = {
                                    areaEnEdicion = area
                                    mostrarFormulario = true
                                },
                                onCambiarEstatus = { viewModel.cambiarDisponibilidad(area, it) },
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
            onGuardar = { nombre, tipo, capacidad, emoji, disponibilidad ->
                viewModel.guardarArea(areaEnEdicion, nombre, tipo, capacidad, emoji, disponibilidad)
                mostrarFormulario = false
            }
        )
    }

    areaAEliminar?.let { area ->
        DialogoConfirmacion(
            titulo = "Eliminar área",
            mensaje = "¿Seguro que quieres eliminar \"${Deportes.titulo(area.tipo, area.nombre)}\"? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            textoCancelar = "Cancelar",
            onConfirmar = {
                viewModel.eliminarArea(area)
                areaAEliminar = null
            },
            onCancelar = { areaAEliminar = null }
        )
    }
}

@Composable
private fun AreaCard(
    area: Area,
    reservasHoy: Int,
    soloEstatus: Boolean,
    onEditar: () -> Unit,
    onCambiarEstatus: (DisponibilidadArea) -> Unit,
    onEliminar: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val enMantenimiento = area.disponibilidad == DisponibilidadArea.MANTENIMIENTO

    TarjetaClub(modifier = Modifier.fillMaxWidth(), onClick = if (soloEstatus) null else onEditar) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BurbujaTexto(
                texto = Deportes.emojiDe(area.tipo, area.emoji),
                tamano = 48.dp,
                tamanoTexto = 24,
                modifier = Modifier.padding(top = 4.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Deportes.titulo(area.tipo, area.nombre),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$reservasHoy reservas hoy · cupo ${area.capacidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 2.dp)
                )
                when (area.disponibilidad) {
                    DisponibilidadArea.MANTENIMIENTO -> Insignia(
                        texto = "En mantenimiento",
                        tipo = TipoInsignia.ALERTA,
                        icono = Icons.Default.Warning,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    DisponibilidadArea.OCUPADA -> Insignia(
                        texto = "Ocupada",
                        tipo = TipoInsignia.NEUTRO,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    DisponibilidadArea.DISPONIBLE -> Insignia(
                        texto = "Disponible",
                        tipo = TipoInsignia.EXITO,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = TextoSecundario)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    if (!soloEstatus) DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuAbierto = false; onEditar() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (enMantenimiento) "Marcar disponible" else "Poner en mantenimiento") },
                        leadingIcon = {
                            Icon(
                                if (enMantenimiento) Icons.Default.CheckCircle else Icons.Default.Build,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            onCambiarEstatus(if (enMantenimiento) DisponibilidadArea.DISPONIBLE else DisponibilidadArea.MANTENIMIENTO)
                        }
                    )
                    if (!soloEstatus) DropdownMenuItem(
                        text = { Text("Eliminar", color = Peligro) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Peligro) },
                        onClick = { menuAbierto = false; onEliminar() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaFormDialog(
    areaExistente: Area?,
    deportesExistentes: List<Pair<String, String>>,
    nombreRepetido: (nombre: String, tipo: String) -> Boolean,
    onCerrar: () -> Unit,
    onGuardar: (nombre: String, tipo: String, capacidad: Int, emoji: String, disponibilidad: DisponibilidadArea) -> Unit
) {
    val esEdicion = areaExistente != null
    var modoNuevo by remember { mutableStateOf(false) }
    var deporteElegido by remember { mutableStateOf(areaExistente?.tipo ?: "") }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var deporteNuevo by remember { mutableStateOf("") }
    var emojiNuevo by remember { mutableStateOf(Deportes.emojisSugeridos.first()) }
    var nombre by remember { mutableStateOf(areaExistente?.nombre ?: "") }
    var capacidad by remember { mutableStateOf((areaExistente?.capacidad ?: 10).toString()) }
    var enMantenimiento by remember { mutableStateOf(areaExistente?.disponibilidad == DisponibilidadArea.MANTENIMIENTO) }

    val tipo = if (modoNuevo) deporteNuevo.trim() else deporteElegido
    val repetido = nombre.isNotBlank() && tipo.isNotBlank() && nombreRepetido(nombre, tipo)
    val puedeGuardar = tipo.isNotBlank() && nombre.isNotBlank() && !repetido &&
            (!modoNuevo || emojiNuevo.isNotBlank()) && (capacidad.toIntOrNull() ?: 0) > 0

    DialogoFormulario(
        titulo = if (esEdicion) "Editar área" else "Agregar área",
        onCerrar = onCerrar,
        pie = {
            BotonPrimario(
                texto = if (esEdicion) "Guardar cambios" else "Agregar área",
                enabled = puedeGuardar,
                onClick = {
                    val emoji = if (modoNuevo) emojiNuevo.trim() else areaExistente?.emoji.orEmpty()
                    onGuardar(
                        nombre.trim(),
                        tipo,
                        capacidad.toIntOrNull() ?: 1,
                        emoji,
                        if (enMantenimiento) DisponibilidadArea.MANTENIMIENTO else DisponibilidadArea.DISPONIBLE
                    )
                }
            )
        }
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
                    placeholder = "Elige un deporte",
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
            EtiquetaCampo("NOMBRE DEL DEPORTE")
            CampoTexto(value = deporteNuevo, onValueChange = { deporteNuevo = it }, placeholder = "Ej: Pádel")

            EspacioCampos()

            EtiquetaCampo("EMOJI")
            SelectorEmoji(seleccionado = emojiNuevo, onSeleccion = { emojiNuevo = it })
        }

        EspacioCampos()

        EtiquetaCampo("NOMBRE DE LA CANCHA")
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = "Ej: Cancha C",
            capitalizacion = KeyboardCapitalization.Sentences,
            isError = repetido,
            mensajeError = if (repetido) "Ya existe un área con ese nombre en este deporte" else null
        )

        EspacioCampos()

        EtiquetaCampo("CUPO MÁXIMO (PERSONAS)")
        CampoTexto(
            value = capacidad,
            onValueChange = { capacidad = it.filter(Char::isDigit).take(4) },
            tipoTeclado = KeyboardType.Number,
            imeAction = ImeAction.Done
        )

        EspacioCampos()

        EtiquetaCampo("ESTATUS")
        PestanasPildora(
            opciones = listOf("Disponible", "En mantenimiento"),
            seleccionada = if (enMantenimiento) 1 else 0,
            onSeleccion = { enMantenimiento = it == 1 },
            margenHorizontal = 0.dp
        )
        Text(
            text = if (enMantenimiento) "No se aceptan reservas nuevas en esta área. Las que ya existen se conservan."
            else "El área acepta reservas con normalidad.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }
}


/** Cuadrícula de emojis para elegir el del deporte nuevo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorEmoji(seleccionado: String, onSeleccion: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Deportes.emojisSugeridos.forEach { emoji ->
            val elegido = emoji == seleccionado
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (elegido) MarcaSuave else Color.Transparent)
                    .border(if (elegido) 2.dp else 1.dp, if (elegido) Marca else BordeCampo, RoundedCornerShape(12.dp))
                    .selectable(selected = elegido, role = Role.RadioButton, onClick = { onSeleccion(emoji) }),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
    }
}
