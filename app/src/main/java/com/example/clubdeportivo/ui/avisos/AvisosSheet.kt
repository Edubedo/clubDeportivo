package com.example.clubdeportivo.ui.avisos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoConfirmacion
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.Superficie
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_TITULO = 60
private const val MAX_MENSAJE = 300

private sealed interface ModoAvisos {
    data object Lista : ModoAvisos
    data object Redactar : ModoAvisos
    data class Editar(val aviso: Notificacion) : ModoAvisos
}

/**
 * Ventana de avisos para todos los roles. Socios y visitantes solo leen; el personal además ve lo que envió,
 * redacta avisos nuevos y puede modificar o eliminar los suyos.
 */
@Composable
fun AvisosSheet(onCerrar: () -> Unit, viewModel: AvisosViewModel) {
    var modo by remember { mutableStateOf<ModoAvisos>(ModoAvisos.Lista) }
    var pestana by remember { mutableIntStateOf(0) }
    var aEliminar by remember { mutableStateOf<Notificacion?>(null) }

    var titulo by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    val permitidos = remember { viewModel.destinatariosPermitidos() }
    var destinatario by remember { mutableStateOf(permitidos.first()) }

    LaunchedEffect(Unit) { viewModel.cargar() }

    val esPersonal = viewModel.esPersonal
    val enLista = modo is ModoAvisos.Lista

    DialogoFormulario(
        titulo = when (modo) {
            is ModoAvisos.Lista -> "Avisos del club"
            is ModoAvisos.Redactar -> "Nuevo aviso"
            is ModoAvisos.Editar -> "Modificar aviso"
        },
        onCerrar = { if (enLista) onCerrar() else modo = ModoAvisos.Lista },
        pie = when {
            enLista && esPersonal -> {
                {
                    BotonPrimario(
                        texto = "Redactar nuevo aviso",
                        onClick = {
                            titulo = ""
                            texto = ""
                            destinatario = permitidos.first()
                            modo = ModoAvisos.Redactar
                        }
                    )
                }
            }
            modo is ModoAvisos.Redactar -> {
                {
                    BotonPrimario(
                        texto = "Enviar aviso",
                        enabled = titulo.isNotBlank() && texto.isNotBlank(),
                        cargando = viewModel.enviando,
                        onClick = {
                            viewModel.publicar(titulo, texto, destinatario) {
                                pestana = 1
                                modo = ModoAvisos.Lista
                            }
                        }
                    )
                }
            }
            modo is ModoAvisos.Editar -> {
                {
                    val editando = (modo as ModoAvisos.Editar).aviso
                    BotonPrimario(
                        texto = "Guardar cambios",
                        enabled = titulo.isNotBlank() && texto.isNotBlank(),
                        cargando = viewModel.enviando,
                        onClick = { viewModel.editar(editando.id, titulo, texto) { modo = ModoAvisos.Lista } }
                    )
                }
            }
            else -> null
        }
    ) {
        when (val actual = modo) {
            is ModoAvisos.Lista -> ListaAvisos(
                viewModel = viewModel,
                esPersonal = esPersonal,
                pestana = pestana,
                onPestana = { pestana = it },
                onModificar = { aviso ->
                    titulo = aviso.titulo
                    texto = aviso.mensaje
                    modo = ModoAvisos.Editar(aviso)
                },
                onEliminar = { aEliminar = it }
            )
            is ModoAvisos.Redactar -> {
                if (permitidos.size > 1) {
                    EtiquetaCampo("ENVIAR A")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        permitidos.forEach { opcion ->
                            FilterChip(
                                selected = destinatario == opcion,
                                onClick = { destinatario = opcion },
                                label = { Text(opcion.etiqueta) },
                                shape = RoundedCornerShape(50.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Marca,
                                    selectedLabelColor = SobreMarca
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Este aviso lo recibirán los miembros del club.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                EspacioCampos()
                CamposAviso(titulo, { titulo = it.take(MAX_TITULO) }, texto, { texto = it.take(MAX_MENSAJE) })
            }
            is ModoAvisos.Editar -> {
                Text(
                    text = "Para: ${actual.aviso.destinatario.etiqueta}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                EspacioCampos()
                CamposAviso(titulo, { titulo = it.take(MAX_TITULO) }, texto, { texto = it.take(MAX_MENSAJE) })
            }
        }
    }

    aEliminar?.let { aviso ->
        DialogoConfirmacion(
            titulo = "Eliminar aviso",
            mensaje = "¿Eliminar \"${aviso.titulo}\"? Dejará de verse para todos y no se puede deshacer.",
            textoConfirmar = "Eliminar",
            textoCancelar = "Cancelar",
            onConfirmar = {
                viewModel.eliminar(aviso.id)
                aEliminar = null
            },
            onCancelar = { aEliminar = null }
        )
    }
}

@Composable
private fun CamposAviso(
    titulo: String,
    onTitulo: (String) -> Unit,
    texto: String,
    onTexto: (String) -> Unit
) {
    EtiquetaCampo("TÍTULO")
    CampoTexto(
        value = titulo,
        onValueChange = onTitulo,
        placeholder = "Ej: Mantenimiento programado",
        capitalizacion = KeyboardCapitalization.Sentences,
        mensajeError = "${titulo.length}/$MAX_TITULO"
    )

    EspacioCampos()

    EtiquetaCampo("MENSAJE")
    CampoTexto(
        value = texto,
        onValueChange = onTexto,
        placeholder = "Escribe los detalles del aviso...",
        singleLine = false,
        minLines = 4,
        capitalizacion = KeyboardCapitalization.Sentences,
        mensajeError = "${texto.length}/$MAX_MENSAJE"
    )
}

@Composable
private fun ListaAvisos(
    viewModel: AvisosViewModel,
    esPersonal: Boolean,
    pestana: Int,
    onPestana: (Int) -> Unit,
    onModificar: (Notificacion) -> Unit,
    onEliminar: (Notificacion) -> Unit
) {
    if (esPersonal) {
        PestanasPildora(
            opciones = listOf(
                if (viewModel.sinLeer > 0) "Recibidos (${viewModel.sinLeer})" else "Recibidos",
                "Enviados"
            ),
            seleccionada = pestana,
            onSeleccion = onPestana,
            margenHorizontal = 0.dp
        )
        Spacer(Modifier.height(8.dp))
    }

    val verEnviados = esPersonal && pestana == 1
    val lista = if (verEnviados) viewModel.enviados else viewModel.recibidos

    when {
        viewModel.cargando && lista.isEmpty() -> {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
        lista.isEmpty() -> Text(
            text = if (verEnviados) "Todavía no has enviado ningún aviso." else "No hay avisos por ahora.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
        )
        else -> {
            if (!verEnviados && viewModel.sinLeer > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.marcarTodosLeidos() }) {
                        Text("Marcar todos como leídos", color = Marca, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                lista.forEach { aviso ->
                    TarjetaAviso(
                        aviso = aviso,
                        sinLeer = !verEnviados && !viewModel.esLeido(aviso.id),
                        esEnviado = verEnviados,
                        onAbrir = { if (!verEnviados) viewModel.marcarLeido(aviso.id) },
                        onModificar = { onModificar(aviso) },
                        onEliminar = { onEliminar(aviso) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaAviso(
    aviso: Notificacion,
    sinLeer: Boolean,
    esEnviado: Boolean,
    onAbrir: () -> Unit,
    onModificar: () -> Unit,
    onEliminar: () -> Unit
) {
    val fecha = remember(aviso.fechaMillis) {
        SimpleDateFormat("d MMM, h:mm a", Locale("es", "MX")).format(Date(aviso.fechaMillis))
    }
    var expandido by remember { mutableStateOf(false) }

    TarjetaClub(
        modifier = Modifier.fillMaxWidth(),
        fondo = if (sinLeer) MarcaSuave else Superficie,
        onClick = {
            expandido = !expandido
            onAbrir()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (sinLeer) {
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Marca, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = aviso.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal,
                        maxLines = if (expandido) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = fecha, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = aviso.mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                maxLines = if (expandido) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (esEnviado) {
                Insignia(texto = "Para: ${aviso.destinatario.etiqueta}", tipo = TipoInsignia.NEUTRO)
            } else if (aviso.autorNombre.isNotBlank()) {
                Text(text = "De: ${aviso.autorNombre}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }

            if (esEnviado && expandido) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onModificar) {
                        Text("Modificar", color = Marca, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onEliminar) {
                        Text("Eliminar", color = Peligro, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
