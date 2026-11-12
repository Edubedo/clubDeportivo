package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal fun DocumentSnapshot.toMembresia(): Membresia? {
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
        fechaVencimiento = getString("fechaVencimiento") ?: "",
        comprobanteUrl = getString("comprobanteUrl"),
        motivoSuspension = getString("motivoSuspension") ?: ""
    )
}

/**
 * Lee la membresía de una persona. Las membresías se dan de alta y se administran desde la
 * sección Membresías (ver FirebaseGestionMembresiasRepository).
 */
class FirebaseMembresiaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MembresiaRepository {

    private val membresias = db.collection("membresias")
    private val integrantesFamiliares = db.collection("integrantesFamiliares")
    private val miembros = db.collection("miembros")

    override suspend fun obtenerMembresia(usuarioId: String): Membresia? {
        val propias = membresias.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.toMembresia() }

        // Un integrante de un paquete familiar tiene su propia cuenta y código, pero la membresía es del titular.
        val idsDeFamilia = miembros.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.getString("membresiaId") }
        val deFamilia = idsDeFamilia.distinct().mapNotNull { id ->
            membresias.document(id).get().await().toMembresia()
        }

        return seleccionarMembresia((propias + deFamilia).distinctBy { it.id }, Fechas.hoy())
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

internal fun seleccionarMembresia(membresias: List<Membresia>, hoy: String): Membresia? =
    membresias.maxWithOrNull(
        compareBy<Membresia> {
            when (ReglasMembresia.estadoEfectivo(it, hoy)) {
                EstadoMembresia.ACTIVA -> 2
                EstadoMembresia.VENCIDA -> 1
                EstadoMembresia.SUSPENDIDA -> 0
            }
        }.thenBy { it.fechaInicio }
            .thenBy { it.fechaVencimiento }
    )
