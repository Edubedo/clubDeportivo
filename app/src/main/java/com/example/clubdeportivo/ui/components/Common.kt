package com.example.clubdeportivo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.clubdeportivo.ui.theme.Alerta
import com.example.clubdeportivo.ui.theme.AlertaSuave
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.ExitoSuave
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PeligroSuave
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.TextoTenue

private val RadioCampo = 12.dp
private val AlturaBoton = 52.dp
private val RadioBoton = 12.dp
private val RadioTarjeta = 16.dp

/** Margen horizontal estándar del contenido de todas las pantallas. */
val MargenPantalla = 16.dp

/** Encabezado estándar de una pantalla: título grande y, opcionalmente, una línea de contexto debajo. */
@Composable
fun EncabezadoPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    Column(modifier = modifier.padding(top = 16.dp, bottom = 12.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = TextoPrincipal
        )
        if (!subtitulo.isNullOrBlank()) {
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Título de una sección dentro de una pantalla ("Reservas recientes", "Membresía individual"...). */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleLarge,
        color = TextoPrincipal,
        modifier = modifier
    )
}

/** Etiqueta que va arriba de cada campo de formulario. */
@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoSecundario,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** Campo de texto estándar de la app. Fondo blanco para que se vea igual sobre gris o sobre blanco. */
@Composable
fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    mensajeError: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.None,
    /** Acción del botón del teclado. Por omisión "Siguiente" (pasa al campo de abajo); usa Done en el último. */
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    /** Se ejecuta al pulsar la acción final del teclado (Listo, Buscar, Ir). */
    onAccion: (() -> Unit)? = null
) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextoTenue) }
        } else null,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        isError = isError,
        supportingText = mensajeError?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
            capitalization = capitalizacion,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { foco.moveFocus(FocusDirection.Down) },
            onDone = {
                foco.clearFocus()
                onAccion?.invoke()
            },
            onSearch = {
                foco.clearFocus()
                onAccion?.invoke()
            },
            onGo = {
                foco.clearFocus()
                onAccion?.invoke()
            }
        ),
        shape = RoundedCornerShape(RadioCampo),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextoPrincipal,
            unfocusedTextColor = TextoPrincipal,
            disabledTextColor = TextoSecundario,
            focusedContainerColor = Superficie,
            unfocusedContainerColor = Superficie,
            disabledContainerColor = Superficie,
            errorContainerColor = Superficie,
            focusedBorderColor = Marca,
            unfocusedBorderColor = BordeCampo,
            cursorColor = Marca,
            focusedLeadingIconColor = Marca,
            unfocusedLeadingIconColor = TextoTenue,
            focusedTrailingIconColor = Marca,
            unfocusedTrailingIconColor = TextoTenue
        )
    )
}

/** Campo de búsqueda: lupa a la izquierda y una X para limpiar cuando hay texto. */
@Composable
fun CampoBusqueda(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    CampoTexto(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        imeAction = ImeAction.Search,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        } else null
    )
}

/**
 * true mientras la lista está arriba o se desplaza hacia arriba; false al desplazarse hacia abajo.
 * Sirve para esconder el botón flotante mientras se lee la lista y que no tape el último elemento.
 */
@Composable
fun LazyListState.botonFlotanteVisible(): Boolean {
    var indiceAnterior by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var desplazamientoAnterior by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    val visible by remember(this) {
        derivedStateOf {
            val sube = if (indiceAnterior != firstVisibleItemIndex) {
                firstVisibleItemIndex < indiceAnterior
            } else {
                firstVisibleItemScrollOffset <= desplazamientoAnterior
            }
            indiceAnterior = firstVisibleItemIndex
            desplazamientoAnterior = firstVisibleItemScrollOffset
            sube
        }
    }
    return visible
}

/** Botón flotante "Agregar ...": esquina inferior derecha, aparece y desaparece con una animación corta. */
@Composable
fun BotonFlotanteAgregar(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(texto, fontWeight = FontWeight.Bold) },
            containerColor = Marca,
            contentColor = SobreMarca,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/** Flecha de los campos que abren una lista de opciones. */
@Composable
fun IconoDesplegable() {
    Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = null, tint = TextoTenue)
}

/** Botón principal de acción. Solo uno por pantalla o formulario. */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        modifier = modifier
            .fillMaxWidth()
            .height(AlturaBoton),
        shape = RoundedCornerShape(RadioBoton),
        colors = ButtonDefaults.buttonColors(
            containerColor = Marca,
            contentColor = SobreMarca,
            disabledContainerColor = Borde,
            disabledContentColor = TextoTenue
        )
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SobreMarca, strokeWidth = 2.dp)
        } else {
            Text(text = texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp), maxLines = 1)
        }
    }
}

/** Botón secundario (Cancelar, Cerrar sesión...). */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colorTexto: Color = Marca
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AlturaBoton),
        shape = RoundedCornerShape(RadioBoton),
        border = BorderStroke(1.dp, BordeCampo),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colorTexto,
            disabledContentColor = TextoTenue
        )
    ) {
        Text(text = texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp), maxLines = 1)
    }
}

/** Botón compacto de acción con fondo suave (aprobar/rechazar, asistió/no vino...). El color solo comunica el tipo de acción. */
@Composable
fun BotonTonal(
    texto: String,
    icono: ImageVector,
    fondo: Color,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = fondo, contentColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = texto, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}

/**
 * Tarjeta estándar: blanca, esquinas de 16 y borde fino. Con [onClick] toda la tarjeta es pulsable;
 * con [destacada] el borde pasa a azul de marca (para resaltar una opción, como el plan recomendado).
 */
@Composable
fun TarjetaClub(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fondo: Color = Superficie,
    destacada: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val colores = CardDefaults.cardColors(containerColor = fondo)
    val borde = if (destacada) BorderStroke(2.dp, Marca) else BorderStroke(1.dp, Borde)
    val elevacion = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(RadioTarjeta),
            colors = colores,
            border = borde,
            elevation = elevacion,
            content = contenido
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(RadioTarjeta),
            colors = colores,
            border = borde,
            elevation = elevacion,
            content = contenido
        )
    }
}

/** Tipos de insignia. El color solo comunica el estado, nunca decora. */
enum class TipoInsignia(val fondo: Color, val texto: Color) {
    EXITO(ExitoSuave, Exito),
    ALERTA(AlertaSuave, Alerta),
    PELIGRO(PeligroSuave, Peligro),
    MARCA(MarcaSuave, Marca),
    NEUTRO(FondoApp, TextoSecundario)
}

/** Píldora pequeña de estado ("Confirmada", "Activo", "Vencida"...). */
@Composable
fun Insignia(
    texto: String,
    tipo: TipoInsignia,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = tipo.fondo) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icono != null) {
                Icon(imageVector = icono, contentDescription = null, tint = tipo.texto, modifier = Modifier.size(14.dp))
            }
            Text(
                text = texto,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tipo.texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Círculo suave con un icono dentro: el "avatar" de tarjetas de datos (áreas, indicadores, artículos). */
@Composable
fun IconoCircular(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    descripcion: String? = null
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MarcaSuave),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icono, contentDescription = descripcion, tint = Marca, modifier = Modifier.size(tamano * 0.5f))
    }
}

/** Círculo suave con un texto dentro (emoji de un deporte o inicial de una persona). */
@Composable
fun BurbujaTexto(
    texto: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    tamanoTexto: Int = 22
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MarcaSuave),
        contentAlignment = Alignment.Center
    ) {
        Text(text = texto, fontSize = tamanoTexto.sp, color = Marca, fontWeight = FontWeight.Bold)
    }
}

/** Botón cuadrado con icono para sumar o restar (cantidades, personas). */
@Composable
fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = Superficie,
        border = BorderStroke(1.dp, BordeCampo)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = if (enabled) Marca else BordeCampo,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Diálogo de formulario estándar. */
@Composable
fun DialogoFormulario(
    titulo: String,
    onCerrar: () -> Unit,
    /** Zona fija bajo el formulario (botón de guardar y mensajes de error): siempre visible, sin tener que bajar. */
    pie: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            color = Superficie
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    IconButton(onClick = onCerrar) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = TextoSecundario
                        )
                    }
                }
                HorizontalDivider(color = Borde)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (pie == null) 20.dp else 12.dp)
                ) {
                    content()
                }
                if (pie != null) {
                    HorizontalDivider(color = Borde)
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)) {
                        pie()
                    }
                }
            }
        }
    }
}

/** Confirmación antes de una acción que no se puede deshacer (eliminar, cancelar una reserva...). */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoCancelar: String = "Volver"
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(20.dp),
        containerColor = Superficie,
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge, color = TextoPrincipal) },
        text = { Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(textoConfirmar, color = Peligro, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(textoCancelar, color = TextoSecundario, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/** Separación vertical estándar entre campos de un formulario. */
@Composable
fun EspacioCampos() {
    Spacer(modifier = Modifier.height(20.dp))
}

/** Indicador de carga centrado. */
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Marca)
    }
}

/** Mensaje centrado para listas vacías, con un icono para que no se vea como una pantalla rota. */
@Composable
fun EmptyState(
    mensaje: String,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Outlined.Inbox
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconoCircular(icono = icono, tamano = 64.dp)
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Avatar circular con las iniciales del nombre. */
@Composable
fun InitialsAvatar(nombre: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val iniciales = nombre.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MarcaSuave),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = Marca,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

/**
 * Selector de una opción en píldoras. Con 3 opciones o menos se reparten el ancho; con más, se desplazan
 * horizontalmente para que ningún texto se corte.
 */
@Composable
fun PestanasPildora(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
    margenHorizontal: Dp = MargenPantalla
) {
    val repartir = opciones.size <= 3
    val fila: @Composable RowScope.() -> Unit = {
        opciones.forEachIndexed { indice, texto ->
            val activa = indice == seleccionada
            Surface(
                onClick = { onSeleccion(indice) },
                modifier = if (repartir) Modifier.weight(1f).height(44.dp) else Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (activa) Marca else Superficie,
                border = if (activa) null else BorderStroke(1.dp, BordeCampo)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = texto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activa) SobreMarca else TextoSecundario,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (repartir) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = margenHorizontal, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = fila
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = margenHorizontal, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = fila
        )
    }
}
