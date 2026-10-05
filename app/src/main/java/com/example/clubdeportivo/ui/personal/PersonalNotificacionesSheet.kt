package com.example.clubdeportivo.ui.personal

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.notificaciones.Notificacion
import java.text.SimpleDateFormat
import java.util.*

private val TextoOscuro = Color(0xFF111827)
private val TextoGris = Color(0xFF64748B)
private val Verde = Color(0xFF16A34A)

@Composable
fun PersonalNotificacionesSheet(
    onCerrar: () -> Unit,
    viewModel: PersonalNotificacionesViewModel = viewModel()
) {
    val notificaciones by viewModel.notificaciones.collectAsState()
    val leidas by viewModel.leidas.collectAsState()

    var pestanaSeleccionada by remember { mutableIntStateOf(0) } // 0: Recibidos, 1: Mis avisos
    var modoRedactar by remember { mutableStateOf(false) }

    var notificacionEditandoId by remember { mutableStateOf<String?>(null) }
    var tituloEdit by remember { mutableStateOf("") }
    var mensajeEdit by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            shape = RoundedCornerShape(28.dp),
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
                    // 1. MODO EDITAR AVISO
                    notificacionEditandoId != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Editar aviso",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = { notificacionEditandoId = null }) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Verde,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Verde,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Aviso actualizado correctamente ✅", Toast.LENGTH_SHORT).show()
                                notificacionEditandoId = null
                            },
                            enabled = tituloEdit.isNotBlank() && mensajeEdit.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Verde)
                        ) {
                            Text("Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }

                    // 2. MODO REDACTAR NUEVO AVISO
                    modoRedactar -> {
                        var titulo by remember { mutableStateOf("") }
                        var mensaje by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Redactar nuevo aviso",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = { modoRedactar = false }) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TIPO DE DESTINATARIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("Socios") },
                                shape = RoundedCornerShape(50.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Verde,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TÍTULO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Cancha en mantenimiento", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Verde,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("MENSAJE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mensaje,
                            onValueChange = { mensaje = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Escribe los detalles del aviso...", color = Color(0xFF94A3B8)) },
                            minLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Verde,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { modoRedactar = false },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("Cancelar", color = TextoGris, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    viewModel.enviarAvisoASocios(titulo, mensaje) {
                                        Toast.makeText(context, "Aviso enviado ✅", Toast.LENGTH_SHORT).show()
                                        modoRedactar = false
                                    }
                                },
                                enabled = titulo.isNotBlank() && mensaje.isNotBlank(),
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Verde)
                            ) {
                                Text("Enviar aviso", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // 3. VISTA PRINCIPAL (LISTA + PESTAÑAS)
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avisos y Notificaciones",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )
                            IconButton(onClick = onCerrar) {
                                Text("✕", fontSize = 18.sp, color = TextoGris, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        SecondaryTabRow(
                            selectedTabIndex = pestanaSeleccionada,
                            modifier = Modifier,
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = TabRowDefaults.primaryContentColor,
                            indicator = {},
                            divider = {},
                            tabs = {
                                Tab(
                                    selected = pestanaSeleccionada == 0,
                                    onClick = { pestanaSeleccionada = 0 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Recibidos", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = TextoGris
                                )
                                Tab(
                                    selected = pestanaSeleccionada == 1,
                                    onClick = { pestanaSeleccionada = 1 },
                                    modifier = Modifier.clip(RoundedCornerShape(50.dp)),
                                    text = { Text("Mis enviados", fontWeight = FontWeight.SemiBold) },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = TextoGris
                                )
                            })

                        Spacer(Modifier.height(20.dp))

                        if (notificaciones.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay avisos disponibles.",
                                    color = TextoGris,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(notificaciones) { notif ->
                                    val isLeida = leidas.contains(notif.id)

                                    TarjetaNotificacionGestionable(
                                        notif = notif,
                                        isLeida = isLeida,
                                        esModoEdicion = pestanaSeleccionada == 1,
                                        onEditar = {
                                            notificacionEditandoId = notif.id
                                            tituloEdit = notif.titulo
                                            mensajeEdit = notif.mensaje
                                        },
                                        onClick = { viewModel.marcarComoLeida(notif.id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { modoRedactar = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Verde)
                        ) {
                            Text("Redactar nuevo aviso", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaNotificacionGestionable(
    notif: Notificacion,
    isLeida: Boolean,
    esModoEdicion: Boolean,
    onEditar: () -> Unit,
    onClick: () -> Unit
) {
    val fechaFormat = remember(notif.fechaMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.fechaMillis))
    }

    var expandida by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLeida) Color(0xFFF8FAFD) else Color.White,
        border = BorderStroke(1.dp, if (isLeida) Color(0xFFE2E8F0) else Verde.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                expandida = !expandida
                if (!isLeida) onClick()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isLeida && !esModoEdicion) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Verde, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = notif.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextoOscuro,
                        maxLines = if (expandida) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = fechaFormat, fontSize = 11.sp, color = TextoGris)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notif.mensaje,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                maxLines = if (expandida) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (esModoEdicion) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onEditar,
                        colors = ButtonDefaults.textButtonColors(contentColor = Verde)
                    ) {
                        Text("✏️ Modificar aviso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}