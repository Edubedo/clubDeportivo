package com.example.clubdeportivo.ui.membresia

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.ui.theme.AttentionAmber
import com.example.clubdeportivo.ui.theme.DeepNavy
import com.example.clubdeportivo.ui.theme.MembershipPro
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.Textos
import java.util.Calendar
import java.util.Locale

private const val BANCO_TRANSFERENCIA = "BBVA México"
private const val BENEFICIARIO_TRANSFERENCIA = "Athletic Club S.A. de C.V."
private const val CLABE_TRANSFERENCIA = "4152314495752563"

@Composable
fun MembresiaSocioSection(
    membresia: Membresia?,
    modifier: Modifier = Modifier,
    viewModel: RenovacionViewModel = viewModel()
) {
    val usuario = SesionManager.usuarioActual ?: return
    val pendiente by viewModel.solicitudPendiente.collectAsState()
    val guardando by viewModel.guardando.collectAsState()
    val error by viewModel.error.collectAsState()
    val enviado by viewModel.enviado.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var mostrarSheet by remember { mutableStateOf(false) }
    var comprobante by remember(membresia?.id) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(usuario.id) { viewModel.observarSolicitudSocio(usuario.id) }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarError()
        }
    }
    LaunchedEffect(enviado) {
        if (enviado) {
            mostrarSheet = false
            Toast.makeText(context, Textos.get("renewal_request_sent"), Toast.LENGTH_SHORT).show()
            viewModel.limpiarEnviado()
        }
    }

    val selectorComprobante = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> comprobante = uri }

    val estado = membresia?.let { ReglasMembresia.estadoEfectivo(it, Fechas.hoy()) }
    val puedeRenovar = membresia != null && when (estado) {
        EstadoMembresia.VENCIDA -> true
        EstadoMembresia.ACTIVA ->
            membresia.fechaVencimiento >= Fechas.hoy() &&
                membresia.fechaVencimiento <= Fechas.sumarDias(7)
        else -> false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (pendiente != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = AttentionAmber)
                    Text(
                        Textos.get("renewal_review_pending"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (puedeRenovar) {
            Button(
                onClick = {
                    comprobante = null
                    mostrarSheet = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MembershipPro,
                    contentColor = DeepNavy
                )
            ) {
                Text(Textos.get("renew_membership"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (mostrarSheet && membresia != null) {
        val concepto = remember(usuario.id) {
            val meses = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
            val mes = meses[Calendar.getInstance().get(Calendar.MONTH)]
            "REN-${usuario.id.takeLast(4).uppercase(Locale.ROOT)}-$mes"
        }
        val planIdActual = membresia.planIdRenovacion()
        Dialog(
            onDismissRequest = { if (!guardando) mostrarSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Textos.get("renewal_dialog_title"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (!guardando) mostrarSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = Textos.get("close"), tint = Color(0xFF475569))
                    }
                }

                EtiquetaRenovacion(Textos.get("renewal_section_plan").uppercase())
                Text(
                    ReglasMembresia.etiquetaPlan(membresia),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF475569)
                )
                Text(
                    Textos.get(
                        "transfer_amount_value",
                        String.format(Locale.US, "%,.2f", membresia.precio)
                    ) + " / " + Textos.get("per_month"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF0F294A),
                    fontWeight = FontWeight.Bold
                )

                EtiquetaRenovacion(Textos.get("renewal_section_transfer").uppercase())
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(Textos.get("bank_label", BANCO_TRANSFERENCIA), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                        Text(Textos.get("beneficiary_label", BENEFICIARIO_TRANSFERENCIA), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                        CajaCopiableRenovacion(
                            etiqueta = "CLABE",
                            valor = CLABE_TRANSFERENCIA,
                            onCopiar = {
                                clipboard.setText(AnnotatedString(CLABE_TRANSFERENCIA))
                                Toast.makeText(context, Textos.get("copied_clabe"), Toast.LENGTH_SHORT).show()
                            }
                        )
                        CajaCopiableRenovacion(
                            etiqueta = Textos.get("unique_concept"),
                            valor = concepto,
                            onCopiar = {
                                clipboard.setText(AnnotatedString(concepto))
                                Toast.makeText(context, Textos.get("copied_concept"), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                EtiquetaRenovacion(Textos.get("renewal_section_proof").uppercase())
                if (comprobante != null) {
                    AsyncImage(
                        model = comprobante,
                        contentDescription = Textos.get("proof_preview"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
                OutlinedButton(
                    onClick = {
                        selectorComprobante.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E3A5F))
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Text(
                        if (comprobante == null) Textos.get("choose_proof") else Textos.get("change_proof"),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val planId = planIdActual ?: return@Button
                        val uri = comprobante ?: return@Button
                        viewModel.enviar(
                            SolicitudRenovacion(
                                id = "",
                                socioId = usuario.id,
                                socioNombre = usuario.nombre,
                                membresiaId = membresia.id,
                                planId = planId,
                                monto = membresia.precio,
                                concepto = concepto,
                                comprobanteUrl = "",
                                status = SolicitudRenovacion.PENDIENTE,
                                createdAt = System.currentTimeMillis()
                            ),
                            uri,
                            context.contentResolver
                        )
                    },
                    enabled = !guardando && pendiente == null && planIdActual != null && comprobante != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF17365D),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (guardando) Textos.get("sending_request") else Textos.get("send_request"), fontWeight = FontWeight.Bold)
                }
            }
        }
   }
}

@Composable
private fun EtiquetaRenovacion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF475569)
    )
}

@Composable
private fun CajaCopiableRenovacion(
    etiqueta: String,
    valor: String,
    onCopiar: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                Text(valor, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = onCopiar,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E3A5F))
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(Textos.get("copy"), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun Membresia.planIdRenovacion(): String? = when (tipo) {
   com.example.clubdeportivo.data.model.TipoMembresia.INDIVIDUAL ->
       plan?.let { "INDIVIDUAL_${it.name}" }
   com.example.clubdeportivo.data.model.TipoMembresia.FAMILIAR ->
       paqueteFamiliarId?.let { "FAMILIAR_$it" }
   com.example.clubdeportivo.data.model.TipoMembresia.VISITA -> null
}
