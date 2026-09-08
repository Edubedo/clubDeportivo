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

/**
 * Todavía no hay una pantalla en la app para "contratar" una membresía — por ahora, para
 * probar esta pantalla con datos, hay que crear a mano un documento en la colección
 * "membresias" desde la consola de Firebase (ver docs/arquitectura.md).
 */
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
}
