package com.example.clubdeportivo.data.model

import com.example.clubdeportivo.util.Idioma
import com.example.clubdeportivo.util.PreferenciaIdioma
import com.example.clubdeportivo.util.Textos
import java.text.Normalizer

/*
 * Algunos datos se guardan en Firestore con su texto en español ("Titular", "Matutino", "Fútbol", "Cancha de fútbol 1"...)
 * y así se comparan, por eso no se traducen al guardarlos. Estas funciones solo cambian cómo se MUESTRAN; lo que no
 * conocen (por ejemplo un parentesco escrito a mano, como "Cónyuge") se devuelve tal cual.
 */

fun etiquetaParentesco(valor: String): String = when (valor) {
    MiembroClub.PARENTESCO_TITULAR -> Textos.get("parentesco_titular")
    "Integrante" -> Textos.get("parentesco_integrante")
    else -> valor
}

fun etiquetaTurno(valor: String): String = when (valor) {
    "Matutino" -> Textos.get("turno_matutino")
    "Vespertino" -> Textos.get("turno_vespertino")
    else -> valor
}

fun etiquetaTipoPersonal(valor: String): String = when (valor) {
    "Instructor" -> Textos.get("tipo_personal_instructor")
    "Limpieza" -> Textos.get("tipo_personal_limpieza")
    "Ayudante de área" -> Textos.get("tipo_personal_ayudante")
    "Administrador de área" -> Textos.get("tipo_personal_administrador")
    "Administrador" -> Textos.get("rol_admin")
    else -> valor
}

/** "fútbol", "Futbol" y "FÚTBOL" se comparan igual. */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private val clavesDeporte = mapOf(
    "basquetbol" to "deporte_basquetbol",
    "baloncesto" to "deporte_basquetbol",
    "tenis" to "deporte_tenis",
    "futbol" to "deporte_futbol",
    "natacion" to "deporte_natacion"
)

/** Deporte de un área, reserva, artículo o persona del personal ("Fútbol" → "Soccer"). */
fun etiquetaDeporte(valor: String): String =
    clavesDeporte[normalizar(valor)]?.let { Textos.get(it) } ?: valor

private val cancha = Regex("""^cancha(?:\s+de)?\s+(\p{L}+)(.*)$""", RegexOption.IGNORE_CASE)
private val canchaSola = Regex("""^cancha(\s.*)?$""", RegexOption.IGNORE_CASE)
private val alberca = Regex("""^(?:alberca|piscina)(\s.*)?$""", RegexOption.IGNORE_CASE)

/**
 * Nombre de un área tal como lo escribió el administrador ("Cancha de fútbol 1", "Alberca 2"). Se traducen los
 * nombres que siguen esos patrones; un nombre libre ("Salón de usos múltiples") se muestra tal cual.
 */
fun etiquetaArea(nombre: String): String {
    if (PreferenciaIdioma.actual == Idioma.ES) return nombre
    val limpio = nombre.trim()
    cancha.matchEntire(limpio)?.let { m ->
        // Un deporte que no está en el catálogo ("handball") se deja como lo escribieron, con mayúscula inicial.
        val deporte = clavesDeporte[normalizar(m.groupValues[1])]?.let { Textos.get(it) }
            ?: m.groupValues[1].replaceFirstChar { it.uppercase() }
        return Textos.get("area_cancha_de", deporte, m.groupValues[2])
    }
    canchaSola.matchEntire(limpio)?.let { return Textos.get("area_cancha", it.groupValues[1]) }
    alberca.matchEntire(limpio)?.let { return Textos.get("area_alberca", it.groupValues[1]) }
    return nombre
}

/**
 * Motivo de suspensión guardado en la membresía. Se reconoce en cualquier idioma (los guardados antes de este cambio
 * pueden estar en inglés) y se muestra en el idioma elegido; "Otro: <detalle>" traduce solo la palabra "Otro".
 */
fun etiquetaMotivoSuspension(guardado: String): String {
    val texto = guardado.trim()
    if (texto.isEmpty()) return guardado
    val claves = MotivoSuspension.entries.filterNot { it.pideDetalle }.map { it.clave } + MotivoSuspension.CLAVE_BAJA_MIEMBRO
    claves.firstOrNull { clave -> Idioma.entries.any { Textos.en(it, clave).equals(texto, ignoreCase = true) } }
        ?.let { return Textos.get(it) }
    val otro = MotivoSuspension.OTRO
    Idioma.entries.forEach { idioma ->
        val prefijo = otro.prefijoGuardado(Textos.en(idioma, otro.clave))
        if (texto.startsWith(prefijo, ignoreCase = true)) return otro.prefijoGuardado(otro.etiqueta) + texto.substring(prefijo.length)
    }
    return guardado
}
