package com.example.clubdeportivo

import android.graphics.Color
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clubdeportivo.notificaciones.Notificador
import com.example.clubdeportivo.util.PreferenciaIdioma
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.ui.ArranqueApp
import com.example.clubdeportivo.ui.ClubDeportivoApp
import com.example.clubdeportivo.ui.theme.ClubDeportivoTheme
import com.example.clubdeportivo.ui.theme.PreferenciaTema

/** Única Activity de la app: todo el contenido y la navegación viven en Compose. */
class MainActivity : ComponentActivity() {

    /** Código que trae el enlace de acceso con el que se abrió la app (clubdeportivo://registro?codigo=...). */
    private var codigoEntrante by mutableStateOf<String?>(null)

    /** Pantalla que pidió abrir la notificación que se tocó (ver [Notificador.EXTRA_RUTA]). */
    private var rutaEntrante by mutableStateOf<String?>(null)

    /** Aplica el idioma elegido en Perfil → Idioma (español o inglés) a todo lo que dibuja esta Activity. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(PreferenciaIdioma.envolver(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de arranque de marca: se queda hasta revisar la sesión guardada (con un tope por si la red tarda).
        val arranque = installSplashScreen()
        val inicio = SystemClock.elapsedRealtime()
        arranque.setKeepOnScreenCondition { !ArranqueApp.sesionRevisada && SystemClock.elapsedRealtime() - inicio < 3000 }
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        // Barras transparentes; el color de sus íconos (claros u oscuros) lo ajusta ClubDeportivoApp según el tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        codigoEntrante = ReglasMembresia.codigoDeEnlace(intent?.dataString)
        rutaEntrante = intent?.getStringExtra(Notificador.EXTRA_RUTA)

        setContent {
            ClubDeportivoTheme {
                ClubDeportivoApp(
                    codigoEntrante = codigoEntrante,
                    onCodigoUsado = { codigoEntrante = null },
                    rutaEntrante = rutaEntrante,
                    onRutaUsada = { rutaEntrante = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReglasMembresia.codigoDeEnlace(intent.dataString)?.let { codigoEntrante = it }
        intent.getStringExtra(Notificador.EXTRA_RUTA)?.let { rutaEntrante = it }
    }
}
