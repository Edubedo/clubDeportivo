package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.PagoClub
import com.example.clubdeportivo.data.model.TipoMembresia
import java.util.Calendar

/** Números de membresías y dinero que ve el administrador. Se calculan aquí, sin Firebase, para poder probarlos. */
data class ResumenAdmin(
    /** Mes que se está mostrando, "yyyy-MM". */
    val mes: String,
    val ingresosMes: Double,
    val ingresosMesAnterior: Double,
    val cobrosMes: Int,
    /** Los últimos meses de más antiguo a más reciente: ("2026-05", 12000.0)... incluye el actual. */
    val ingresosPorMes: List<Pair<String, Double>>,
    val ingresosPorMetodo: List<Pair<MetodoPago?, Double>>,
    /** Lo que se cobraría en un mes si todas las membresías activas renuevan (no incluye visitas). */
    val ingresoMensualEsperado: Double,
    val activas: Int,
    val personasActivas: Int,
    val vencidas: Int,
    val suspendidas: Int,
    /** Altas (membresías y visitas nuevas) cobradas este mes. */
    val altasMes: Int,
    /** Activas que vencen en los próximos días, las más próximas primero. */
    val porVencer: List<MembresiaDetalle>
) {
    /** Cambio porcentual contra el mes anterior; null si el mes anterior no tuvo ingresos (no hay base para comparar). */
    val variacionContraMesAnterior: Double?
        get() = if (ingresosMesAnterior > 0) (ingresosMes - ingresosMesAnterior) / ingresosMesAnterior * 100 else null
}

object CalculoResumenAdmin {
    const val DIAS_POR_VENCER = 7
    const val MESES_EN_GRAFICA = 6

    /** "2026-10-07" -> "2026-10". */
    fun mesDe(fecha: String): String = fecha.take(7)

    /** Los [cantidad] meses que terminan en el mes de [hoy], del más antiguo al actual ("yyyy-MM"). */
    fun ultimosMeses(hoy: String, cantidad: Int = MESES_EN_GRAFICA): List<String> {
        val anio = hoy.substring(0, 4).toInt()
        val mes = hoy.substring(5, 7).toInt()
        val cal = Calendar.getInstance().apply { clear(); set(anio, mes - 1, 1) }
        return (cantidad - 1 downTo 0).map { atras ->
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -atras)
            "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
        }
    }

    /** Primer día del mes más antiguo de la gráfica: desde ahí hay que pedir los cobros. */
    fun primerDiaDeLaGrafica(hoy: String, cantidad: Int = MESES_EN_GRAFICA): String = ultimosMeses(hoy, cantidad).first() + "-01"

    fun calcular(membresias: List<MembresiaDetalle>, pagos: List<PagoClub>, hoy: String): ResumenAdmin {
        val meses = ultimosMeses(hoy)
        val mesActual = meses.last()
        val mesAnterior = meses.getOrNull(meses.size - 2)

        val porMes = pagos.groupBy { mesDe(it.fecha) }
        val totalDe = { mes: String? -> porMes[mes].orEmpty().sumOf { it.monto } }
        val pagosDelMes = porMes[mesActual].orEmpty()

        val conEstado = membresias.map { it to ReglasMembresia.estadoEfectivo(it.membresia, hoy) }
        val activas = conEstado.filter { it.second == EstadoMembresia.ACTIVA && it.first.membresia.tipo != TipoMembresia.VISITA }
        val limite = Fechas.sumarDias(DIAS_POR_VENCER)

        return ResumenAdmin(
            mes = mesActual,
            ingresosMes = pagosDelMes.sumOf { it.monto },
            ingresosMesAnterior = totalDe(mesAnterior),
            cobrosMes = pagosDelMes.size,
            ingresosPorMes = meses.map { it to totalDe(it) },
            ingresosPorMetodo = pagosDelMes.groupBy { it.metodo }
                .map { (metodo, lista) -> metodo to lista.sumOf { it.monto } }
                .sortedByDescending { it.second },
            ingresoMensualEsperado = activas.sumOf { it.first.membresia.precio },
            activas = activas.size,
            personasActivas = activas.sumOf { it.first.personas.size },
            vencidas = conEstado.count { it.second == EstadoMembresia.VENCIDA && it.first.membresia.tipo != TipoMembresia.VISITA },
            suspendidas = conEstado.count { it.second == EstadoMembresia.SUSPENDIDA },
            altasMes = pagosDelMes.count { it.concepto == ConceptoPago.ALTA || it.concepto == ConceptoPago.VISITA },
            porVencer = activas.map { it.first }
                .filter { it.membresia.fechaVencimiento in hoy..limite }
                .sortedBy { it.membresia.fechaVencimiento }
        )
    }
}
