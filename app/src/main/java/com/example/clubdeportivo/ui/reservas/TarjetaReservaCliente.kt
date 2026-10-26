package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Textos

/** Reserva vigente con su estado y la opción de cancelarla (siempre pide confirmación antes). */
@Composable
internal fun TarjetaReservaCliente(reserva: ReservaListada, onCancelar: () -> Unit) {
    val pendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION
    val rechazada = reserva.estado == EstadoReserva.RECHAZADA
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
                        text = if (reserva.personas == 1) Textos.get("n_1_person") else Textos.get("people_4", reserva.personas),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                Insignia(
                    texto = when {
                        pendiente -> Textos.get("under_review_2")
                        rechazada -> Textos.get("rejected")
                        else -> Textos.get("confirmed_2")
                    },
                    tipo = when {
                        pendiente -> TipoInsignia.ALERTA
                        rechazada -> TipoInsignia.PELIGRO
                        else -> TipoInsignia.EXITO
                    }
                )
            }

            if (rechazada) {
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { confirmando = true }
                    ) {
                        Text(
                            if (pendiente) Textos.get("cancel_request") else Textos.get("cancel_booking"),
                            color = Peligro,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (confirmando) {
        DialogoConfirmacion(
            titulo = Textos.get("cancel_booking"),
            mensaje = Textos.get("cancel_on_at", reserva.titulo, Fechas.legible(reserva.fecha), reserva.horaInicio) +
                    Textos.get("if_there_are_less_than_hours", Catalogos.CANCELACION_SIN_PENALIZACION_HORAS),
            textoConfirmar = Textos.get("cancel_booking"),
            onConfirmar = {
                confirmando = false
                onCancelar()
            },
            onCancelar = { confirmando = false }
        )
    }
}
