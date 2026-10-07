package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.EstadoPago
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.PagoClub
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface PagosRepository {
    /** Cobros registrados con fecha igual o posterior a [fechaInicio] ("yyyy-MM-dd"), solo los pagados. */
    suspend fun obtenerDesde(fechaInicio: String): List<PagoClub>
}

private fun DocumentSnapshot.toPagoClub(): PagoClub? {
    val monto = getDouble("monto") ?: return null
    val fecha = getString("fechaPago") ?: getString("fecha") ?: return null
    return PagoClub(
        id = id,
        membresiaId = getString("membresiaId").orEmpty(),
        titularNombre = getString("titularNombre").orEmpty(),
        concepto = runCatching { ConceptoPago.valueOf(getString("concepto").orEmpty()) }.getOrDefault(ConceptoPago.OTRO),
        monto = monto,
        metodo = runCatching { MetodoPago.valueOf(getString("metodoPago").orEmpty().uppercase()) }.getOrNull(),
        fecha = fecha,
        estado = runCatching { EstadoPago.valueOf(getString("estado") ?: "PAGADO") }.getOrDefault(EstadoPago.PAGADO)
    )
}

class FirebasePagosRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PagosRepository {

    override suspend fun obtenerDesde(fechaInicio: String): List<PagoClub> =
        db.collection("pagos").whereGreaterThanOrEqualTo("fechaPago", fechaInicio).get().await()
            .documents.mapNotNull { it.toPagoClub() }
            .filter { it.estado == EstadoPago.PAGADO }
}
