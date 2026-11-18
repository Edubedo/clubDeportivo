package com.example.clubdeportivo.ui.membresia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.SolicitudCambioPlan
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esSupervisor
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.IconoCircular
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.ReglasCambioPlan
import com.example.clubdeportivo.util.Textos

/** Solicitudes de cambio de plan que esperan respuesta de un administrador o un supervisor. */
@Composable
fun ValidarCambiosPlanScreen(viewModel: CambioPlanViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    var aRechazar by remember { mutableStateOf<SolicitudCambioPlan?>(null) }
    var aAprobar by remember { mutableStateOf<SolicitudCambioPlan?>(null) }
    var motivo by remember { mutableStateOf("") }

    val rol = SesionManager.usuarioActual?.rol
    val puedeResolver = rol?.esAdministrador() == true || rol?.esSupervisor() == true

    LaunchedEffect(Unit) { viewModel.observarPendientes() }
    LaunchedEffect(ui.mensaje) {
        ui.mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.mensajeMostrado()
        }
    }

    if (ui.pendientes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconoCircular(icono = Icons.Outlined.SwapHoriz, tamano = 64.dp)
                Text(
                    text = Textos.get("cambio_plan_sin_pendientes"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(ui.pendientes, key = { it.id }) { solicitud ->
                TarjetaCambioPlan(
                    solicitud = solicitud,
                    enCurso = ui.accionEnCurso == solicitud.id,
                    puedeResolver = puedeResolver,
                    onAprobar = { aAprobar = solicitud },
                    onRechazar = { motivo = ""; aRechazar = solicitud }
                )
            }
        }
    }

    aAprobar?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { aAprobar = null },
            title = { Text(Textos.get("cambio_plan_titulo")) },
            text = {
                Text(
                    Textos.get(
                        "cambio_plan_aprobar_confirmar", solicitud.socioNombre,
                        Catalogos.nombrePlanIndividual(solicitud.planNuevo)
                    )
                )
            },
            confirmButton = { Button(onClick = { viewModel.aprobar(solicitud); aAprobar = null }) { Text(Textos.get("approve")) } },
            dismissButton = { OutlinedButton(onClick = { aAprobar = null }) { Text(Textos.get("cancel")) } }
        )
    }

    aRechazar?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { aRechazar = null },
            title = { Text(Textos.get("cambio_plan_rechazar_titulo")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Textos.get("cambio_plan_rechazar_pregunta", solicitud.socioNombre))
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text(Textos.get("renewal_rejection_reason")) },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.rechazar(solicitud, motivo.trim().ifBlank { Textos.get("renewal_rejection_default_reason") })
                    aRechazar = null
                }) { Text(Textos.get("reject")) }
            },
            dismissButton = { OutlinedButton(onClick = { aRechazar = null }) { Text(Textos.get("cancel")) } }
        )
    }
}

@Composable
private fun TarjetaCambioPlan(
    solicitud: SolicitudCambioPlan,
    enCurso: Boolean,
    puedeResolver: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    val mejora = ReglasCambioPlan.esMejora(solicitud.planActual, solicitud.planNuevo)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    solicitud.socioNombre,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
                Insignia(
                    texto = if (mejora) Textos.get("cambio_plan_mejora") else Textos.get("cambio_plan_reduccion"),
                    tipo = if (mejora) TipoInsignia.EXITO else TipoInsignia.ALERTA
                )
            }
            Text(
                "${Catalogos.nombrePlanIndividual(solicitud.planActual)} → ${Catalogos.nombrePlanIndividual(solicitud.planNuevo)}",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF1E3A5F),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                Textos.get("cambio_plan_tarjeta_precio", dinero(solicitud.precioActual), dinero(solicitud.precioNuevo)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            if (puedeResolver) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAprobar,
                        enabled = !enCurso,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17365D), contentColor = Color.White)
                    ) { Text(if (enCurso) Textos.get("processing") else Textos.get("approve")) }
                    OutlinedButton(
                        onClick = onRechazar,
                        enabled = !enCurso,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF991B1B))
                    ) { Text(Textos.get("reject"), color = Color(0xFF991B1B)) }
                }
            }
        }
    }
}
