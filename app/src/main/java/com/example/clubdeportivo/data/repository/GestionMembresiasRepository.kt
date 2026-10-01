package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia

/** Altas, cambios y control de las membresías del club y de las personas (con su código) que cubren. */
interface GestionMembresiasRepository {
    suspend fun obtenerTodas(): List<MembresiaDetalle>

    /** Crea la membresía y una persona por cada elemento de [personas] (el primero es el titular), cada una con su código único. */
    suspend fun registrar(
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteId: Int?,
        precio: Double,
        personas: List<PersonaForm>
    ): MembresiaDetalle

    /**
     * Cambia plan, precio y personas. Las personas con código se conservan (y se actualizan), las que no traen código
     * son nuevas y reciben uno, y las que ya no aparecen pierden su acceso.
     */
    suspend fun actualizar(
        membresiaId: String,
        plan: PlanIndividual?,
        paqueteId: Int?,
        precio: Double,
        personas: List<PersonaForm>
    )

    /** Solo cambia el plan y el precio (para membresías anteriores a los códigos, cuyas personas no se pueden editar). */
    suspend fun cambiarPlan(membresiaId: String, plan: PlanIndividual?, paqueteId: Int?, precio: Double)

    suspend fun cambiarEstado(membresiaId: String, estado: EstadoMembresia)

    suspend fun renovar(membresiaId: String, precio: Double)
}
