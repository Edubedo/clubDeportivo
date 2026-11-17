package com.example.clubdeportivo.ui.areas

import com.example.clubdeportivo.util.tr
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.imagenDeporte
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

@Composable
fun AreaDetalleScreen(
    area: Area,
    onVolver: () -> Unit
) {
    val titulo = Deportes.titulo(area.tipo, area.nombre)

    Dialog(
        onDismissRequest = onVolver,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            color = Superficie
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("area_det_regresar"),
                            tint = TextoSecundario
                        )
                    }
                }
                HorizontalDivider(color = Borde)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = MargenPantalla,
                            vertical = 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (area.imagenUrl.isNotBlank()) {
                        AsyncImage(
                            model = area.imagenUrl,
                            contentDescription = tr("area_det_foto", area.nombre),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = imagenDeporte(area.tipo)),
                            contentDescription = tr("area_det_foto", area.nombre),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr("area_det_descripcion_titulo"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextoPrincipal,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = area.descripcion.ifBlank {
                                    tr("area_det_descripcion_defecto", area.tipo)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario
                            )
                        }
                    }

                    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = tr("area_det_info"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextoPrincipal,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    tint = TextoSecundario
                                )
                                Text(
                                    text = tr("area_det_capacidad", area.capacidad),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoPrincipal
                                )
                            }
                            Insignia(
                                texto = when (area.disponibilidad) {
                                    DisponibilidadArea.DISPONIBLE -> tr("available")
                                    DisponibilidadArea.OCUPADA -> tr("occupied")
                                    DisponibilidadArea.MANTENIMIENTO -> tr("under_maintenance")
                                },
                                tipo = when (area.disponibilidad) {
                                    DisponibilidadArea.DISPONIBLE -> TipoInsignia.DISPONIBLE
                                    DisponibilidadArea.OCUPADA -> TipoInsignia.NO_DISPONIBLE
                                    DisponibilidadArea.MANTENIMIENTO -> TipoInsignia.MANTENIMIENTO
                                }
                            )
                            if (area.carriles > 0) {
                                Text(
                                    text = tr("area_det_carriles", area.carriles),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoPrincipal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
