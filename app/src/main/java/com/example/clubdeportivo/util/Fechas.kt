package com.example.clubdeportivo.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ayuda a comparar fechas/horas de texto ("yyyy-MM-dd", "HH:mm") sin depender de java.time (minSdk 24). */
object Fechas {
    private val NOMBRES_DIA = arrayOf("dom.", "lun.", "mar.", "mié.", "jue.", "vie.", "sáb.")
    private val NOMBRES_MES = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val MESES_LARGOS = arrayOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private const val PATRON_FECHA_HORA = "yyyy-MM-dd HH:mm"
    private const val PATRON_FECHA = "yyyy-MM-dd"

    private fun formatoFechaHora() = SimpleDateFormat(PATRON_FECHA_HORA, Locale.getDefault())
    private fun formatoFecha() = SimpleDateFormat(PATRON_FECHA, Locale.getDefault())

    fun combinar(fecha: String, hora: String): Date =
        formatoFechaHora().parse("$fecha $hora") ?: Date()

    /** Horas entre ahora y el momento indicado. Positivo = en el futuro. */
    fun horasDesdeAhora(fecha: String, hora: String): Double =
        (combinar(fecha, hora).time - System.currentTimeMillis()) / 3_600_000.0

    fun hoy(): String = formatoFecha().format(Date())

    /** [fecha] más [meses] meses (31 de enero + 1 mes = 28 o 29 de febrero). */
    fun sumarMeses(fecha: String, meses: Int): String {
        val base = runCatching { formatoFecha().parse(fecha) }.getOrNull() ?: return fecha
        val cal = Calendar.getInstance().apply { time = base; add(Calendar.MONTH, meses) }
        return formatoFecha().format(cal.time)
    }

    /** Todas las fechas "yyyy-MM-dd" de [inicio] a [fin], ambas incluidas. Vacío si el rango es inválido. */
    fun diasEntre(inicio: String, fin: String): List<String> {
        val formato = formatoFecha()
        val desde = runCatching { formato.parse(inicio) }.getOrNull() ?: return emptyList()
        val hasta = runCatching { formato.parse(fin) }.getOrNull() ?: return emptyList()
        val cal = Calendar.getInstance().apply { time = desde }
        val dias = mutableListOf<String>()
        while (!cal.time.after(hasta) && dias.size < 400) {
            dias.add(formato.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dias
    }

    /**
     * Fecha para mostrar a las personas: "Hoy", "Mañana" o "mié. 7 oct" (con el año si no es el actual).
     * Si [fecha] no tiene el formato "yyyy-MM-dd" se devuelve tal cual.
     */
    fun legible(fecha: String): String {
        if (fecha == hoy()) return "Hoy"
        if (fecha == sumarDias(1)) return "Mañana"
        val base = runCatching { formatoFecha().parse(fecha) }.getOrNull() ?: return fecha
        val cal = Calendar.getInstance().apply { time = base }
        val dia = NOMBRES_DIA[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val mes = NOMBRES_MES[cal.get(Calendar.MONTH)]
        val anio = cal.get(Calendar.YEAR)
        val sufijo = if (anio == Calendar.getInstance().get(Calendar.YEAR)) "" else " $anio"
        return "$dia ${cal.get(Calendar.DAY_OF_MONTH)} $mes$sufijo"
    }

    /** "2026-10" -> "octubre" (o "oct" si [corto]). Devuelve [mes] tal cual si no tiene ese formato. */
    fun nombreMes(mes: String, corto: Boolean = false): String {
        val numero = mes.substringAfter("-", "").take(2).toIntOrNull() ?: return mes
        if (numero !in 1..12) return mes
        return if (corto) NOMBRES_MES[numero - 1] else MESES_LARGOS[numero - 1]
    }

    fun sumarDias(dias: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dias)
        return formatoFecha().format(cal.time)
    }

    /** Próximos [cantidad] días (incluyendo hoy) como pares fecha/etiqueta legible, ej. "Hoy", "Mañana", "vie. 12 sep". */
    fun proximosDias(cantidad: Int): List<Pair<String, String>> {
        val nombresDia = NOMBRES_DIA
        val nombresMes = NOMBRES_MES
        val cal = Calendar.getInstance()
        return (0 until cantidad).map { offset ->
            val diaCal = cal.clone() as Calendar
            diaCal.add(Calendar.DAY_OF_YEAR, offset)
            val fecha = formatoFecha().format(diaCal.time)
            val etiqueta = when (offset) {
                0 -> "Hoy"
                1 -> "Mañana"
                else -> {
                    val diaSemana = nombresDia[diaCal.get(Calendar.DAY_OF_WEEK) - 1]
                    val dia = diaCal.get(Calendar.DAY_OF_MONTH)
                    val mes = nombresMes[diaCal.get(Calendar.MONTH)]
                    "$diaSemana $dia $mes"
                }
            }
            fecha to etiqueta
        }
    }
}
