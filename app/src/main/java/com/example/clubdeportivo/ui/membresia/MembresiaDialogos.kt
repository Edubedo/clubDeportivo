package com.example.clubdeportivo.ui.membresia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MotivoSuspension
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

/** Máximo de caracteres del detalle cuando el motivo es "Otro". */
private const val LIMITE_DETALLE = 120

/** true si hay un motivo elegido y, cuando es "Otro", un detalle escrito. */
internal fun motivoCompleto(seleccion: MotivoSuspension?, detalle: String): Boolean =
    seleccion != null && (!seleccion.pideDetalle || detalle.trim().length >= 3)

/** Opciones de motivo de suspensión (una sola elección) y, para "Otro motivo", un campo para explicarlo. */
@Composable
internal fun SelectorMotivoSuspension(
    seleccion: MotivoSuspension?,
    onSeleccion: (MotivoSuspension) -> Unit,
    detalle: String,
    onDetalle: (String) -> Unit
) {
    Column {
        MotivoSuspension.entries.forEach { motivo ->
            Row(
                modifier = if (seleccion == motivo) {
                    modifierReflejo(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) { onSeleccion(motivo) }
                            .padding(vertical = 2.dp),
                        enabled = true
                    )
                } else {
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onSeleccion(motivo) }
                        .padding(vertical = 2.dp)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = seleccion == motivo,
                    onClick = null,
                    modifier = Modifier.padding(12.dp),
                    colors = RadioButtonDefaults.colors(selectedColor = Marca, unselectedColor = BordeCampo)
                )
                Text(
                    text = motivo.etiqueta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoPrincipal
                )
            }
        }
        if (seleccion?.pideDetalle == true) {
            Spacer(modifier = Modifier.height(8.dp))
            CampoTexto(
                value = detalle,
                onValueChange = { onDetalle(it.take(LIMITE_DETALLE)) },
                placeholder = Textos.get(R.string.write_the_reason),
                singleLine = false,
                minLines = 2,
                capitalizacion = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        }
    }
}

/** Pregunta por qué se suspende una membresía. El motivo queda guardado y el miembro puede verlo. */
@Composable
internal fun DialogoSuspension(
    nombre: String,
    onConfirmar: (motivo: String) -> Unit,
    onCancelar: () -> Unit
) {
    var seleccion by remember { mutableStateOf<MotivoSuspension?>(null) }
    var detalle by remember { mutableStateOf("") }

    DialogoFormulario(
        titulo = Textos.get(R.string.suspend_membership),
        onCerrar = onCancelar,
        pie = {
            BotonPrimario(
                texto = Textos.get(R.string.suspend_membership),
                enabled = motivoCompleto(seleccion, detalle),
                onClick = { seleccion?.let { onConfirmar(it.textoGuardado(detalle)) } }
            )
        }
    ) {
        Text(
            text = Textos.get(R.string.why_is_s_membership_being_suspended, nombre),
            style = MaterialTheme.typography.titleSmall,
            color = TextoPrincipal
        )
        Text(
            text = Textos.get(R.string.while_it_is_suspended_they_won),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        SelectorMotivoSuspension(
            seleccion = seleccion,
            onSeleccion = { seleccion = it },
            detalle = detalle,
            onDetalle = { detalle = it }
        )
    }
}

/** Resumen de la renovación y forma de pago; al confirmar se registra el cobro. */
@Composable
internal fun DialogoRenovar(
    nombre: String,
    plan: String,
    precio: Double,
    vigenteHasta: String,
    onConfirmar: (MetodoPago) -> Unit,
    onCancelar: () -> Unit
) {
    var metodo by remember { mutableStateOf(MetodoPago.EFECTIVO) }

    DialogoFormulario(
        titulo = Textos.get(R.string.renew_membership),
        onCerrar = onCancelar,
        pie = {
            BotonPrimario(
                texto = Textos.get(R.string.charge_and_renew, dinero(precio)),
                onClick = { onConfirmar(metodo) }
            )
        }
    ) {
        TarjetaClub(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(nombre, style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                Text(plan, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                Text(
                    text = Textos.get(R.string.one_more_month_until, vigenteHasta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Text(
                    text = dinero(precio),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Marca,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        EtiquetaCampo(Textos.get(R.string.payment_method))
        PestanasPildora(
            opciones = MetodoPago.entries.map { it.etiqueta },
            seleccionada = metodo.ordinal,
            onSeleccion = { metodo = MetodoPago.entries[it] },
            margenHorizontal = 0.dp
        )
    }
}
