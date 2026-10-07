package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas

/** Reserva vigente con su estado y la opción de cancelarla (siempre pide confirmación antes). */
@Composable
internal fun TarjetaReservaCliente(reserva: ReservaListada, onCancelar: () -> Unit) {
    val pendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION
    var confirmando by remember { mutableStateOf(false) }

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BurbujaTexto(texto = reserva.emoji, tamano = 48.dp, tamanoTexto = 24)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reserva.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Fechas.legible(reserva.fecha)} · ${reserva.horaInicio}–${reserva.horaFin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                    Text(
                        text = if (reserva.personas == 1) "1 persona" else "${reserva.personas} personas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                Insignia(
                    texto = if (pendiente) "Pendiente" else "Confirmada",
                    tipo = if (pendiente) TipoInsignia.ALERTA else TipoInsignia.EXITO
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { confirmando = true }) {
                    Text("Cancelar reserva", color = Peligro, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmando) {
        DialogoConfirmacion(
            titulo = "Cancelar reserva",
            mensaje = "¿Cancelar \"${reserva.titulo}\" del ${Fechas.legible(reserva.fecha)} a las ${reserva.horaInicio}? " +
                    "Si faltan menos de ${Catalogos.CANCELACION_SIN_PENALIZACION_HORAS} horas, cuenta como cancelación tardía.",
            textoConfirmar = "Cancelar reserva",
            onConfirmar = {
                confirmando = false
                onCancelar()
            },
            onCancelar = { confirmando = false }
        )
    }
}
