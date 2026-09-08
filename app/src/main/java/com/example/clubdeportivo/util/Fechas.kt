package com.example.clubdeportivo.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ayuda a comparar fechas/horas de texto ("yyyy-MM-dd", "HH:mm") sin depender de java.time (minSdk 24). */
object Fechas {
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

    fun sumarDias(dias: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dias)
        return formatoFecha().format(cal.time)
    }

    /** Próximos [cantidad] días (incluyendo hoy) como pares fecha/etiqueta legible, ej. "Hoy", "Mañana", "vie. 12 sep". */
    fun proximosDias(cantidad: Int): List<Pair<String, String>> {
        val nombresDia = arrayOf("dom.", "lun.", "mar.", "mié.", "jue.", "vie.", "sáb.")
        val nombresMes = arrayOf(
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
        )
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
