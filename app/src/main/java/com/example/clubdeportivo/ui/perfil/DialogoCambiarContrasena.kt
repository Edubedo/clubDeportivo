package com.example.clubdeportivo.ui.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.RequisitosContrasena
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.tr

/** Cada persona cambia su propia contraseña: escribe la actual (para confirmar que es ella) y la nueva dos veces. */
@Composable
fun DialogoCambiarContrasena(
    cambiando: Boolean,
    error: String?,
    onCambiar: (actual: String, nueva: String, confirmacion: String) -> Unit,
    onCerrar: () -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val ojo: @Composable () -> Unit = {
        IconButton(onClick = { visible = !visible }) {
            Icon(
                imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (visible) tr("ocultar_contrasena") else tr("mostrar_contrasena")
            )
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Superficie,
        title = { Text(tr("password_change_title"), style = MaterialTheme.typography.titleLarge, color = TextoPrincipal) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                EtiquetaCampo(tr("password_current"))
                CampoTexto(
                    value = actual,
                    onValueChange = { actual = it },
                    esContrasena = !visible,
                    trailingIcon = ojo
                )
                Spacer(Modifier.height(14.dp))
                EtiquetaCampo(tr("password_new"))
                CampoTexto(value = nueva, onValueChange = { nueva = it }, esContrasena = !visible)
                Spacer(Modifier.height(8.dp))
                RequisitosContrasena(contrasena = nueva)
                Spacer(Modifier.height(14.dp))
                EtiquetaCampo(tr("password_confirm"))
                CampoTexto(value = confirmacion, onValueChange = { confirmacion = it }, esContrasena = !visible)
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Peligro,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                Text(
                    text = tr("password_change_note"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !cambiando, onClick = { onCambiar(actual, nueva, confirmacion) }) {
                Text(
                    text = if (cambiando) tr("password_changing") else tr("password_change_button"),
                    color = Marca,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !cambiando, onClick = onCerrar) { Text(tr("cancelar"), color = TextoSecundario) }
        }
    )
}
