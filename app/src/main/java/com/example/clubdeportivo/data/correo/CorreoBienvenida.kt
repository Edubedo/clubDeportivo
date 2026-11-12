package com.example.clubdeportivo.data.correo

import android.util.Log
import com.example.clubdeportivo.BuildConfig
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.util.ReglasMembresia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Correo de bienvenida de un miembro nuevo: su código de acceso y un enlace que abre la app (ver docs/correo-de-bienvenida.md).
 *
 * Se manda con la API de Resend directo desde la app. La key (`resend.apiKey` en local.properties) es una key de SOLO ENVÍO,
 * pero va dentro del APK: quien lo desarme puede sacarla y mandar correos con el dominio del club. Si eso es un problema,
 * hay que mover el envío a un servidor (Cloud Function o similar).
 */
object CorreoBienvenida {

    private const val ENDPOINT = "https://api.resend.com/emails"
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Lo manda en segundo plano y sin molestar: si falla (sin red, sin correo) el alta ya quedó hecha y solo se anota en el log. */
    fun enviarEnSegundoPlano(personas: List<MiembroClub>) {
        personas.filter { it.correo.isNotBlank() && ReglasMembresia.esCodigo(it.codigo) }.forEach { persona ->
            alcance.launch {
                val ok = runCatching { enviar(persona.nombre, persona.correo.trim(), persona.codigo) }.getOrElse {
                    Log.w("CorreoBienvenida", "No se pudo enviar la bienvenida de ${persona.codigo}: ${it.message}")
                    false
                }
                if (!ok) Log.w("CorreoBienvenida", "Resend no aceptó la bienvenida de ${persona.codigo}")
            }
        }
    }

    private fun enviar(nombre: String, correo: String, codigo: String): Boolean {
        if (BuildConfig.RESEND_API_KEY.isBlank()) return false
        val cuerpo = cuerpoJson(BuildConfig.RESEND_FROM, nombre, correo, codigo, BuildConfig.ENLACE_BIENVENIDA)
        val conexion = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${BuildConfig.RESEND_API_KEY}")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            conexion.responseCode in 200..299
        } finally {
            conexion.disconnect()
        }
    }

    /** El enlace https del correo: la página de hosting lo redirige a la app (o la app lo abre directo si ya está instalada). */
    internal fun enlace(base: String, codigo: String) = "$base?codigo=${URLEncoder.encode(codigo, "UTF-8")}"

    /** Cuerpo JSON de la petición a Resend. Pública solo dentro del módulo para poder probarla sin red. */
    internal fun cuerpoJson(de: String, nombre: String, correo: String, codigo: String, baseEnlace: String): String {
        val enlace = enlace(baseEnlace, codigo)
        val primer = nombre.trim().split(Regex("\\s+")).first()
        val saludo = if (primer.isNotEmpty()) "Hola $primer" else "Hola"
        val asunto = "Tu código de acceso a Athletic Club: $codigo"
        val texto = "$saludo,\n\nTe damos la bienvenida a Athletic Club. Ya eres miembro.\n\n" +
            "Tu código de acceso: $codigo\n\n" +
            "Para crear tu cuenta abre este enlace desde tu teléfono Android (con la app instalada):\n$enlace\n\n" +
            "Si el enlace no abre la app, entra a \"Regístrate con tu código\" y escribe el código a mano.\n" +
            "El código sirve una sola vez, para crear tu cuenta. Después entras con tu correo y contraseña.\n\n" +
            "Si no esperabas este correo, ignóralo."
        val html = """<div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#2b2622">""" +
            """<h1 style="color:#e8590c;font-size:22px">Athletic Club</h1>""" +
            """<p>${html(saludo)}, te damos la bienvenida. Ya eres miembro.</p>""" +
            """<p style="margin:0;color:#6b625a;font-size:14px">Tu código de acceso</p>""" +
            """<p style="font:bold 28px Menlo,Consolas,monospace;letter-spacing:3px;margin:4px 0 20px">${html(codigo)}</p>""" +
            """<p><a href="${html(enlace)}" style="background:#e8590c;color:#fff;text-decoration:none;font-weight:bold;padding:12px 22px;border-radius:8px;display:inline-block">Crear mi cuenta</a></p>""" +
            """<p style="color:#6b625a;font-size:13px">Ábrelo desde tu teléfono Android con la app instalada. Si no abre la app, entra a <b>Regístrate con tu código</b> y escribe el código a mano. El código sirve una sola vez.</p>""" +
            """<p style="color:#9a9188;font-size:12px">Si el botón no funciona, copia este enlace: ${html(enlace)}<br>Si no esperabas este correo, ignóralo.</p></div>"""
        return """{"from":${json(de)},"to":[${json(correo)}],"subject":${json(asunto)},"text":${json(texto)},"html":${json(html)}}"""
    }

    private fun html(texto: String) = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun json(texto: String): String = buildString {
        append('"')
        texto.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }
}
