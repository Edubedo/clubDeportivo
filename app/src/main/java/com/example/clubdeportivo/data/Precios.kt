package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia

/**
 * Identificadores de cada precio editable del club. Los valores base salen de [Catalogos]; lo que
 * el administrador edita se guarda aparte (colección "precios") y tiene prioridad sobre el base.
 */
object ClavesPrecio {
    const val VISITA = "VISITA"

    fun individual(plan: PlanIndividual) = "INDIVIDUAL_${plan.name}"

    fun familiar(paqueteId: Int) = "FAMILIAR_$paqueteId"

    fun base(clave: String): Double? = when {
        clave == VISITA -> Catalogos.PRECIO_VISITA
        clave.startsWith("INDIVIDUAL_") ->
            PlanIndividual.entries.firstOrNull { it.name == clave.removePrefix("INDIVIDUAL_") }
                ?.let { Catalogos.precioPlanIndividual(it) }
        clave.startsWith("FAMILIAR_") ->
            clave.removePrefix("FAMILIAR_").toIntOrNull()?.let { Catalogos.paqueteFamiliarPorId(it)?.precioMensual }
        else -> null
    }

    fun de(clave: String, editados: Map<String, Double>): Double = editados[clave] ?: base(clave) ?: 0.0

    fun deMembresia(tipo: TipoMembresia, plan: PlanIndividual?, paqueteId: Int?, editados: Map<String, Double>): Double =
        when (tipo) {
            TipoMembresia.VISITA -> de(VISITA, editados)
            TipoMembresia.INDIVIDUAL -> plan?.let { de(individual(it), editados) } ?: 0.0
            TipoMembresia.FAMILIAR -> paqueteId?.let { de(familiar(it), editados) } ?: 0.0
        }
}
