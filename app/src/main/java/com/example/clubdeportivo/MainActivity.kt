package com.example.clubdeportivo

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.ui.ClubDeportivoApp
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme
import com.example.clubdeportivo.ui.theme.PreferenciaTema

/** Única Activity de la app: todo el contenido y la navegación viven en Compose. */
class MainActivity : ComponentActivity() {

    /** Código que trae el enlace de acceso con el que se abrió la app (clubdeportivo://registro?codigo=...). */
    private var codigoEntrante by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        // Barras transparentes; el color de sus íconos (claros u oscuros) lo ajusta ClubDeportivoApp según el tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        codigoEntrante = ReglasMembresia.codigoDeEnlace(intent?.dataString)

        setContent {
            ClubDeportivoTheme {
                ClubDeportivoApp(codigoEntrante = codigoEntrante, onCodigoUsado = { codigoEntrante = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReglasMembresia.codigoDeEnlace(intent.dataString)?.let { codigoEntrante = it }
    }
}
