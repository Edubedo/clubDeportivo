package com.example.clubdeportivo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/** Idiomas de la app. [bandera] es el emoji de la bandera que se muestra en el selector. */
enum class Idioma(val codigo: String, val bandera: String, val nombre: String) {
    ES("es", "🇪🇸", "Español"),
    EN("en", "🇬🇧", "English");

    companion object {
        fun deCodigo(codigo: String?): Idioma? = entries.firstOrNull { it.codigo == codigo }

        /** Si la persona nunca eligió, se usa el idioma del teléfono cuando es inglés; en cualquier otro caso, español. */
        fun delSistema(): Idioma = if (Locale.getDefault().language == EN.codigo) EN else ES
    }
}

/**
 * Idioma elegido por la persona, guardado en el teléfono. Se aplica envolviendo el contexto de la Activity
 * ([envolver], desde `attachBaseContext`); cambiarlo reinicia la app para que todo salga en el idioma nuevo.
 *
 * Los textos que arman los ViewModels y repositorios (que no tienen un Context) se piden con [Textos].
 */
object PreferenciaIdioma {
    private const val ARCHIVO = "preferencias_club"
    private const val CLAVE_IDIOMA = "idioma"

    @Volatile
    var actual: Idioma = Idioma.ES
        private set

    private fun leer(contexto: Context): Idioma =
        runCatching {
            Idioma.deCodigo(contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE_IDIOMA, null))
        }.getOrNull() ?: Idioma.delSistema()

    /** Lee el idioma guardado; se llama al arrancar la app para que [Textos] ya hable en él. */
    fun cargar(contexto: Context) {
        actual = leer(contexto)
    }

    /** Devuelve [base] con el idioma elegido; úsalo en `attachBaseContext` de la Activity. */
    fun envolver(base: Context): Context {
        cargar(base)
        return conIdioma(base, actual)
    }

    /**
     * Guarda el idioma y reinicia la app para aplicarlo. No basta con recrear la pantalla: catálogos, etiquetas y otros
     * textos se arman una sola vez por proceso, así que se reinicia para que todo salga en el idioma nuevo. La sesión
     * no se pierde: Firebase la recuerda y la app la restaura al abrir.
     */
    fun elegir(actividad: Activity, nuevo: Idioma) {
        if (nuevo == actual) return
        actual = nuevo
        runCatching {
            actividad.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE_IDIOMA, nuevo.codigo).commit()
        }
        Textos.idiomaCambiado()
        val reinicio = Intent(actividad, actividad.javaClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        actividad.startActivity(reinicio)
        actividad.finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    internal fun conIdioma(base: Context, idioma: Idioma): Context {
        val locale = Locale.forLanguageTag(idioma.codigo)
        Locale.setDefault(locale)
        val configuracion = Configuration(base.resources.configuration)
        configuracion.setLocale(locale)
        return base.createConfigurationContext(configuracion)
    }
}

/**
 * Acceso a los textos traducidos desde código que no vive en una pantalla (ViewModels, repositorios).
 * En una pantalla de Compose se usa `stringResource` directamente.
 */
object Textos {
    private var aplicacion: Context? = null
    private var contexto: Context? = null

    fun iniciar(app: Context) {
        aplicacion = app.applicationContext
        contexto = null
    }

    internal fun idiomaCambiado() {
        contexto = null
    }

    /** Solo para pruebas unitarias (JVM), donde no existen los recursos de Android: devuelve el texto de [id] sin ellos. */
    var resolutorDePrueba: ((Int, Array<out Any>) -> String)? = null

    fun get(@StringRes id: Int, vararg argumentos: Any): String {
        resolutorDePrueba?.let { return it(id, argumentos) }
        val app = checkNotNull(aplicacion) { "Textos.iniciar() no se llamó en ClubApplication." }
        val localizado = contexto ?: PreferenciaIdioma.conIdioma(app, PreferenciaIdioma.actual).also { contexto = it }
        // Sin argumentos no se formatea: así un texto con "%" (como "%.0f%%") llega tal cual a quien lo formatea después.
        return if (argumentos.isEmpty()) localizado.getString(id) else localizado.getString(id, *argumentos)
    }
}
