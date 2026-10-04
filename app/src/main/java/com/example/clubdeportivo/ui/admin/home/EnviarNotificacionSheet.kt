package com.example.clubdeportivo.ui.admin.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion

private val Azul = Color(0xFF2F80FF)
private val TextoOscuro = Color(0xFF111827)
private val TextoGris = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnviarNotificacionSheet(
    onDismiss: () -> Unit,
    viewModel: AdminNotificacionesViewModel = viewModel()
) {
    val estado by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Cuando se envía con éxito: aviso + cerrar hoja
    LaunchedEffect(estado.enviada) {
        if (estado.enviada) {
            Toast.makeText(context, "Notificación enviada ✅", Toast.LENGTH_SHORT).show()
            viewModel.consumirEnviada()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📢 Enviar notificación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                TextButton(onClick = onDismiss) { Text("✕", color = TextoGris) }
            }

            Spacer(Modifier.height(12.dp))

            // Destinatarios
            Text("ENVIAR A", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinatarioNotificacion.values().forEach { opcion ->
                    FilterChip(
                        selected = estado.destinatario == opcion,
                        onClick = { viewModel.onDestinatario(opcion) },
                        label = { Text(opcion.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Azul,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Título
            OutlinedTextField(
                value = estado.titulo,
                onValueChange = viewModel::onTitulo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título") },
                placeholder = { Text("Ej: Mantenimiento programado") },
                singleLine = true,
                isError = estado.errorTitulo != null,
                supportingText = { estado.errorTitulo?.let { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Mensaje
            OutlinedTextField(
                value = estado.mensaje,
                onValueChange = viewModel::onMensaje,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mensaje") },
                minLines = 4,
                maxLines = 6,
                isError = estado.errorMensaje != null,
                supportingText = {
                    Text(estado.errorMensaje ?: "${estado.mensaje.length}/300")
                },
                shape = RoundedCornerShape(12.dp)
            )

            estado.errorGeneral?.let {
                Text(it, color = Color(0xFFDC2626), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.enviar(autorId = SesionManager.usuarioActual?.id?.toString())
                },
                enabled = !estado.enviando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azul)
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar 🚀", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}