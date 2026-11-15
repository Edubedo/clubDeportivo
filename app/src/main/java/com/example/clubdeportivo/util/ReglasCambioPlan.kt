package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.SolicitudCambioPlan
import com.example.clubdeportivo.data.model.TipoMembresia

/**
 * Reglas del cambio de plan que pide un miembro (sin Firebase ni Android, para poder probarlas).
 *
 *  - Solo las membresías INDIVIDUALES cambian de plan desde la app; un paquete familiar cambia el número de personas
 *    y una visita dura un día, así que eso se hace en recepción.
 *  - Los niveles van de menor a mayor: NINO, NORMAL, DELUXE. Cambiar a uno mayor es una mejora; a uno menor, una reducción.
 *  - Solo con la membresía activa, sin otra solicitud de cambio ni una renovación en revisión (si se aprobara la
 *    renovación después del cambio, el plan de la renovación ya no coincidiría con el de la membresía).
 *  - Al aprobarse el cambio es inmediato; el precio nuevo se cobra desde la siguiente renovación.
 */
object ReglasCambioPlan {

    /** Planes a los que se puede pasar desde [actual], del más bajo al más alto. */
    fun planesDisponibles(actual: PlanIndividual?): List<PlanIndividual> =
        PlanIndividual.entries.filter { it != actual }

    fun esMejora(actual: PlanIndividual, nuevo: PlanIndividual) = nuevo.ordinal > actual.ordinal

    /** Lo que cambia al mes: positivo si el plan nuevo cuesta más. */
    fun diferencia(precioActual: Double, precioNuevo: Double) = precioNuevo - precioActual

    /**
     * @return por qué el miembro NO puede pedir pasar a [nuevo], o null si puede.
     */
    fun motivoNoPuedePedir(
        membresia: Membresia?,
        hoy: String,
        nuevo: PlanIndividual,
        hayCambioPendiente: Boolean,
        hayRenovacionPendiente: Boolean
    ): String? {
        if (membresia == null || membresia.tipo != TipoMembresia.INDIVIDUAL || membresia.plan == null) {
            return Textos.get("cambio_plan_solo_individual")
        }
        if (ReglasMembresia.estadoEfectivo(membresia, hoy) != EstadoMembresia.ACTIVA) return Textos.get("cambio_plan_no_activa")
        if (membresia.plan == nuevo) return Textos.get("cambio_plan_mismo_plan")
        if (hayCambioPendiente) return Textos.get("cambio_plan_ya_pendiente")
        if (hayRenovacionPendiente) return Textos.get("cambio_plan_renovacion_pendiente")
        return null
    }

    /**
     * Lo que revisa quien aprueba, con la membresía recién leída: que la solicitud siga pendiente, que la membresía
     * siga en el plan desde el que se pidió el cambio y que no haya una renovación en revisión.
     * @return el motivo por el que no se puede aprobar, o null.
     */
    fun motivoNoSePuedeAprobar(
        solicitud: SolicitudCambioPlan,
        membresia: Membresia?,
        hoy: String,
        hayRenovacionPendiente: Boolean
    ): String? {
        if (solicitud.status != SolicitudCambioPlan.PENDIENTE) return Textos.get("cambio_plan_ya_atendida")
        if (membresia == null || membresia.id != solicitud.membresiaId || membresia.tipo != TipoMembresia.INDIVIDUAL ||
            membresia.plan != solicitud.planActual
        ) return Textos.get("cambio_plan_obsoleta")
        if (ReglasMembresia.estadoEfectivo(membresia, hoy) == EstadoMembresia.SUSPENDIDA) return Textos.get("cambio_plan_no_activa")
        if (hayRenovacionPendiente) return Textos.get("cambio_plan_renovacion_en_curso")
        return null
    }
}
