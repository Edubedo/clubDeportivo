package com.example.clubdeportivo.ui.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Textos

/** Pestañas del documento legal: los términos de uso y el aviso de privacidad. */
enum class DocumentoLegal(val etiqueta: String) { TERMINOS(Textos.get("terms")), PRIVACIDAD(Textos.get("privacy")) }

/**
 * Términos y condiciones a pantalla completa, para leerlos con calma antes de aceptarlos. [onAceptar] muestra el botón
 * "Aceptar y continuar" al pie; sin ella (solo lectura), el botón es "Cerrar".
 */
@Composable
fun TerminosScreen(
    documentoInicial: DocumentoLegal = DocumentoLegal.TERMINOS,
    onCerrar: () -> Unit,
    onAceptar: (() -> Unit)? = null
) {
    var pestana by rememberSaveable { mutableIntStateOf(documentoInicial.ordinal) }
    val documento = DocumentoLegal.entries[pestana]

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = FondoApp) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Textos.get("terms_and_privacy"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, contentDescription = Textos.get("close"), tint = TextoSecundario)
                    }
                }
                Text(
                    text = Textos.get("version_updated_on", TerminosTexto.VERSION, TerminosTexto.ULTIMA_ACTUALIZACION),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                PestanasPildora(
                    opciones = DocumentoLegal.entries.map { it.etiqueta },
                    seleccionada = pestana,
                    onSeleccion = { pestana = it }
                )
                HorizontalDivider(color = Borde)

                val secciones = if (documento == DocumentoLegal.TERMINOS) TerminosTexto.terminos else TerminosTexto.privacidad
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(
                            text = if (documento == DocumentoLegal.TERMINOS) Textos.get("terms_and_conditions_of_use") else Textos.get("privacy_notice"),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextoPrincipal,
                            textAlign = TextAlign.Start
                        )
                    }
                    items(secciones, key = { it.titulo }) { seccion ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                seccion.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextoPrincipal,
                                textAlign = TextAlign.Start
                            )
                            seccion.parrafos.forEach {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoSecundario,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Borde)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FondoApp)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    BotonPrimario(
                        texto = if (onAceptar != null) Textos.get("accept_and_continue") else Textos.get("close"),
                        onClick = { onAceptar?.invoke() ?: onCerrar() }
                    )
                }
            }
        }
    }
}
