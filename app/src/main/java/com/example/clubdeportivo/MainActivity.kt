package com.example.clubdeportivo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.clubdeportivo.ui.ClubDeportivoApp
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme

/** Única Activity de la app: todo el contenido y la navegación viven en Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClubDeportivoTheme {
                ClubDeportivoApp()
            }
        }
    }
}
