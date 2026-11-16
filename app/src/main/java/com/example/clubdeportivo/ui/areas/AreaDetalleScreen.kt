
package com.example.clubdeportivo.ui.areas

import com.example.clubdeportivo.util.tr
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.imagenDeporte

@Composable
fun AreaDetalleScreen(
    area: Area,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        IconButton(onClick = onVolver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = tr("area_det_regresar")
            )
        }

        Text(
            text = Deportes.titulo(area.tipo, area.nombre),
            style = MaterialTheme.typography.headlineSmall
        )

        if (area.imagenUrl.isNotBlank()) {
            AsyncImage(
                model = area.imagenUrl,
                contentDescription = tr("area_det_foto", area.nombre),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = imagenDeporte(area.tipo)),
                contentDescription = tr("area_det_foto", area.nombre),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = tr("area_det_descripcion_titulo"),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = area.descripcion.ifBlank {
                tr("area_det_descripcion_defecto", area.tipo)
            },
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = tr("area_det_entrenadores"),
            style = MaterialTheme.typography.titleMedium
        )

        if (area.entrenadores.isEmpty()) {
            Text(
                text = tr("area_det_sin_entrenadores"),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            area.entrenadores.forEach { entrenador ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )

                        Text(
                            text = entrenador,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = tr("area_det_info"),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = null
            )

            Text(tr("area_det_capacidad", area.capacidad))
        }

        Text(
            text = tr("area_det_estado", when (area.disponibilidad) {
                DisponibilidadArea.DISPONIBLE -> tr("available")
                DisponibilidadArea.OCUPADA -> tr("occupied")
                DisponibilidadArea.MANTENIMIENTO -> tr("under_maintenance")
            })
        )

        if (area.carriles > 0) {
            Text(tr("area_det_carriles", area.carriles))
        }
    }
}
