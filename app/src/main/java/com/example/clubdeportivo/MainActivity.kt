package com.example.clubdeportivo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.clubdeportivo.ui.ClubDeportivoApp
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme

/** Única Activity de la app: todo el contenido y la navegación viven en Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app tiene una sola apariencia (clara): los íconos de las barras del sistema siempre van oscuros,
        // aunque el teléfono esté en modo oscuro; si no, quedarían invisibles sobre la barra blanca.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            ClubDeportivoTheme {
                ClubDeportivoApp()
            }
        }
    }
}
