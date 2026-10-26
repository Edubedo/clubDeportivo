package com.example.clubdeportivo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.clubdeportivo.util.tr
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoSecundario

/*
 * Pantallas de carga: en vez de un círculo girando sobre un fondo vacío, se dibuja la forma de lo que va a aparecer
 * (tarjetas "esqueleto" con un brillo que las recorre) y, para esperas largas, el logo del club animado.
 */

/** Pincel con un brillo que recorre el bloque de izquierda a derecha; se mueve igual en todos los bloques. */
@Composable
private fun pincelDeBrillo(): Brush {
    val transicion = rememberInfiniteTransition(label = "brillo")
    val desplazamiento by transicion.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "brilloDesplazamiento"
    )
    val base = TextoSecundario.copy(alpha = 0.16f)
    val brillo = TextoSecundario.copy(alpha = 0.05f)
    return Brush.linearGradient(
        colors = listOf(base, brillo, base),
        start = Offset(desplazamiento, 0f),
        end = Offset(desplazamiento + 320f, 120f)
    )
}

/** Un rectángulo gris con brillo: representa un texto, una imagen o un botón que todavía no cargó. */
@Composable
fun BloqueEsqueleto(modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(forma).background(pincelDeBrillo()))
}

/** Tarjeta de lista en "esqueleto": avatar, dos líneas de texto y una etiqueta. */
@Composable
fun TarjetaEsqueleto(modifier: Modifier = Modifier) {
    TarjetaClub(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BloqueEsqueleto(Modifier.size(48.dp), CircleShape)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BloqueEsqueleto(Modifier.fillMaxWidth(0.62f).height(15.dp))
                BloqueEsqueleto(Modifier.fillMaxWidth(0.4f).height(11.dp))
            }
            BloqueEsqueleto(Modifier.width(62.dp).height(26.dp), RoundedCornerShape(50))
        }
    }
}

/** Varias tarjetas "esqueleto" una debajo de otra, para listas que están cargando. */
@Composable
fun EsqueletoLista(modifier: Modifier = Modifier, cantidad: Int = 4) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(cantidad) { TarjetaEsqueleto() }
    }
}

/** Esqueleto del dashboard del administrador: tarjeta grande de ingresos, gráfica y dos indicadores. */
@Composable
fun EsqueletoDashboard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BloqueEsqueleto(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(20.dp))
        BloqueEsqueleto(Modifier.fillMaxWidth().height(190.dp), RoundedCornerShape(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BloqueEsqueleto(Modifier.weight(1f).height(96.dp), RoundedCornerShape(16.dp))
            BloqueEsqueleto(Modifier.weight(1f).height(96.dp), RoundedCornerShape(16.dp))
        }
    }
}

/**
 * El logo del club (balón sobre un cuadrado redondeado) con un anillo que gira y un latido suave.
 * [sobreOscuro] lo adapta a fondos azul oscuro como el del login.
 */
@Composable
fun CargandoMarca(modifier: Modifier = Modifier, tamano: Dp = 64.dp, sobreOscuro: Boolean = false) {
    val transicion = rememberInfiniteTransition(label = "marca")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "marcaGiro"
    )
    val latido by transicion.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "marcaLatido"
    )
    val colorPista = if (sobreOscuro) Color.White.copy(alpha = 0.14f) else TextoSecundario.copy(alpha = 0.16f)
    val colorArco = if (sobreOscuro) Color.White else Marca
    val colorLogoFondo = if (sobreOscuro) Color.White.copy(alpha = 0.14f) else Marca
    val colorLogo = if (sobreOscuro) Color.White else SobreMarca

    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trazo = Stroke(width = size.minDimension * 0.06f, cap = StrokeCap.Round)
            drawArc(color = colorPista, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = trazo)
            drawArc(color = colorArco, startAngle = giro, sweepAngle = 95f, useCenter = false, style = trazo)
        }
        Box(
            modifier = Modifier
                .size(tamano * 0.6f)
                .scale(latido)
                .clip(RoundedCornerShape(tamano * 0.15f))
                .background(colorLogoFondo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = colorLogo, modifier = Modifier.size(tamano * 0.36f))
        }
    }
}

/**
 * Pantalla de carga estándar (la usa [FullScreenLoading] en todas las pantallas): un encabezado con el logo animado y
 * "Cargando…", y debajo la forma de la lista que viene. [conMargen] agrega el margen horizontal de las pantallas
 * cuando quien la usa no lo trae ya.
 */
@Composable
fun PantallaCarga(modifier: Modifier = Modifier, conMargen: Boolean = false) {
    val descripcion = tr("cargando")
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (conMargen) MargenPantalla else 0.dp)
            .semantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CargandoMarca(tamano = 40.dp)
            Text(text = descripcion, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }
        Spacer(modifier = Modifier.height(2.dp))
        EsqueletoLista(cantidad = 4)
    }
}
