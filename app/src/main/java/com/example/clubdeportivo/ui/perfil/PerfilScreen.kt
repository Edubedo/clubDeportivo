package com.example.clubdeportivo.ui.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.InitialsAvatar

@Composable
fun PerfilScreen(
    onVerMembresia: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val usuario by viewModel.usuario.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InitialsAvatar(nombre = usuario?.nombre ?: "?", size = 88.dp)

        Text(
            text = usuario?.nombre ?: "",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = usuario?.correo ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = usuario?.rol?.nombreLegible() ?: "",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Button(
            onClick = onVerMembresia,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Mi membresía")
        }

        OutlinedButton(
            onClick = {
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}
