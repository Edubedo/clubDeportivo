package com.example.clubdeportivo.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubdeportivo.util.Idioma
import com.example.clubdeportivo.util.PreferenciaIdioma

private tailrec fun Context.comoActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.comoActividad()
    else -> null
}

/** Selector de idioma con bandera y nombre (🇪🇸 Español / 🇬🇧 English), para Perfil. Al elegir, la app se redibuja en ese idioma. */
@Composable
fun SelectorIdioma(modifier: Modifier = Modifier) {
    val actividad = LocalContext.current.comoActividad()
    PestanasPildora(
        opciones = Idioma.entries.map { "${it.bandera}  ${it.nombre}" },
        seleccionada = PreferenciaIdioma.actual.ordinal,
        onSeleccion = { indice -> actividad?.let { PreferenciaIdioma.elegir(it, Idioma.entries[indice]) } },
        modifier = modifier,
        margenHorizontal = 0.dp
    )
}

/** Solo las banderas, en pequeño, para pantallas sobre fondo oscuro como el login. */
@Composable
fun BanderasIdioma(modifier: Modifier = Modifier) {
    val actividad = LocalContext.current.comoActividad()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Idioma.entries.forEach { idioma ->
            val activo = idioma == PreferenciaIdioma.actual
            Surface(
                onClick = { actividad?.let { PreferenciaIdioma.elegir(it, idioma) } },
                modifier = Modifier.semantics { contentDescription = idioma.nombre },
                shape = CircleShape,
                color = if (activo) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (activo) 0.9f else 0.3f))
            ) {
                Text(
                    text = idioma.bandera,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
