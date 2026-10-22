package com.example.clubdeportivo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaClub = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = SobreMarca,
    primaryContainer = SoftBlueSelection,
    onPrimaryContainer = DeepNavy,
    secondary = SlateAccent,
    onSecondary = DeepNavy,
    secondaryContainer = SoftBlueSelection,
    onSecondaryContainer = DeepNavy,
    tertiary = CourtGreen,
    onTertiary = DeepNavy,
    tertiaryContainer = SuccessContainer,
    onTertiaryContainer = SuccessForeground,
    error = AlertRed,
    onError = SobreMarca,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRedForeground,
    background = Background,
    onBackground = DeepNavy,
    surface = Surface,
    onSurface = DeepNavy,
    surfaceVariant = Background,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Surface,
    outline = SlateAccent,
    outlineVariant = BorderSubdued,
    inverseSurface = DeepNavy,
    inverseOnSurface = SobreMarca,
    inversePrimary = SoftBlueSelection,
    scrim = DeepNavy,
    surfaceBright = Surface,
    surfaceDim = Background,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Background
)

private val FormasClub = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Tema único de la app. Es solo claro a propósito: toda la interfaz (tarjetas blancas sobre fondo azul muy
 * claro) está pensada para una sola apariencia y así se ve igual en cualquier teléfono.
 */
@Composable
fun ClubDeportivoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClub,
        typography = TipografiaClub,
        shapes = FormasClub,
        content = content
    )
}