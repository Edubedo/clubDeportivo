package com.example.clubdeportivo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
 * Los textos de la app salen de los archivos JSON de [Textos] según este idioma.
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
 * Textos traducidos de toda la app. Viven en `assets/i18n/<codigo>.json` (un archivo por [Idioma], clave → texto) y
 * se cargan una vez por proceso con [LectorJson]. Se usan igual desde pantallas, ViewModels y repositorios: [tr] o
 * [Textos.get] con la clave.
 *
 * Si a un idioma le falta una clave se usa el texto en español, y si tampoco está ahí, la clave misma (así se nota en
 * pantalla en vez de cerrar la app).
 */
object Textos {
    private const val CARPETA = "i18n"

    private var aplicacion: Context? = null
    private val cargados = ConcurrentHashMap<Idioma, Map<String, String>>()

    fun iniciar(app: Context) {
        aplicacion = app.applicationContext
    }

    /** Solo para pruebas unitarias (JVM), donde no existen los assets de Android: devuelve el texto de [clave] sin ellos. */
    var resolutorDePrueba: ((String, Array<out Any>) -> String)? = null

    fun get(clave: String, vararg argumentos: Any): String {
        resolutorDePrueba?.let { return it(clave, argumentos) }
        val texto = textosDe(PreferenciaIdioma.actual)[clave] ?: textosDe(Idioma.ES)[clave] ?: clave
        // Sin argumentos no se formatea: así un texto con "%" (como "%.0f%%") llega tal cual a quien lo formatea después.
        return if (argumentos.isEmpty()) texto else String.format(Locale.forLanguageTag(PreferenciaIdioma.actual.codigo), texto, *argumentos)
    }

    /**
     * Texto de [clave] en [idioma], sin importar el idioma elegido. Sirve para guardar datos siempre en español y
     * para reconocer un dato guardado en cualquier idioma.
     */
    fun en(idioma: Idioma, clave: String): String {
        if (idioma == Idioma.ES) resolutorDePrueba?.let { return it(clave, emptyArray()) }
        if (resolutorDePrueba != null) return clave
        return textosDe(idioma)[clave] ?: textosDe(Idioma.ES)[clave] ?: clave
    }

    private fun textosDe(idioma: Idioma): Map<String, String> = cargados.getOrPut(idioma) {
        val app = checkNotNull(aplicacion) { "Textos.iniciar() no se llamó en ClubApplication." }
        app.assets.open("$CARPETA/${idioma.codigo}.json").bufferedReader(Charsets.UTF_8).use { LectorJson.leerTextos(it.readText()) }
    }
}

/** Texto traducido de [clave] en el idioma elegido; los [argumentos] reemplazan los `%1$s`, `%2$d`, etc. del texto. */
fun tr(clave: String, vararg argumentos: Any): String = Textos.get(clave, *argumentos)
