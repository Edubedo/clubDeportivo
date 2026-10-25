package com.example.clubdeportivo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF052659),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC1E8FF),
    onPrimaryContainer = Color(0xFF021024),
    secondary = Color(0xFF5483B3),
    onSecondary = Color(0xFF021024),
    secondaryContainer = Color(0xFFC1E8FF),
    onSecondaryContainer = Color(0xFF021024),
    tertiary = Color(0xFF16A34A),
    onTertiary = Color(0xFF021024),
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF15803D),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF021024),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF021024),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Color(0xFFFFFFFF),
    outline = Color(0xFF5483B3),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF021024),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFC1E8FF),
    scrim = Color(0xFF021024),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF8FAFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF8FAFC)
)

/** Mismo azul de marca sobre fondos azul marino casi negro: las tarjetas se separan del fondo por tono y borde fino. */
private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF8DB6F5),
    onPrimary = Color(0xFF06142B),
    primaryContainer = Color(0xFF1B3358),
    onPrimaryContainer = Color(0xFFDCE8FA),
    secondary = Color(0xFF9DB9DC),
    onSecondary = Color(0xFF06142B),
    secondaryContainer = Color(0xFF1B3358),
    onSecondaryContainer = Color(0xFFDCE8FA),
    tertiary = Color(0xFF6EE7A0),
    onTertiary = Color(0xFF06231A),
    tertiaryContainer = Color(0xFF0F2E1E),
    onTertiaryContainer = Color(0xFF6EE7A0),
    error = Color(0xFFF87171),
    onError = Color(0xFF3A0A0A),
    errorContainer = Color(0xFF3A1519),
    onErrorContainer = Color(0xFFFCA5A5),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE8EEF7),
    surface = Color(0xFF121C2E),
    onSurface = Color(0xFFE8EEF7),
    surfaceVariant = Color(0xFF1A2740),
    onSurfaceVariant = Color(0xFFA9B8CC),
    surfaceTint = Color(0xFF121C2E),
    outline = Color(0xFF4A6A94),
    outlineVariant = Color(0xFF263449),
    inverseSurface = Color(0xFFE8EEF7),
    inverseOnSurface = Color(0xFF0B1220),
    inversePrimary = Color(0xFF052659),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF1C2B45),
    surfaceDim = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF0B1220),
    surfaceContainerLow = Color(0xFF0F1829),
    surfaceContainer = Color(0xFF121C2E),
    surfaceContainerHigh = Color(0xFF17243A),
    surfaceContainerHighest = Color(0xFF1C2B45)
)

private val FormasClub = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Tema de la app, en claro y en oscuro. Sigue al teléfono salvo que la persona haya elegido una apariencia en su
 * perfil ([PreferenciaTema]). Toda la interfaz toma sus colores de [LocalModoOscuro] (ver Color.kt).
 */
@Composable
fun ClubDeportivoTheme(modo: ModoTema = PreferenciaTema.modo, content: @Composable () -> Unit) {
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    CompositionLocalProvider(LocalModoOscuro provides oscuro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = TipografiaClub,
            shapes = FormasClub,
            content = content
        )
    }
}
