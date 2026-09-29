package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toMembresia(): Membresia? {
    val usuarioId = getString("usuarioId") ?: return null
    return Membresia(
        id = id,
        usuarioId = usuarioId,
        tipo = TipoMembresia.valueOf(getString("tipo") ?: "INDIVIDUAL"),
        plan = getString("plan")?.let { PlanIndividual.valueOf(it) },
        paqueteFamiliarId = getLong("paqueteFamiliarId")?.toInt(),
        precio = getDouble("precio") ?: 0.0,
        estado = EstadoMembresia.valueOf(getString("estado") ?: "ACTIVA"),
        fechaInicio = getString("fechaInicio") ?: "",
        fechaVencimiento = getString("fechaVencimiento") ?: ""
    )
}

private fun datosMembresia(
    usuarioId: String,
    tipo: TipoMembresia,
    plan: PlanIndividual?,
    paqueteFamiliarId: Int?,
    precio: Double,
    estado: EstadoMembresia,
    fechaInicio: String,
    fechaVencimiento: String
): Map<String, Any?> = mapOf(
    "usuarioId" to usuarioId,
    "tipo" to tipo.name,
    "plan" to plan?.name,
    "paqueteFamiliarId" to paqueteFamiliarId?.toLong(),
    "precio" to precio,
    "estado" to estado.name,
    "fechaInicio" to fechaInicio,
    "fechaVencimiento" to fechaVencimiento
)

class FirebaseMembresiaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MembresiaRepository {

    private val membresias = db.collection("membresias")
    private val integrantesFamiliares = db.collection("integrantesFamiliares")

    override suspend fun obtenerMembresia(usuarioId: String): Membresia? {
        return membresias.whereEqualTo("usuarioId", usuarioId).limit(1).get().await()
            .documents.firstOrNull()?.toMembresia()
    }

    override suspend fun obtenerIntegrantes(membresiaId: String): List<IntegranteFamiliar> {
        return integrantesFamiliares.whereEqualTo("membresiaId", membresiaId).get().await()
            .documents.map { doc ->
                IntegranteFamiliar(
                    id = doc.id,
                    membresiaId = membresiaId,
                    nombre = doc.getString("nombre") ?: "",
                    parentesco = doc.getString("parentesco") ?: ""
                )
            }
    }

    override suspend fun obtenerMembresias(): List<Membresia> {
        return membresias.get().await().documents.mapNotNull { it.toMembresia() }
    }

    override suspend fun crearMembresia(
        usuarioId: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia {
        val datos = datosMembresia(
            usuarioId, tipo, plan, paqueteFamiliarId, precio,
            EstadoMembresia.ACTIVA, fechaInicio, fechaVencimiento
        )
        val documento = membresias.add(datos).await()
        return Membresia(
            id = documento.id,
            usuarioId = usuarioId,
            tipo = tipo,
            plan = plan,
            paqueteFamiliarId = paqueteFamiliarId,
            precio = precio,
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = fechaInicio,
            fechaVencimiento = fechaVencimiento
        )
    }

    override suspend fun actualizarMembresia(
        id: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia {
        val membresiaActual = membresias.document(id).get().await().toMembresia()
        val usuarioId = membresiaActual?.usuarioId ?: ""
        val datos = datosMembresia(usuarioId, tipo, plan, paqueteFamiliarId, precio, estado, fechaInicio, fechaVencimiento)
        membresias.document(id).set(datos).await()
        return Membresia(id, usuarioId, tipo, plan, paqueteFamiliarId, precio, estado, fechaInicio, fechaVencimiento)
    }

    override suspend fun agregarIntegrante(membresiaId: String, nombre: String, parentesco: String): IntegranteFamiliar {
        val datos = mapOf(
            "membresiaId" to membresiaId,
            "nombre" to nombre,
            "parentesco" to parentesco
        )
        val documento = integrantesFamiliares.add(datos).await()
        return IntegranteFamiliar(documento.id, membresiaId, nombre, parentesco)
    }

    override suspend fun eliminarIntegrante(integranteId: String) {
        integrantesFamiliares.document(integranteId).delete().await()
    }
}
