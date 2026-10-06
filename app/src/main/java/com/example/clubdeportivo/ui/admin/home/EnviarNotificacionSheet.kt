package com.example.clubdeportivo.ui.admin.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion

private val TextoOscuro = Color(0xFF192338)
private val TextoGris = Color(0xFF31487A)
private val AzulPrimario = Color(0xFF1E2E4F)

@Composable
fun EnviarNotificacionSheet(
    onDismiss: () -> Unit,
    viewModel: AdminNotificacionesViewModel = viewModel()
) {
    val estado by viewModel.ui.collectAsState()
    val context = LocalContext.current

    var pestanaSeleccionada by remember { mutableIntStateOf(0) }
    var notificacionEditandoId by remember { mutableStateOf<String?>(null) }
    var tituloEdit by remember { mutableStateOf("") }
    var mensajeEdit by remember { mutableStateOf("") }

    LaunchedEffect(estado.enviada) {
        if (estado.enviada) {
            Toast.makeText(context, "Notificación enviada correctamente", Toast.LENGTH_SHORT).show()
            viewModel.consumirEnviada()
            pestanaSeleccionada = 0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                when {
                    // 1. MODO EDITAR
                    notificacionEditandoId != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Editar notificación",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = { notificacionEditandoId = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextoGris
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TÍTULO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = tituloEdit,
                            onValueChange = { tituloEdit = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulPrimario,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("MENSAJE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mensajeEdit,
                            onValueChange = { mensajeEdit = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulPrimario,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Aviso actualizado correctamente", Toast.LENGTH_SHORT).show()
                                notificacionEditandoId = null
                            },
                            enabled = tituloEdit.isNotBlank() && mensajeEdit.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }
                        }
                    }

                    // 2. VISTA PRINCIPAL (CUADRO FLOTANTE)
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Panel de Notificaciones",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextoGris
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        SecondaryTabRow(
                            selectedTabIndex = pestanaSeleccionada,
                            modifier = Modifier,
                            containerColor = Color(0xFFEAF1F8),
                            contentColor = TabRowDefaults.primaryContentColor,
                            indicator = {},
                            divider = {},
                            tabs = {
                                Tab(
                                    selected = pestanaSeleccionada == 0,
                                    onClick = { pestanaSeleccionada = 0 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Nueva", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.Black,
                                    unselectedContentColor = TextoGris
                                )
                                Tab(
                                    selected = pestanaSeleccionada == 1,
                                    onClick = { pestanaSeleccionada = 1 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Historial", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.Black,
                                    unselectedContentColor = TextoGris
                                )
                            }
                        )

                        Spacer(Modifier.height(20.dp))

                        if (pestanaSeleccionada == 1) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay notificaciones registradas.",
                                    color = TextoGris,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("TIPO DE DESTINATARIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DestinatarioNotificacion.entries.forEach { opcion ->
                                        FilterChip(
                                            selected = estado.destinatario == opcion,
                                            onClick = { viewModel.onDestinatario(opcion) },
                                            label = { Text(opcion.etiqueta) },
                                            shape = RoundedCornerShape(50.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulPrimario,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Text("TÍTULO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = estado.titulo,
                                    onValueChange = viewModel::onTitulo,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Ej: Mantenimiento programado", color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AzulPrimario,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    isError = estado.errorTitulo != null
                                )

                                Spacer(Modifier.height(16.dp))

                                Text("MENSAJE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = estado.mensaje,
                                    onValueChange = viewModel::onMensaje,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Escribe el contenido...", color = Color(0xFF94A3B8)) },
                                    minLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AzulPrimario,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    isError = estado.errorMensaje != null
                                )

                                estado.errorGeneral?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(it, color = Color(0xFFDC2626), fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.enviar(autorId = SesionManager.usuarioActual?.id?.toString())
                                },
                                enabled = !estado.enviando,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario)
                            ) {
                                if (estado.enviando) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Enviar notificación", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}