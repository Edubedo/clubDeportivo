package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.ReglasReserva
import com.example.clubdeportivo.util.Textos

/**
 * Pide el nuevo día, hora de inicio, duración y personas de una reserva ya confirmada. No la cambia: manda una
 * solicitud que el administrador o el encargado del área valida (la disponibilidad se revisa al enviar y al aprobar).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DialogoModificarReserva(
    reserva: ReservaListada,
    fechas: List<Pair<String, String>>,
    onEnviar: (fecha: String, desde: Int, horas: Int, personas: Int) -> Unit,
    onCerrar: () -> Unit
) {
    val inicioActual = (ReglasReserva.aMinutos(reserva.horaInicio) ?: 0) / 60
    val duracionActual = (((ReglasReserva.aMinutos(reserva.horaFin) ?: 0) - (ReglasReserva.aMinutos(reserva.horaInicio) ?: 0)) / 60)
        .coerceIn(1, Catalogos.MAX_HORAS_POR_RESERVA)
    var fecha by remember { mutableStateOf(reserva.fecha) }
    var desde by remember { mutableIntStateOf(inicioActual) }
    var horas by remember { mutableIntStateOf(duracionActual) }
    var personas by remember { mutableIntStateOf(reserva.personas) }

    // La hora de fin no puede pasar de medianoche.
    val maxDesde = 24 - horas

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Superficie,
        shape = RoundedCornerShape(20.dp),
        title = { Text(Textos.get("request_change"), style = MaterialTheme.typography.titleLarge, color = TextoPrincipal) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    Textos.get("change_request_explain", reserva.titulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(14.dp))
                Etiqueta(Textos.get("lbl_fecha"))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // La fecha actual de la reserva siempre se ofrece, aunque ya no esté en la ventana de días reservables.
                    val opciones = if (fechas.any { it.first == reserva.fecha }) fechas else listOf(reserva.fecha to reserva.fecha) + fechas
                    opciones.forEach { (valor, etiqueta) ->
                        val activa = valor == fecha
                        Surface(
                            onClick = { fecha = valor },
                            shape = RoundedCornerShape(12.dp),
                            color = if (activa) Marca else Superficie,
                            border = if (activa) null else BorderStroke(1.dp, BordeCampo)
                        ) {
                            Text(
                                text = etiqueta,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activa) SobreMarca else TextoSecundario,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Contador(
                    etiqueta = Textos.get("change_request_start_hour"),
                    valor = String.format(java.util.Locale.ROOT, "%02d:00", desde),
                    onMenos = { if (desde > 0) desde -= 1 },
                    onMas = { if (desde < maxDesde) desde += 1 }
                )
                Contador(
                    etiqueta = Textos.get("change_request_duration"),
                    valor = "${horas}h → " + String.format(java.util.Locale.ROOT, "%02d:00", desde + horas),
                    onMenos = { if (horas > 1) horas -= 1 },
                    onMas = {
                        if (horas < Catalogos.MAX_HORAS_POR_RESERVA && desde + horas < 24) horas += 1
                    }
                )
                Contador(
                    etiqueta = Textos.get("change_request_people"),
                    valor = personas.toString(),
                    onMenos = { if (personas > 1) personas -= 1 },
                    onMas = { personas += 1 }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Textos.get("change_request_note"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onEnviar(fecha, desde, horas, personas) }) {
                Text(Textos.get("change_request_send"), color = Marca, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(Textos.get("cancel"), color = TextoSecundario) } }
    )
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoSecundario,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun Contador(etiqueta: String, valor: String, onMenos: () -> Unit, onMas: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Etiqueta(etiqueta)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onMenos, shape = RoundedCornerShape(12.dp)) { Text("−", color = Marca, fontSize = 18.sp) }
            Text(
                valor,
                style = MaterialTheme.typography.titleSmall,
                color = TextoPrincipal,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onMas, shape = RoundedCornerShape(12.dp)) { Text("+", color = Marca, fontSize = 18.sp) }
        }
    }
}
