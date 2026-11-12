package com.example.clubdeportivo.ui.membresia

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.data.model.esPersonal
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.ui.theme.DeepNavy
import com.example.clubdeportivo.ui.theme.MembershipPro
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Textos

@Composable
fun ValidarRenovacionesScreen(viewModel: RenovacionViewModel = viewModel()) {
    val solicitudes by viewModel.solicitudesPendientes.collectAsState()
    val accionEnCurso by viewModel.accionEnCurso.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var comprobanteAmpliado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.observarPendientes() }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarError()
        }
    }

    if (SesionManager.usuarioActual?.rol?.esPersonal() != true) return

    if (solicitudes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                Textos.get("no_pending_renewals"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(solicitudes, key = { it.id }) { solicitud ->
                SolicitudRenovacionCard(
                    solicitud = solicitud,
                    enCurso = accionEnCurso == solicitud.id,
                    onVerComprobante = { comprobanteAmpliado = solicitud.comprobanteUrl },
                    onAprobar = { viewModel.aprobar(solicitud) },
                    onRechazar = { viewModel.rechazar(solicitud) }
                )
            }
        }
    }

    comprobanteAmpliado?.let { url ->
        Dialog(
            onDismissRequest = { comprobanteAmpliado = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url,
                        contentDescription = Textos.get("proof_of_payment"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                    IconButton(
                        onClick = { comprobanteAmpliado = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = Textos.get("close_proof"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SolicitudRenovacionCard(
    solicitud: SolicitudRenovacion,
    enCurso: Boolean,
    onVerComprobante: () -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                solicitud.socioNombre,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold
            )
            Text(
                Textos.get("plan_label", etiquetaPlan(solicitud.planId)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            Text(
                Textos.get("amount_label", dinero(solicitud.monto)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            Text(
                Textos.get("expected_concept", solicitud.concepto),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E3A5F),
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    Textos.get("pending"),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (solicitud.comprobanteUrl.isNotBlank()) {
                Box {
                    AsyncImage(
                        model = solicitud.comprobanteUrl,
                        contentDescription = Textos.get("open_proof"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clickable(onClick = onVerComprobante)
                    )
                    Icon(
                        Icons.Filled.OpenInFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    )
                }
            } else {
                Text(Textos.get("no_proof_attached"), color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAprobar,
                    enabled = !enCurso,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF17365D),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (enCurso) Textos.get("processing") else Textos.get("approve"))
                }
                OutlinedButton(
                    onClick = onRechazar,
                    enabled = !enCurso,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF991B1B))
                ) {
                    Text(Textos.get("reject"), color = Color(0xFF991B1B))
                }
            }
        }
    }
}

private fun etiquetaPlan(planId: String): String = when {
    planId.startsWith("INDIVIDUAL_") ->
        PlanIndividual.entries
            .firstOrNull { it.name == planId.removePrefix("INDIVIDUAL_") }
            ?.let(Catalogos::nombrePlanIndividual)
            ?: planId
    planId.startsWith("FAMILIAR_") ->
        planId.removePrefix("FAMILIAR_")
            .toIntOrNull()
            ?.let(Catalogos::paqueteFamiliarPorId)
            ?.nombre
            ?: planId
    else -> planId
}
