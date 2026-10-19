package com.example.clubdeportivo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Base azul (Azul Profundo): primary y tertiary son la marca. El elemento seleccionado (barra de navegación,
// chips) usa Azul Cielo con texto azul noche. El Verde Azulado (Caledula) no está en el esquema a propósito:
// se usa a mano como acento pequeño (ver Color.kt) para que ningún componente lo convierta en dominante.
private val EsquemaClub = lightColorScheme(
    primary = Marca,
    onPrimary = SobreMarca,
    primaryContainer = MarcaSuave,
    onPrimaryContainer = MarcaOscura,
    secondary = Marca,
    onSecondary = SobreMarca,
    secondaryContainer = Trigo,
    onSecondaryContainer = MarcaOscura,
    tertiary = Caledula,
    onTertiary = MarcaOscura,
    tertiaryContainer = CaledulaSuave,
    onTertiaryContainer = Citrino,
    error = Peligro,
    onError = SobreMarca,
    errorContainer = PeligroSuave,
    onErrorContainer = Peligro,
    background = FondoApp,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = Perla,
    onSurfaceVariant = TextoSecundario,
    surfaceTint = Superficie,
    outline = BordeCampo,
    outlineVariant = Borde,
    // Los avisos emergentes (Snackbar) usan inverseSurface: van en el azul noche de la marca, no en negro.
    inverseSurface = MarcaOscura,
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