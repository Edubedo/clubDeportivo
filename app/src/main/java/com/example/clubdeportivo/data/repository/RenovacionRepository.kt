package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toSolicitudRenovacion(): SolicitudRenovacion? {
    val socioId = getString("socioId") ?: return null
    val monto = getDouble("monto") ?: getLong("monto")?.toDouble() ?: return null
    return SolicitudRenovacion(
        id = id,
        socioId = socioId,
        socioNombre = getString("socioNombre").orEmpty(),
        membresiaId = getString("membresiaId").orEmpty(),
        planId = getString("planId").orEmpty(),
        monto = monto,
        concepto = getString("concepto").orEmpty(),
        comprobanteUrl = getString("comprobanteUrl").orEmpty(),
        status = getString("status").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L
    )
}

class RenovacionRepository(
    private val gestionMembresiasRepository: GestionMembresiasRepository = FirebaseGestionMembresiasRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val solicitudes = db.collection("solicitudes_renovacion")

    fun observarSolicitudPendiente(socioId: String): Flow<SolicitudRenovacion?> = callbackFlow {
        val registration = solicitudes
            .whereEqualTo("socioId", socioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val pendiente = snapshot?.documents
                        ?.mapNotNull { it.toSolicitudRenovacion() }
                        ?.filter { it.status == SolicitudRenovacion.PENDIENTE }
                        ?.maxByOrNull { it.createdAt }
                    trySend(pendiente)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observarSolicitudesPendientes(): Flow<List<SolicitudRenovacion>> = callbackFlow {
        val registration = solicitudes
            .whereEqualTo("status", SolicitudRenovacion.PENDIENTE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val pendientes = snapshot?.documents.orEmpty()
                        .mapNotNull { it.toSolicitudRenovacion() }
                        .sortedBy { it.createdAt }
                    trySend(pendientes)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun enviar(solicitud: SolicitudRenovacion, comprobanteBase64: String) {
        val socioUid = auth.currentUser?.uid
            ?: error(Textos.get("renewal_auth_required"))
        check(solicitud.socioId == socioUid) {
            Textos.get("renewal_uid_mismatch")
        }
        require(comprobanteBase64.startsWith(PREFIJO_COMPROBANTE)) {
            Textos.get("renewal_proof_invalid")
        }
        require(comprobanteBase64.length <= MAX_COMPROBANTE_CARACTERES) {
            Textos.get("renewal_proof_too_large")
        }
        val documento = solicitudes.document()
        try {
            documento.set(
                solicitud.copy(
                    id = documento.id,
                    socioId = socioUid,
                    comprobanteUrl = comprobanteBase64
                ).toMap()
            ).await()
        } catch (error: Exception) {
            throw IllegalStateException(
                "${Textos.get("renewal_request_write_failed")} ${error.message.orEmpty()}",
                error
            )
        }
    }

    suspend fun aprobar(solicitud: SolicitudRenovacion) {
        gestionMembresiasRepository.renovar(
            membresiaId = solicitud.membresiaId,
            precio = solicitud.monto,
            metodoPago = com.example.clubdeportivo.data.model.MetodoPago.TRANSFERENCIA,
            solicitudId = solicitud.id
        )
    }

    suspend fun rechazar(solicitudId: String) {
        val referencia = solicitudes.document(solicitudId)
        db.runTransaction { transaction ->
            val solicitud = transaction.get(referencia)
            check(solicitud.getString("status") == SolicitudRenovacion.PENDIENTE) {
                "La solicitud ya fue atendida."
            }
            transaction.update(
                referencia,
                mapOf(
                    "status" to SolicitudRenovacion.RECHAZADO,
                    "reviewedBy" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                    "reviewedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    private fun SolicitudRenovacion.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "socioId" to socioId,
        "socioNombre" to socioNombre,
        "membresiaId" to membresiaId,
        "planId" to planId,
        "monto" to monto,
        "concepto" to concepto,
        "comprobanteUrl" to comprobanteUrl,
        "status" to status,
        "createdAt" to createdAt
    )

    private companion object {
        const val PREFIJO_COMPROBANTE = "data:image/jpeg;base64,"
        const val MAX_COMPROBANTE_CARACTERES = 280_000
    }
}
