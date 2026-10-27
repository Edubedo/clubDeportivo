package com.example.clubdeportivo.ui.admin.home

import com.example.clubdeportivo.ui.components.EsqueletoLista
import com.example.clubdeportivo.ui.components.EsqueletoDashboard
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.publicaciones.AudienciaPublicacion
import com.example.clubdeportivo.data.publicaciones.Publicacion
import com.example.clubdeportivo.ui.publicaciones.PublicacionesViewModel
import com.example.clubdeportivo.ui.components.BurbujaTexto
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.components.TituloSeccion
import com.example.clubdeportivo.ui.membresia.dinero
import com.example.clubdeportivo.ui.theme.Borde
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.ActionBlue
import com.example.clubdeportivo.ui.theme.ActionOrange
import com.example.clubdeportivo.ui.theme.CourtBlueBright
import com.example.clubdeportivo.ui.theme.AlertOnDark
import com.example.clubdeportivo.ui.theme.DeepNavy
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.PaymentCard
import com.example.clubdeportivo.ui.theme.PaymentCash
import com.example.clubdeportivo.ui.theme.PaymentTransfer
import com.example.clubdeportivo.ui.theme.SlateAccent
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.PrimaryNavy
import com.example.clubdeportivo.ui.theme.SuccessOnDark
import com.example.clubdeportivo.ui.theme.SoftBlueSelection
import com.example.clubdeportivo.ui.theme.AzulAccion
import com.example.clubdeportivo.ui.theme.NavyFijo
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.ResumenAdmin
import com.example.clubdeportivo.util.comprimirImagenPublicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import com.example.clubdeportivo.util.Textos

@Composable
fun AdminHomeScreen(
    viewModel: AdminHomeViewModel = viewModel(),
    publicacionesViewModel: PublicacionesViewModel = viewModel(),
    onIrReservas: () -> Unit = {},
    onIrMembresia: () -> Unit = {}
) {
    PanelAdmin(
        resumen = viewModel.resumen,
        operacion = viewModel.operacion,
        proximasReservas = viewModel.proximasReservas,
        cargando = viewModel.cargando,
        errorFinanzas = viewModel.errorFinanzas,
        onActualizar = { viewModel.cargar() },
        publicacionesViewModel = publicacionesViewModel,
        onIrReservas = onIrReservas,
        onIrMembresia = onIrMembresia
    )
}

/** Contenido del dashboard sin ViewModel: así se puede dibujar (y revisar) con cualquier dato. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelAdmin(
    resumen: ResumenAdmin?,
    operacion: OperacionHoy,
    proximasReservas: List<Reserva>?,
    cargando: Boolean,
    errorFinanzas: Boolean,
    onActualizar: () -> Unit,
    publicacionesViewModel: PublicacionesViewModel = viewModel(),
    onIrReservas: () -> Unit = {},
    onIrMembresia: () -> Unit = {}
) {
    var mostrandoNovedades by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = mostrandoNovedades) { mostrandoNovedades = false }
    if (mostrandoNovedades) {
        NovedadesAdminScreen(
            publicacionesViewModel = publicacionesViewModel
        )
        return
    }
    PullToRefreshBox(
        isRefreshing = cargando && (resumen != null || errorFinanzas),
        onRefresh = onActualizar,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MargenPantalla)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EncabezadoPantalla(
                    titulo = Textos.get("dashboard"),
                    modifier = Modifier.weight(1f),
                    subtitulo = resumen?.let { Textos.get("summary", Fechas.nombreMes(it.mes)) } ?: Textos.get("club_summary")
                )
                Button(
                    onClick = { mostrandoNovedades = true },
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Marca)
                ) {
                    Text(Textos.get("posts"), fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            when {
                resumen == null && cargando -> {
                    EsqueletoDashboard()
                }
                resumen == null -> AvisoError()
                else -> {
                    TarjetaIngresos(resumen)

                    Spacer(modifier = Modifier.height(12.dp))
                    GraficaIngresos(resumen)

                    Spacer(modifier = Modifier.height(22.dp))
                    AccionesAdmin(
                        pendientes = operacion.pendientesDeAprobar,
                        porVencer = resumen.porVencer,
                        onIrReservas = onIrReservas,
                        onIrMembresia = onIrMembresia
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    TituloSeccion(Textos.get("memberships"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                        Metrica(
                            icono = Icons.Filled.CardMembership,
                            valor = resumen.activas.toString(),
                            titulo = Textos.get("active_memberships"),
                            detalle = Textos.get("people_3", resumen.personasActivas),
                            modifier = Modifier.weight(1f),
                            tipo = TipoInsignia.METRICA_ACTIVAS
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                        Metrica(
                            icono = Icons.Filled.PersonAdd,
                            valor = resumen.altasMes.toString(),
                            titulo = Textos.get("new_sign_ups_this_month"),
                            modifier = Modifier.weight(1f),
                            tipo = TipoInsignia.METRICA_ALTAS
                        )
                        Metrica(
                            icono = Icons.Filled.HourglassTop,
                            valor = resumen.porVencer.size.toString(),
                            titulo = Textos.get("expire_in_days", com.example.clubdeportivo.util.CalculoResumenAdmin.DIAS_POR_VENCER),
                            tipo = TipoInsignia.METRICA_VENCIMIENTOS,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                        Metrica(
                            icono = Icons.Filled.EventBusy,
                            valor = resumen.vencidas.toString(),
                            titulo = Textos.get("expired_2"),
                            tipo = TipoInsignia.METRICA_VENCIDAS,
                            modifier = Modifier.weight(1f)
                        )
                        Metrica(
                            icono = Icons.Filled.Pause,
                            valor = resumen.suspendidas.toString(),
                            titulo = Textos.get("suspended_2"),
                            tipo = TipoInsignia.METRICA_SUSPENDIDAS,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TituloSeccion(Textos.get("expiring_this_week"))
                    Spacer(modifier = Modifier.height(12.dp))
                    ListaPorVencer(resumen.porVencer)

                    if (resumen.ingresosPorMetodo.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        TituloSeccion(Textos.get("payments_by_method"))
                        Spacer(modifier = Modifier.height(12.dp))
                        CobrosPorMetodo(resumen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TituloSeccion(Textos.get("at_the_club_today"))
            Spacer(modifier = Modifier.height(12.dp))
            OperacionDelDia(operacion)

            Spacer(modifier = Modifier.height(24.dp))
            TituloSeccion(Textos.get("upcoming_bookings"))
            Spacer(modifier = Modifier.height(12.dp))
            val proximas = proximasReservas
            when {
                proximas == null -> EsqueletoLista(cantidad = 2)
                proximas.isEmpty() -> TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = Textos.get("no_upcoming_bookings"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    proximas.forEach { ReservaResumen(it) }
                }
            }
        }
    }
}

@Composable
private fun AccionesAdmin(
    pendientes: Int?,
    porVencer: List<MembresiaDetalle>,
    onIrReservas: () -> Unit,
    onIrMembresia: () -> Unit
) {
    var dialogo by remember { mutableStateOf<String?>(null) }
    var recordatorioEnviado by remember { mutableStateOf(false) }

    TituloSeccion(Textos.get("needs_your_attention"))
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaAtencion(
            cantidad = pendientes?.toString() ?: "—",
            titulo = Textos.get("pending_requests_2"),
            detalle = Textos.get("bookings_to_approve"),
            color = ActionOrange,
            modifier = Modifier.weight(1f),
            onClick = onIrReservas
        )
        TarjetaAtencion(
            cantidad = porVencer.size.toString(),
            titulo = Textos.get("memberships_expiring"),
            detalle = Textos.get("in_the_next_7_days"),
            color = AzulAccion,
            modifier = Modifier.weight(1f),
            onClick = { dialogo = "membresias" }
        )
    }

    when (dialogo) {
        "membresias" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text(Textos.get("memberships_expiring"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (porVencer.isEmpty()) {
                        Text(Textos.get("no_memberships_expiring_this_week"), color = TextoSecundario)
                    } else {
                        porVencer.take(7).forEach { detalle ->
                            Column {
                                Text(detalle.titular?.nombre ?: Textos.get("club_member"), color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                                Text(Textos.get("expires_on", Fechas.legible(detalle.membresia.fechaVencimiento)), color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (recordatorioEnviado) {
                        Text(Textos.get("reminders_prepared_for_the_members_on"), color = Exito, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recordatorioEnviado = true }) {
                    Text(if (recordatorioEnviado) Textos.get("sent_2") else Textos.get("send_reminders"))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogo = null; onIrMembresia() }) { Text(Textos.get("view_memberships")) }
            }
        )
    }
}

@Composable
private fun NovedadesAdminScreen(
    publicacionesViewModel: PublicacionesViewModel
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var publicacionEditando by remember { mutableStateOf<Publicacion?>(null) }
    var publicacionDetalle by remember { mutableStateOf<Publicacion?>(null) }
    var publicacionParaBorrar by remember { mutableStateOf<Publicacion?>(null) }
    var publicacionGuardada by remember { mutableStateOf(false) }
    val publicaciones = publicacionesViewModel.propias.sortedByDescending { it.fechaMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargenPantalla)
            .padding(bottom = 24.dp)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EncabezadoPantalla(
                    titulo = Textos.get("posts"),
                    modifier = Modifier.weight(1f),
                    subtitulo = Textos.get("club_news_2")
                )
                FilledTonalButton(
                    onClick = { publicacionEditando = null; mostrarFormulario = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(Textos.get("create"), maxLines = 1)
            }
        }

        if (publicaciones.isEmpty()) {
            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Text(
                    Textos.get("there_are_no_posts_yet_create"),
                    modifier = Modifier.padding(18.dp),
                    color = TextoSecundario
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                publicaciones.forEach { aviso ->
                    TarjetaClub(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { publicacionDetalle = aviso }
                    ) {
                        if (aviso.imagenUrl.isNotBlank()) {
                            AsyncImage(
                                model = aviso.imagenUrl,
                                contentDescription = aviso.titulo,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Insignia(Textos.get("published"), TipoInsignia.EXITO)
                            Spacer(Modifier.height(7.dp))
                            Text(aviso.titulo, style = MaterialTheme.typography.titleMedium, color = TextoPrincipal, fontWeight = FontWeight.Bold)
                            Text(
                                Textos.get("for_3", aviso.audiencia.etiqueta) +
                                    aviso.vigencia.takeIf { it.isNotBlank() }?.let { Textos.get("until", Fechas.legible(it)) }.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSecundario
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { publicacionDetalle = aviso },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        Textos.get("view_details"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    publicacionesViewModel.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = publicacionesViewModel::limpiarError,
            title = { Text(Textos.get("the_action_could_not_be_completed"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text(mensaje, color = TextoSecundario) },
            confirmButton = { TextButton(onClick = publicacionesViewModel::limpiarError) { Text(Textos.get("got_it")) } }
        )
    }

    if (mostrarFormulario) {
        FormularioPublicacion(
            inicial = publicacionEditando,
            enviando = publicacionesViewModel.enviando,
            onCerrar = { mostrarFormulario = false; publicacionEditando = null },
            onPublicar = { datos ->
                publicacionesViewModel.guardar(
                    id = publicacionEditando?.id,
                    titulo = datos.titulo,
                    descripcion = datos.mensaje,
                    imagenUrl = datos.imagenUrl,
                    audiencia = datos.destinatario,
                    vigencia = datos.vigencia
                ) {
                    mostrarFormulario = false
                    publicacionEditando = null
                    publicacionGuardada = true
                }
            }
        )
    }

    publicacionDetalle?.let { publicacion ->
        Dialog(
            onDismissRequest = { publicacionDetalle = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (publicacion.imagenUrl.isNotBlank()) {
                            AsyncImage(
                                model = publicacion.imagenUrl,
                                contentDescription = publicacion.titulo,
                                modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Insignia(Textos.get("published"), TipoInsignia.EXITO)
                        Text(publicacion.titulo, color = TextoPrincipal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(publicacion.descripcion, color = TextoSecundario, style = MaterialTheme.typography.bodyLarge)
                        Text(Textos.get("audience", publicacion.audiencia.etiqueta), color = TextoSecundario)
                        if (publicacion.vigencia.isNotBlank()) Text(Textos.get("valid_until", Fechas.legible(publicacion.vigencia)), color = TextoSecundario)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { publicacionDetalle = null; publicacionEditando = publicacion; mostrarFormulario = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(Textos.get("edit")) }
                    OutlinedButton(
                        onClick = { publicacionDetalle = null; publicacionParaBorrar = publicacion },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(Textos.get("delete_post"), color = Peligro) }
                    OutlinedButton(
                        onClick = { publicacionDetalle = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Peligro)
                    ) { Text(Textos.get("close")) }
                }
            }
        }
    }

    publicacionParaBorrar?.let { publicacion ->
        AlertDialog(
            onDismissRequest = { publicacionParaBorrar = null },
            title = { Text(Textos.get("delete_post"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text(Textos.get("do_you_want_to_delete_this", publicacion.titulo), color = TextoSecundario) },
            confirmButton = {
                TextButton(onClick = {
                    publicacionesViewModel.eliminar(publicacion)
                    publicacionParaBorrar = null
                }) { Text(Textos.get("delete_2"), color = Peligro) }
            },
            dismissButton = { TextButton(onClick = { publicacionParaBorrar = null }) { Text(Textos.get("cancel")) } }
        )
    }

    if (publicacionGuardada) {
        AlertDialog(
            onDismissRequest = { publicacionGuardada = false },
            title = { Text(Textos.get("news_saved"), color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text(Textos.get("the_post_was_saved_successfully"), color = TextoSecundario) },
            confirmButton = { TextButton(onClick = { publicacionGuardada = false }) { Text(Textos.get("done")) } }
        )
    }
}

@Composable
private fun TarjetaAtencion(
    cantidad: String,
    titulo: String,
    detalle: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TarjetaClub(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(cantidad, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioPublicacion(
    inicial: Publicacion? = null,
    enviando: Boolean = false,
    onCerrar: () -> Unit,
    onPublicar: (DatosFormularioPublicacion) -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var titulo by remember(inicial) { mutableStateOf(inicial?.titulo.orEmpty()) }
    var descripcion by remember(inicial) { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var imagen by remember(inicial) { mutableStateOf(inicial?.imagenUrl.orEmpty()) }
    var vigencia by remember(inicial) { mutableStateOf(inicial?.vigencia.orEmpty()) }
    var audiencia by remember(inicial) {
        mutableStateOf(
            inicial?.audiencia ?: AudienciaPublicacion.TODOS
        )
    }
    var errorImagen by remember { mutableStateOf<String?>(null) }
    var comprimiendoImagen by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    val fechasDesdeHoy = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= (fechaASeleccionMillis(Fechas.hoy()) ?: 0L)
        }
    }
    val estadoFecha = rememberDatePickerState(
        initialSelectedDateMillis = fechaASeleccionMillis(vigencia)?.takeIf { vigencia >= Fechas.hoy() },
        selectableDates = fechasDesdeHoy
    )
    var errorVigencia by remember(inicial) { mutableStateOf(false) }
    val selectorImagen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            alcance.launch {
                comprimiendoImagen = true
                val jpeg = withContext(Dispatchers.IO) { comprimirImagenPublicacion(contexto.contentResolver, uri) }
                if (jpeg == null) {
                    errorImagen = Textos.get("the_image_could_not_be_read")
                } else {
                    val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
                    if (dataUri.length > MAX_PORTADA_FIRESTORE_CARACTERES) {
                        errorImagen = Textos.get("the_cover_is_still_too_large")
                    } else {
                        imagen = dataUri
                        errorImagen = null
                    }
                }
                comprimiendoImagen = false
            }
        }
    }
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    if (inicial == null) Textos.get("publish_news") else Textos.get("edit_news"),
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                OutlinedButton(onClick = { selectorImagen.launch("image/*") }, enabled = !comprimiendoImagen, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Text(if (comprimiendoImagen) Textos.get("preparing_image") else if (imagen.isBlank()) Textos.get("choose_cover_photo") else Textos.get("change_cover_photo"))
                }
                if (imagen.isNotBlank()) {
                    AsyncImage(model = imagen, contentDescription = Textos.get("cover_preview"), modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
                errorImagen?.let { Text(it, color = Peligro, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(value = titulo, onValueChange = { titulo = it.take(60) }, label = { Text(Textos.get("title_2")) }, singleLine = true)
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it.take(300) },
                    label = { Text(Textos.get("description")) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { mostrarCalendario = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(if (vigencia.isBlank()) Textos.get("choose_end_date") else Textos.get("valid_until_2", Fechas.legible(vigencia)))
                }
                if (errorVigencia || (vigencia.isNotBlank() && vigencia < Fechas.hoy())) {
                    Text(Textos.get("the_end_date_must_be_today"), color = Peligro, style = MaterialTheme.typography.bodySmall)
                }
                Text(Textos.get("audience_2"), color = TextoSecundario, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AudienciaPublicacion.TODOS to Textos.get("all_2"),
                        AudienciaPublicacion.PERSONAL to Textos.get("staff"),
                        AudienciaPublicacion.SOCIOS to Textos.get("members_2")
                    ).forEach { (destinatario, etiqueta) ->
                        FilterChip(selected = audiencia == destinatario, onClick = { audiencia = destinatario }, label = { Text(etiqueta) })
                    }
                }
            }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCerrar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Peligro)
                    ) { Text(Textos.get("cancel")) }
                    Button(
                        onClick = {
                            if (vigencia.isNotBlank() && vigencia < Fechas.hoy()) {
                                errorVigencia = true
                            } else {
                                onPublicar(DatosFormularioPublicacion(titulo.trim(), descripcion.trim(), imagen, vigencia, audiencia))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = titulo.isNotBlank() && descripcion.isNotBlank() && imagen.isNotBlank() &&
                            (vigencia.isBlank() || vigencia >= Fechas.hoy()) && !comprimiendoImagen && !enviando
                    ) { Text(if (enviando) Textos.get("saving") else if (inicial != null) Textos.get("save") else Textos.get("publish")) }
                }
            }
        }
    }

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    val seleccion = estadoFecha.selectedDateMillis
                    if (seleccion == null) {
                        vigencia = ""
                        errorVigencia = false
                        mostrarCalendario = false
                    } else {
                        val seleccionada = fechaDesdeSeleccionMillis(seleccion)
                        if (seleccionada < Fechas.hoy()) {
                            errorVigencia = true
                        } else {
                            vigencia = seleccionada
                            errorVigencia = false
                            mostrarCalendario = false
                        }
                    }
                }) { Text(Textos.get("ok")) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text(Textos.get("cancel")) }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

private fun fechaASeleccionMillis(fecha: String): Long? =
    runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(fecha)?.time
    }.getOrNull()

private fun fechaDesdeSeleccionMillis(millis: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

private data class DatosFormularioPublicacion(
    val titulo: String,
    val mensaje: String,
    val imagenUrl: String,
    val vigencia: String,
    val destinatario: AudienciaPublicacion
)

private const val MAX_PORTADA_FIRESTORE_CARACTERES = 700 * 1024

@Composable
private fun AvisoError() {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Peligro, modifier = Modifier.size(22.dp))
            Column {
                Text(Textos.get("we_couldn_t_load_the_income"), style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                Text(
                    Textos.get("check_your_connection_and_pull_down") +
                        Textos.get("this_way_we_avoid_showing_you"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** La cifra principal: lo cobrado en el mes y cómo va frente al mes pasado. */
@Composable
private fun TarjetaIngresos(resumen: ResumenAdmin) {
    TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = DeepNavy) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(colors = listOf(NavyFijo, ActionBlue)),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
                    .size(104.dp)
            )
            Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = Textos.get("income", Fechas.nombreMes(resumen.mes)),
                style = MaterialTheme.typography.bodyMedium,
                color = SoftBlueSelection
            )
            Text(
                text = dinero(resumen.ingresosMes),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            val variacion = resumen.variacionContraMesAnterior
            if (variacion != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (variacion >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = if (variacion >= 0) SuccessOnDark else AlertOnDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = Textos.get("n_0f_vs_last_month_s").format(kotlin.math.abs(variacion), dinero(resumen.ingresosMesAnterior)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (variacion >= 0) SuccessOnDark else AlertOnDark
                    )
                }
            } else {
                Text(
                    text = Textos.get("there_are_no_payments_from_last"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Text(
                text = if (resumen.cobrosMes == 1) Textos.get("n_1_payment_recorded") else Textos.get("payments_recorded", resumen.cobrosMes),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(top = 2.dp)
            )
            }
        }
    }
}

/** Barras de los últimos meses; el mes actual va en el color de marca y los anteriores más suaves. */
@Composable
private fun GraficaIngresos(resumen: ResumenAdmin) {
    val maximo = (resumen.ingresosPorMes.maxOfOrNull { it.second } ?: 0.0).takeIf { it > 0 } ?: 1.0
    val alturaMaxima = 96.dp

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(Textos.get("income_by_month"), style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                resumen.ingresosPorMes.forEach { (mes, monto) ->
                    val actual = mes == resumen.mes
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (monto > 0) abreviar(monto) else "",
                            fontSize = 12.sp,
                            fontWeight = if (actual) FontWeight.Bold else FontWeight.Medium,
                            color = if (actual) CourtBlueBright else TextoSecundario,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.height(alturaMaxima).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height((alturaMaxima * (monto / maximo).toFloat()).coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (actual) CourtBlueBright else SlateAccent)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Fechas.nombreMes(mes, corto = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (actual) TextoPrincipal else TextoSecundario,
                            fontWeight = if (actual) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/** 1 250 -> "$1.3k"; 12 400 -> "$12k"; 950 -> "$950". */
private fun abreviar(monto: Double): String = when {
    monto >= 10_000 -> "$%.0fk".format(monto / 1000)
    monto >= 1_000 -> "$%.1fk".format(monto / 1000)
    else -> "$%.0f".format(monto)
}

@Composable
private fun Metrica(
    icono: ImageVector,
    valor: String,
    titulo: String,
    modifier: Modifier = Modifier,
    tipo: TipoInsignia = TipoInsignia.MARCA,
    detalle: String? = null
) {
    TarjetaClub(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tipo.fondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icono, contentDescription = null, tint = tipo.texto, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    text = valor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (detalle != null) {
                    Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
            }
        }
    }
}

@Composable
private fun ListaPorVencer(lista: List<MembresiaDetalle>) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        if (lista.isEmpty()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Exito, modifier = Modifier.size(20.dp))
                Text(
                    text = Textos.get("no_membership_expires_in_the_next"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        } else {
            val hoy = Fechas.hoy()
            lista.take(5).forEachIndexed { indice, detalle ->
                if (indice > 0) HorizontalDivider(color = Borde)
                val vence = detalle.membresia.fechaVencimiento
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detalle.titular?.nombre ?: Textos.get("no_name"),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${ReglasMembresia.etiquetaPlan(detalle.membresia)} · ${dinero(detalle.membresia.precio)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Insignia(
                        texto = when (vence) {
                            hoy -> Textos.get("expires_today")
                            else -> Fechas.legible(vence)
                        },
                        tipo = if (vence == hoy) TipoInsignia.PELIGRO else TipoInsignia.ALERTA
                    )
                }
            }
            if (lista.size > 5) {
                HorizontalDivider(color = Borde)
                Text(
                    text = Textos.get("and_more_in_memberships", lista.size - 5),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CobrosPorMetodo(resumen: ResumenAdmin) {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val total = resumen.ingresosMes.takeIf { it > 0 } ?: 1.0
            resumen.ingresosPorMetodo.forEach { (metodo, monto) ->
                val colorMetodo = when (metodo) {
                    MetodoPago.EFECTIVO -> PaymentCash
                    MetodoPago.TARJETA -> PaymentCard
                    MetodoPago.TRANSFERENCIA -> PaymentTransfer
                    null -> SlateAccent
                }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colorMetodo)
                            )
                            Text(metodo?.etiqueta ?: Textos.get("not_specified_2"), style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
                        }
                        Text(dinero(monto), style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Borde)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((monto / total).toFloat().coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(50))
                                .background(colorMetodo)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OperacionDelDia(operacion: OperacionHoy) {
    fun texto(valor: Int?) = valor?.toString() ?: "–"
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Metrica(
            Icons.Filled.EventAvailable,
            texto(operacion.reservasHoy),
            Textos.get("bookings_today"),
            Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_RESERVAS
        )
        Metrica(
            icono = Icons.Filled.Event,
            valor = texto(operacion.pendientesDeAprobar),
            titulo = Textos.get("to_approve"),
            modifier = Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_APROBAR
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Metrica(
            icono = Icons.Filled.Place,
            valor = texto(operacion.areas),
            titulo = Textos.get("areas"),
            detalle = operacion.areasEnMantenimiento?.takeIf { it > 0 }?.let { Textos.get("under_maintenance_2", it) },
            modifier = Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_MANTENIMIENTO
        )
        Metrica(
            icono = Icons.Filled.Inventory2,
            valor = texto(operacion.articulosConStockBajo),
            titulo = Textos.get("low_stock_items"),
            modifier = Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_STOCK
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Metrica(
            Icons.Filled.Groups,
            texto(operacion.personal),
            Textos.get("staff"),
            Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_PERSONAL
        )
        Metrica(
            Icons.Filled.EmojiEvents,
            texto(operacion.torneosVigentes),
            Textos.get("active_tournaments"),
            Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_TORNEOS
        )
    }
}

@Composable
private fun ReservaResumen(reserva: Reserva) {
    val pendiente = reserva.estado == EstadoReserva.PENDIENTE_APROBACION

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BurbujaTexto(texto = Deportes.emojiDe(reserva.deporte))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reserva.usuarioNombre.ifBlank { Textos.get("user") },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Deportes.titulo(reserva.deporte, reserva.areaNombre)} · ${Fechas.legible(reserva.fecha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${reserva.horaInicio}–${reserva.horaFin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            Insignia(
                texto = if (pendiente) Textos.get("pending") else Textos.get("confirmed_2"),
                tipo = if (pendiente) TipoInsignia.ALERTA else TipoInsignia.EXITO
            )
        }
    }
}
