package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Textos

/**
 * Pide el periodo en que [area] estará en mantenimiento. Solo ese periodo se bloquea: las reservas que ya existan en
 * esos días se cancelan (sin penalización) y se avisa a quienes las hicieron para que reagenden.
 *
 * [afectadas] es cuántas reservas vigentes caen en el periodo elegido (null mientras se calcula).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoMantenimiento(
    area: Area,
    afectadas: Int?,
    onPeriodoElegido: (desde: String, hasta: String) -> Unit,
    onConfirmar: (desde: String, hasta: String, motivo: String) -> Unit,
    onCerrar: () -> Unit
) {
    val hoy = Fechas.hoy()
    var desde by remember { mutableStateOf(hoy) }
    var hasta by remember { mutableStateOf(hoy) }
    var motivo by remember { mutableStateOf("") }
    // Qué fecha se está eligiendo en el calendario: "desde", "hasta" o ninguna.
    var eligiendo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(desde, hasta) {
        if (hasta >= desde) onPeriodoElegido(desde, hasta)
    }

    val periodoValido = hasta >= desde

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Superficie,
        title = {
            Text(
                Textos.get("maintenance_dialog_title", Deportes.titulo(area.tipo, area.nombre)),
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    Textos.get("maintenance_dialog_explain"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo(Textos.get("maintenance_from"))
                        OutlinedButton(onClick = { eligiendo = "desde" }, modifier = Modifier.fillMaxWidth()) {
                            Text(Fechas.legible(desde), color = Marca, maxLines = 1)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EtiquetaCampo(Textos.get("maintenance_until"))
                        OutlinedButton(onClick = { eligiendo = "hasta" }, modifier = Modifier.fillMaxWidth()) {
                            Text(Fechas.legible(hasta), color = Marca, maxLines = 1)
                        }
                    }
                }
                if (!periodoValido) {
                    Text(
                        Textos.get("maintenance_end_before_start"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Peligro,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                EtiquetaCampo(Textos.get("maintenance_reason_label"))
                CampoTexto(
                    value = motivo,
                    onValueChange = { motivo = it.take(120) },
                    placeholder = Textos.get("maintenance_reason_placeholder")
                )
                if (periodoValido) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = when {
                            afectadas == null -> Textos.get("maintenance_counting")
                            afectadas == 0 -> Textos.get("maintenance_no_bookings_affected")
                            else -> Textos.get("maintenance_bookings_affected", afectadas)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if ((afectadas ?: 0) > 0) Peligro else TextoSecundario
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = periodoValido && afectadas != null, onClick = { onConfirmar(desde, hasta, motivo.trim()) }) {
                Text(Textos.get("maintenance_confirm"), color = if (periodoValido) Marca else TextoSecundario)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(Textos.get("cancel"), color = TextoSecundario) } }
    )

    eligiendo?.let { cual ->
        val minimo = if (cual == "desde") hoy else desde
        val inicial = Fechas.fechaAMillisUtc(if (cual == "desde") desde else hasta)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = inicial,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = Fechas.millisUtcAFecha(utcTimeMillis) >= minimo
            }
        )
        DatePickerDialog(
            onDismissRequest = { eligiendo = null },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        val fecha = Fechas.millisUtcAFecha(millis)
                        if (cual == "desde") {
                            desde = fecha
                            if (hasta < fecha) hasta = fecha
                        } else {
                            hasta = fecha
                        }
                    }
                    eligiendo = null
                }) { Text(Textos.get("ok")) }
            },
            dismissButton = { TextButton(onClick = { eligiendo = null }) { Text(Textos.get("cancel")) } }
        ) {
            DatePicker(state = estado)
        }
    }
}
