package com.example.clubdeportivo.util.reportes

import com.example.clubdeportivo.util.Textos
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * Un reporte ya armado, independiente del formato: el mismo contenido se escribe como PDF (EscritorPdf) o como
 * Excel (EscritorXlsx). Así la lógica de qué se exporta se prueba sin Android.
 */

sealed interface Celda {
    data class Texto(val valor: String) : Celda
    /** [dinero] = se muestra con signo de pesos y dos decimales. */
    data class Numero(val valor: Double, val dinero: Boolean = false) : Celda
}

fun texto(valor: String) = Celda.Texto(valor)
fun numero(valor: Number) = Celda.Numero(valor.toDouble())
fun dinero(valor: Double) = Celda.Numero(valor, dinero = true)

/** Texto de una celda tal como se ve en el PDF. */
fun Celda.comoTexto(): String = when (this) {
    is Celda.Texto -> valor
    is Celda.Numero -> if (dinero) formatoDinero(valor) else if (valor % 1.0 == 0.0) valor.toLong().toString() else "%.2f".format(valor)
}

fun formatoDinero(monto: Double): String = "$" + String.format(Locale.US, "%,.2f", monto)

/** Una tabla de datos. [pesos] reparte el ancho de las columnas en el PDF (una por columna). */
data class Tabla(
    val titulo: String,
    val columnas: List<String>,
    val filas: List<List<Celda>>,
    val pesos: List<Float> = columnas.map { 1f }
)

/** Cifras clave de una sección: pares "concepto" -> "valor". */
data class BloqueResumen(val titulo: String, val datos: List<Pair<String, String>>)

data class Reporte(
    val titulo: String,
    val periodo: String,
    val generado: String,
    val resumen: List<BloqueResumen>,
    val tablas: List<Tabla>
)

/** Qué partes del club se incluyen en el reporte. */
enum class SeccionReporte(val clave: String) {
    INGRESOS("rep_sec_ingresos"),
    MEMBRESIAS("rep_sec_membresias"),
    RESERVAS("rep_sec_reservas"),
    AREAS("rep_sec_areas"),
    INVENTARIO("rep_sec_inventario"),
    PERSONAL("rep_sec_personal"),
    TORNEOS("rep_sec_torneos");

    val etiqueta: String get() = Textos.get(clave)
}

/** Periodo que cubren los datos con fecha (cobros, reservas, altas, torneos). */
enum class PeriodoReporte(val clave: String) {
    ESTE_MES("rep_periodo_este_mes"),
    MES_ANTERIOR("rep_periodo_mes_anterior"),
    ULTIMOS_3_MESES("rep_periodo_3_meses"),
    ESTE_ANIO("rep_periodo_anio"),
    TODO("rep_periodo_todo");

    val etiqueta: String get() = Textos.get(clave)

    /** Primer y último día ("yyyy-MM-dd") del periodo, visto desde [hoy]. */
    fun rango(hoy: String): Pair<String, String> {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply { time = formato.parse(hoy) ?: time }
        fun texto() = formato.format(cal.time)
        fun finDeMes(): String { cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); return texto() }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return when (this) {
            ESTE_MES -> texto() to finDeMes()
            MES_ANTERIOR -> { cal.add(Calendar.MONTH, -1); val desde = texto(); desde to finDeMes() }
            ULTIMOS_3_MESES -> { val fin = Calendar.getInstance().apply { time = cal.time }
                cal.add(Calendar.MONTH, -2); val desde = texto(); cal.time = fin.time; desde to finDeMes() }
            ESTE_ANIO -> { cal.set(Calendar.MONTH, Calendar.JANUARY); val desde = texto()
                cal.set(Calendar.MONTH, Calendar.DECEMBER); desde to finDeMes() }
            TODO -> INICIO_HISTORICO to FIN_HISTORICO
        }
    }

    companion object {
        const val INICIO_HISTORICO = "2000-01-01"
        const val FIN_HISTORICO = "2099-12-31"
    }
}
