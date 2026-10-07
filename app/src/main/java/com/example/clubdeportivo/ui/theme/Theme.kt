package com.example.clubdeportivo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Una sola marca: primary, secondary y tertiary son el mismo azul para que ningún componente de Material
// "invente" otro color. Los contenedores usan la versión suave y los fondos son grises neutros.
private val EsquemaClub = lightColorScheme(
    primary = Marca,
    onPrimary = SobreMarca,
    primaryContainer = MarcaSuave,
    onPrimaryContainer = Marca,
    secondary = Marca,
    onSecondary = SobreMarca,
    secondaryContainer = MarcaSuave,
    onSecondaryContainer = Marca,
    tertiary = Marca,
    onTertiary = SobreMarca,
    tertiaryContainer = MarcaSuave,
    onTertiaryContainer = Marca,
    error = Peligro,
    onError = SobreMarca,
    errorContainer = PeligroSuave,
    onErrorContainer = Peligro,
    background = FondoApp,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = FondoApp,
    onSurfaceVariant = TextoSecundario,
    surfaceTint = Superficie,
    outline = BordeCampo,
    outlineVariant = Borde,
    // Los avisos emergentes (Snackbar) usan inverseSurface: van en el naranja de la marca, no en negro.
    inverseSurface = Marca,
    inverseOnSurface = SobreMarca,
    inversePrimary = MarcaSuave,
    scrim = TextoPrincipal,
    surfaceBright = Superficie,
    surfaceDim = FondoApp,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = FondoApp
)

private val FormasClub = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Tema único de la app. Es solo claro a propósito: toda la interfaz (tarjetas blancas sobre fondo gris) está
 * pensada para una sola apariencia y así se ve igual en cualquier teléfono.
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
