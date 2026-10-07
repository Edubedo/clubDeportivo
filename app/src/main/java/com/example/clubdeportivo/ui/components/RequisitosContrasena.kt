package com.example.clubdeportivo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.ReglasContrasena
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

/** Lista de requisitos de la contraseña: cada uno se marca en verde en cuanto se cumple. */
@Composable
fun RequisitosContrasena(contrasena: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FondoApp, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReglasContrasena.requisitos(contrasena).forEach { requisito ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (requisito.cumple) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (requisito.cumple) "Cumple" else "Falta",
                    tint = if (requisito.cumple) Exito else BordeCampo,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = requisito.texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (requisito.cumple) Exito else TextoSecundario
                )
            }
        }
    }
}
