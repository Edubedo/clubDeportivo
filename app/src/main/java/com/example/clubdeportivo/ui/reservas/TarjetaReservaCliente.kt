package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
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
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Textos

/**
 * Reserva con su estado y lo que se puede hacer con ella: cancelarla (siempre pide confirmación), pedir un cambio de día u
 * horario (lo valida el administrador) o, si el club la canceló por mantenimiento, reagendarla.
 */
@Composable
internal fun TarjetaReservaCliente(
    reserva: ReservaListada,
    onCancelar: () -> Unit,
    fechas: List<Pair<String, String>> = emptyList(),
    onSolicitarCambio: (fecha: String, desde: Int, horas: Int, personas: Int) -> Unit = { _, _, _, _ -> },
    onRetirarCambio: () -> Unit = {},
    onReagendar: () -> Unit = {}
) {
    val pendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION
    val rechazada = reserva.estado == EstadoReserva.RECHAZADA
    val porMantenimiento = reserva.canceladaPorMantenimiento && reserva.estado == EstadoReserva.CANCELADA
    val confirmada = reserva.estado == EstadoReserva.CONFIRMADA
    var confirmando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }

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
                        porMantenimiento -> Textos.get("cancelled_by_maintenance")
                        pendiente -> Textos.get("under_review_2")
                        rechazada -> Textos.get("rejected")
                        else -> Textos.get("confirmed_2")
                    },
                    tipo = when {
                        porMantenimiento -> TipoInsignia.MANTENIMIENTO
                        pendiente -> TipoInsignia.ALERTA
                        rechazada -> TipoInsignia.PELIGRO
                        else -> TipoInsignia.EXITO
                    },
                    icono = when {
                        porMantenimiento -> Icons.Filled.Build
                        pendiente -> Icons.Filled.HourglassTop
                        rechazada -> Icons.Filled.Cancel
                        else -> Icons.Filled.CheckCircle
                    }
                )
            }

            reserva.modificacion?.let { cambio ->
                Text(
                    text = Textos.get(
                        "change_request_pending",
                        Fechas.legible(cambio.fecha), cambio.horaInicio, cambio.horaFin
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 10.dp, end = 8.dp)
                )
            }

            if (porMantenimiento) {
                Text(
                    text = Textos.get(
                        "cancelled_by_maintenance_detail",
                        Fechas.legibleConDia(reserva.mantenimientoDesde), Fechas.legibleConDia(reserva.mantenimientoHasta)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 10.dp, end = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReagendar) {
                        Text(Textos.get("reschedule"), color = Marca, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (rechazada) {
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (confirmada && reserva.modificacion != null) {
                        TextButton(onClick = onRetirarCambio) {
                            Text(Textos.get("change_request_withdraw"), color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (confirmada) {
                        TextButton(onClick = { editando = true }) {
                            Text(Textos.get("request_change"), color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    }
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

    if (editando) {
        DialogoModificarReserva(
            reserva = reserva,
            fechas = fechas,
            onEnviar = { fecha, desde, horas, personas ->
                editando = false
                onSolicitarCambio(fecha, desde, horas, personas)
            },
            onCerrar = { editando = false }
        )
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
