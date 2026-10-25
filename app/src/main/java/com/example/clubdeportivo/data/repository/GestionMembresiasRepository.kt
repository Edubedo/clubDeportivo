package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
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
        personas: List<PersonaForm>,
        metodoPago: MetodoPago
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

    /**
     * [motivo] es obligatorio al suspender (queda guardado y lo ve el miembro); al reactivar se borra y, si la membresía
     * estaba dada de baja, las cuentas de sus personas vuelven a poder iniciar sesión.
     */
    suspend fun cambiarEstado(membresiaId: String, estado: EstadoMembresia, motivo: String? = null)

    /**
     * Baja del miembro: suspende la membresía y desactiva la cuenta de cada persona que cubre. Nadie de esa membresía puede
     * iniciar sesión y, si alguien tiene la app abierta, la sesión se cierra sola. Se revierte con [cambiarEstado] a ACTIVA.
     */
    suspend fun darDeBaja(membresiaId: String, motivo: String)

    /** Suma un mes y registra el cobro con su [metodoPago]. */
    suspend fun renovar(membresiaId: String, precio: Double, metodoPago: MetodoPago)

    /**
     * Libera el código de un miembro que ya se había registrado (por ejemplo, si escribió mal su correo): su cuenta
     * anterior se desactiva y el código vuelve a servir para registrarse.
     */
    suspend fun restablecerAcceso(codigo: String)
}
