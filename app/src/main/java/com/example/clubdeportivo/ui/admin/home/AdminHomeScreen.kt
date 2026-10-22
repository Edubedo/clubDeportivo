package com.example.clubdeportivo.ui.admin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardMembership
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.Reserva
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
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.ResumenAdmin

@Composable
fun AdminHomeScreen(viewModel: AdminHomeViewModel = viewModel()) {
    PanelAdmin(
        resumen = viewModel.resumen,
        operacion = viewModel.operacion,
        proximasReservas = viewModel.proximasReservas,
        cargando = viewModel.cargando,
        errorFinanzas = viewModel.errorFinanzas,
        onActualizar = { viewModel.cargar() }
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
    onActualizar: () -> Unit
) {
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
            EncabezadoPantalla(
                titulo = "Dashboard",
                subtitulo = resumen?.let { "Resumen de ${Fechas.nombreMes(it.mes)}" } ?: "Resumen del club"
            )

            when {
                resumen == null && cargando -> {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                resumen == null -> AvisoError()
                else -> {
                    TarjetaIngresos(resumen)

                    Spacer(modifier = Modifier.height(12.dp))
                    GraficaIngresos(resumen)

                    Spacer(modifier = Modifier.height(24.dp))
                    TituloSeccion("Membresías")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                        Metrica(
                            icono = Icons.Filled.CardMembership,
                            valor = resumen.activas.toString(),
                            titulo = "Membresías activas",
                            detalle = "${resumen.personasActivas} personas",
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
                            titulo = "Altas este mes",
                            modifier = Modifier.weight(1f),
                            tipo = TipoInsignia.METRICA_ALTAS
                        )
                        Metrica(
                            icono = Icons.Filled.HourglassTop,
                            valor = resumen.porVencer.size.toString(),
                            titulo = "Vencen en ${com.example.clubdeportivo.util.CalculoResumenAdmin.DIAS_POR_VENCER} días",
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
                            titulo = "Vencidas",
                            tipo = TipoInsignia.METRICA_VENCIDAS,
                            modifier = Modifier.weight(1f)
                        )
                        Metrica(
                            icono = Icons.Filled.Pause,
                            valor = resumen.suspendidas.toString(),
                            titulo = "Suspendidas",
                            tipo = TipoInsignia.METRICA_SUSPENDIDAS,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TituloSeccion("Por vencer esta semana")
                    Spacer(modifier = Modifier.height(12.dp))
                    ListaPorVencer(resumen.porVencer)

                    if (resumen.ingresosPorMetodo.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        TituloSeccion("Cobros por método")
                        Spacer(modifier = Modifier.height(12.dp))
                        CobrosPorMetodo(resumen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TituloSeccion("Hoy en el club")
            Spacer(modifier = Modifier.height(12.dp))
            OperacionDelDia(operacion)

            Spacer(modifier = Modifier.height(24.dp))
            TituloSeccion("Próximas reservas")
            Spacer(modifier = Modifier.height(12.dp))
            val proximas = proximasReservas
            when {
                proximas == null -> TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
                proximas.isEmpty() -> TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No hay reservas próximas.",
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
private fun AvisoError() {
    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Peligro, modifier = Modifier.size(22.dp))
            Column {
                Text("No pudimos cargar los ingresos", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                Text(
                    "Revisa tu conexión y desliza hacia abajo para volver a intentarlo. " +
                        "Así evitamos mostrarte cifras incompletas.",
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
                    brush = Brush.linearGradient(colors = listOf(PrimaryNavy, ActionBlue)),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = SobreMarca.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
                    .size(104.dp)
            )
            Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Ingresos de ${Fechas.nombreMes(resumen.mes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftBlueSelection
            )
            Text(
                text = dinero(resumen.ingresosMes),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = SobreMarca,
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
                        text = "%.0f%% frente al mes pasado (%s)".format(kotlin.math.abs(variacion), dinero(resumen.ingresosMesAnterior)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (variacion >= 0) SuccessOnDark else AlertOnDark
                    )
                }
            } else {
                Text(
                    text = "Aún no hay cobros del mes pasado para comparar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SobreMarca.copy(alpha = 0.78f)
                )
            }
            Text(
                text = if (resumen.cobrosMes == 1) "1 cobro registrado" else "${resumen.cobrosMes} cobros registrados",
                style = MaterialTheme.typography.bodySmall,
                color = SobreMarca.copy(alpha = 0.78f),
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
            Text("Ingresos por mes", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
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
                    text = "Ninguna membresía vence en los próximos días.",
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
                            text = detalle.titular?.nombre ?: "Sin nombre",
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
                            hoy -> "Vence hoy"
                            else -> Fechas.legible(vence)
                        },
                        tipo = if (vence == hoy) TipoInsignia.PELIGRO else TipoInsignia.ALERTA
                    )
                }
            }
            if (lista.size > 5) {
                HorizontalDivider(color = Borde)
                Text(
                    text = "y ${lista.size - 5} más en Membresías",
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
                            Text(metodo?.etiqueta ?: "Sin especificar", style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
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
            "Reservas hoy",
            Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_RESERVAS
        )
        Metrica(
            icono = Icons.Filled.Event,
            valor = texto(operacion.pendientesDeAprobar),
            titulo = "Por aprobar",
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
            titulo = "Áreas",
            detalle = operacion.areasEnMantenimiento?.takeIf { it > 0 }?.let { "$it en mantenimiento" },
            modifier = Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_MANTENIMIENTO
        )
        Metrica(
            icono = Icons.Filled.Inventory2,
            valor = texto(operacion.articulosConStockBajo),
            titulo = "Artículos con stock bajo",
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
            "Personal",
            Modifier.weight(1f),
            tipo = TipoInsignia.METRICA_PERSONAL
        )
        Metrica(
            Icons.Filled.EmojiEvents,
            texto(operacion.torneosVigentes),
            "Torneos vigentes",
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
                    text = reserva.usuarioNombre.ifBlank { "Usuario" },
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
                texto = if (pendiente) "Pendiente" else "Confirmada",
                tipo = if (pendiente) TipoInsignia.ALERTA else TipoInsignia.EXITO
            )
        }
    }
}
