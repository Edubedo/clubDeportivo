package com.example.clubdeportivo.ui.admin.reportes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TituloSeccion
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.util.reportes.BloqueResumen
import com.example.clubdeportivo.util.reportes.PeriodoReporte
import com.example.clubdeportivo.util.reportes.Reporte
import com.example.clubdeportivo.util.reportes.SeccionReporte

/**
 * Reportes del administrador: elige el periodo y qué partes del club incluir, revisa el resumen y exporta todo en
 * PDF o Excel para compartirlo con quien le da seguimiento al club sin tener acceso a la app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportesAdminScreen(viewModel: ReportesViewModel = viewModel()) {
    val contexto = LocalContext.current
    val reporte = viewModel.reporte

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargenPantalla)
            .padding(bottom = 24.dp)
    ) {
        EncabezadoPantalla(titulo = Textos.get("rep_titulo"), subtitulo = Textos.get("rep_subtitulo"))

        TituloSeccion(Textos.get("rep_periodo"))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodoReporte.entries.forEach { periodo ->
                FilterChip(
                    selected = viewModel.periodo == periodo,
                    onClick = { viewModel.elegirPeriodo(periodo) },
                    label = { Text(periodo.etiqueta) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TituloSeccion(Textos.get("rep_incluir"))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeccionReporte.entries.forEach { seccion ->
                val fallo = seccion in viewModel.seccionesConError
                val elegida = seccion in viewModel.secciones
                FilterChip(
                    selected = elegida && !fallo,
                    enabled = !fallo,
                    onClick = { viewModel.alternarSeccion(seccion) },
                    label = { Text(seccion.etiqueta) },
                    leadingIcon = when {
                        fallo -> { { Icon(Icons.Filled.ErrorOutline, null, tint = Peligro, modifier = Modifier.size(18.dp)) } }
                        elegida -> { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) } }
                        else -> null
                    },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
        if (viewModel.seccionesConError.isNotEmpty() && !viewModel.cargando) {
            Text(
                Textos.get("rep_secciones_error"),
                color = Peligro,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val listo = !viewModel.cargando && reporte != null && reporte.tablas.isNotEmpty()
            BotonExportar(
                texto = Textos.get("rep_exportar_pdf"),
                icono = { Icon(Icons.Filled.PictureAsPdf, null) },
                trabajando = viewModel.exportando == FormatoReporte.PDF,
                habilitado = listo && viewModel.exportando == null,
                color = Peligro,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.exportar(contexto, FormatoReporte.PDF) }
            )
            BotonExportar(
                texto = Textos.get("rep_exportar_excel"),
                icono = { Icon(Icons.Filled.TableChart, null) },
                trabajando = viewModel.exportando == FormatoReporte.EXCEL,
                habilitado = listo && viewModel.exportando == null,
                color = Exito,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.exportar(contexto, FormatoReporte.EXCEL) }
            )
        }
        Text(
            Textos.get("rep_ayuda_formatos"),
            color = TextoSecundario,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(22.dp))
        TituloSeccion(Textos.get("rep_vista_previa"))
        Spacer(Modifier.height(10.dp))
        when {
            viewModel.cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text(Textos.get("rep_cargando"), color = TextoSecundario)
                }
            }
            reporte == null || reporte.tablas.isEmpty() -> TarjetaClub(Modifier.fillMaxWidth()) {
                Text(Textos.get("rep_nada_elegido"), color = TextoSecundario, modifier = Modifier.padding(18.dp))
            }
            else -> VistaPrevia(reporte)
        }
    }

    viewModel.archivoListo?.let { listo -> DialogoArchivoListo(viewModel, listo) }

    viewModel.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = viewModel::limpiarAvisos,
            title = { Text(Textos.get("the_action_could_not_be_completed"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text(mensaje, color = TextoSecundario) },
            confirmButton = { TextButton(onClick = viewModel::limpiarAvisos) { Text(Textos.get("got_it")) } }
        )
    }
    LaunchedEffect(viewModel.avisoGuardado) {
        if (viewModel.avisoGuardado) {
            Toast.makeText(contexto, Textos.get("rep_guardado"), Toast.LENGTH_SHORT).show()
            viewModel.limpiarAvisos()
        }
    }
}

@Composable
private fun BotonExportar(
    texto: String,
    icono: @Composable () -> Unit,
    trabajando: Boolean,
    habilitado: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (trabajando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else icono()
        Spacer(Modifier.size(8.dp))
        Text(texto, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun VistaPrevia(reporte: Reporte) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(reporte.periodo, color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
        reporte.resumen.forEach { TarjetaResumen(it) }
        TarjetaClub(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(Textos.get("rep_tablas_incluidas"), style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                Spacer(Modifier.height(8.dp))
                reporte.tablas.forEachIndexed { i, tabla ->
                    if (i > 0) HorizontalDivider(color = Borde)
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tabla.titulo, color = TextoPrincipal)
                        Text(Textos.get("rep_registros", tabla.filas.size), color = TextoSecundario)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumen(bloque: BloqueResumen) {
    TarjetaClub(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(bloque.titulo, style = MaterialTheme.typography.titleSmall, color = Marca, fontWeight = FontWeight.Bold)
            bloque.datos.forEach { (concepto, valor) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        concepto, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(valor, color = TextoPrincipal, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DialogoArchivoListo(viewModel: ReportesViewModel, listo: ArchivoReporte) {
    val contexto = LocalContext.current
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(listo.formato.mime)) { destino ->
        if (destino != null) viewModel.guardarEn(contexto, destino)
    }

    fun lanzar(intent: Intent) {
        try {
            contexto.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(contexto, Textos.get("rep_sin_app"), Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = viewModel::cerrarArchivo,
        title = { Text(Textos.get("rep_listo_titulo"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(listo.archivo.name, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        val enviar = Intent(Intent.ACTION_SEND).apply {
                            type = listo.formato.mime
                            putExtra(Intent.EXTRA_STREAM, listo.uri)
                            putExtra(Intent.EXTRA_SUBJECT, listo.archivo.nameWithoutExtension)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        lanzar(Intent.createChooser(enviar, Textos.get("rep_compartir")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(Textos.get("rep_compartir"))
                }
                OutlinedButton(
                    onClick = {
                        lanzar(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(listo.uri, listo.formato.mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(Textos.get("rep_abrir"))
                }
                OutlinedButton(onClick = { guardar.launch(listo.archivo.name) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Download, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(Textos.get("rep_guardar"))
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::cerrarArchivo) { Text(Textos.get("close")) } }
    )
}
