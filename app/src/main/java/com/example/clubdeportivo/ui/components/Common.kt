package com.example.clubdeportivo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// 🌟 Cambiado de VerdeMarca a Azul unificado de la app
val VerdeMarca = Color(0xFF1E2E4F) // Space Cadet (Azul principal)
private val TextoCampo = Color(0xFF192338) // Oxford Blue
private val TextoSecundario = Color(0xFF31487A) // YinMn Blue
private val RadioCampo = 12.dp
private val AlturaBoton = 52.dp
private val RadioBoton = 14.dp

/** Etiqueta en mayúsculas que va arriba de cada campo de formulario. */
@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF31487A),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** Campo de texto estándar de la app con enfoque y bordes en azul intenso. */
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
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = Color(0xFF94A3B8)) }
        } else null,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = isError,
        supportingText = mensajeError?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado),
        shape = RoundedCornerShape(RadioCampo),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextoCampo,
            unfocusedTextColor = TextoCampo,
            disabledTextColor = TextoCampo,
            focusedBorderColor = Color(0xFF31487A),
            cursorColor = Color(0xFF1E2E4F)
        )
    )
}

/** Botón principal de acción en azul intenso. */
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
        colors = ButtonDefaults.buttonColors(containerColor = VerdeMarca)
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
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
    colorTexto: Color = TextoSecundario
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AlturaBoton),
        shape = RoundedCornerShape(RadioBoton)
    ) {
        Text(text = texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorTexto, maxLines = 1)
    }
}

/** Diálogo de formulario estándar. */
@Composable
fun DialogoFormulario(
    titulo: String,
    onCerrar: () -> Unit,
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
            color = Color.White
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoCampo,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    IconButton(onClick = onCerrar) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = TextoCampo
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                ) {
                    content()
                }
            }
        }
    }
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
        CircularProgressIndicator(color = Color(0xFF1E2E4F))
    }
}

/** Mensaje centrado para listas vacías. */
@Composable
fun EmptyState(mensaje: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = mensaje,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = TextoSecundario
        )
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
            .background(Color(0xFFD6E4FE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = Color(0xFF1E2E4F),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** Pestañas en píldora con el color azul intenso seleccionado. */
@Composable
fun PestanasPildora(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
    margenHorizontal: Dp = 16.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = margenHorizontal, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        opciones.forEachIndexed { indice, texto ->
            val activa = indice == seleccionada
            Surface(
                onClick = { onSeleccion(indice) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (activa) VerdeMarca else Color(0xFFF1F5F9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = texto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activa) Color.White else Color(0xFF31487A),
                        maxLines = 1
                    )
                }
            }
        }
    }
}