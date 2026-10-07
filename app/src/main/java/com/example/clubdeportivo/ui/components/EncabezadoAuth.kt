package com.example.clubdeportivo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.SobreMarca

/** Cabecera azul compartida por el inicio de sesión y el registro. Se extiende detrás de la barra de estado. */
@Composable
fun EncabezadoAuth(subtitulo: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Marca)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SobreMarca.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsSoccer,
                    contentDescription = null,
                    tint = SobreMarca,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Athletic Club",
                style = MaterialTheme.typography.headlineLarge,
                color = SobreMarca
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyLarge,
                color = SobreMarca.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
