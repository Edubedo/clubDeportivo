package com.example.clubdeportivo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.clubdeportivo.R

/**
 * Única familia tipográfica de la app: Plus Jakarta Sans. Es geométrica y de trazo firme (aire deportivo), con
 * números claros y buena lectura en pantallas pequeñas. Todo el texto, incluidos códigos y cifras, usa esta.
 */
val FuenteClub = FontFamily(
    Font(R.font.fuente_regular, FontWeight.Normal),
    Font(R.font.fuente_medium, FontWeight.Medium),
    Font(R.font.fuente_semibold, FontWeight.SemiBold),
    Font(R.font.fuente_bold, FontWeight.Bold)
)

// Interlineado relativo (em): al cambiar fontSize en un Text el interlineado se ajusta solo.
private fun estilo(tamano: Int, peso: FontWeight, interlineado: Float = 1.4f, espaciado: Float = 0f) = TextStyle(
    fontFamily = FuenteClub,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = interlineado.em,
    letterSpacing = espaciado.sp
)

/** Escala tipográfica: 12 · 14 · 16 · 18 · 20 · 24 · 28. Usa estos tamaños y no inventes otros. */
val TipografiaClub = Typography(
    displayLarge = estilo(28, FontWeight.Bold, 1.25f),
    displayMedium = estilo(28, FontWeight.Bold, 1.25f),
    displaySmall = estilo(24, FontWeight.Bold, 1.25f),
    headlineLarge = estilo(28, FontWeight.Bold, 1.25f),
    headlineMedium = estilo(24, FontWeight.Bold, 1.25f),
    headlineSmall = estilo(20, FontWeight.SemiBold, 1.3f),
    titleLarge = estilo(20, FontWeight.SemiBold, 1.3f),
    titleMedium = estilo(16, FontWeight.SemiBold, 1.35f),
    titleSmall = estilo(14, FontWeight.SemiBold, 1.35f),
    bodyLarge = estilo(16, FontWeight.Normal),
    bodyMedium = estilo(14, FontWeight.Normal),
    bodySmall = estilo(12, FontWeight.Normal),
    labelLarge = estilo(14, FontWeight.SemiBold, 1.3f),
    labelMedium = estilo(12, FontWeight.Medium, 1.3f),
    labelSmall = estilo(12, FontWeight.Medium, 1.3f)
)
