
package com.example.clubdeportivo.ui.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

@Composable
fun SupervisorHomeScreen(
    onReservas: () -> Unit,
    onAreas: () -> Unit,
    onInventario: () -> Unit,
    onMiembros: () -> Unit
) {
    val nombre = SesionManager.usuarioActual
        ?.nombre
        ?.substringBefore(" ")
        ?: "Supervisor"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Hola, $nombre",
            style = MaterialTheme.typography.headlineSmall,
            color = TextoPrincipal
        )

        Text(
            text = "Panel de supervisión",
            style = MaterialTheme.typography.titleMedium,
            color = TextoSecundario
        )

        Text(
            text = "Gestiona las operaciones de todas las áreas del club.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )

        TarjetaSupervisor(
            titulo = "Reservas",
            descripcion = "Consulta, aprueba y rechaza reservas de todas las áreas.",
            icono = Icons.Filled.CalendarMonth,
            onClick = onReservas
        )

        TarjetaSupervisor(
            titulo = "Áreas deportivas",
            descripcion = "Consulta horarios, capacidad y estado de las instalaciones.",
            icono = Icons.Filled.Place,
            onClick = onAreas
        )

        TarjetaSupervisor(
            titulo = "Inventario",
            descripcion = "Consulta y ajusta las existencias de todas las áreas.",
            icono = Icons.Filled.Inventory2,
            onClick = onInventario
        )

        TarjetaSupervisor(
            titulo = "Registrar miembros",
            descripcion = "Registra nuevos miembros del club.",
            icono = Icons.Filled.Groups,
            onClick = onMiembros
        )
    }
}

@Composable
private fun TarjetaSupervisor(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Superficie
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Marca,
                modifier = Modifier.size(28.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal
                )

                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
    }
}
