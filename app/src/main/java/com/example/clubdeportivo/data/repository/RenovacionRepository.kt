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
        createdAt = getLong("createdAt") ?: 0L,
        motivoRechazo = getString("motivoRechazo").orEmpty(),
        reviewedDate = getString("reviewedDate").orEmpty(),
        reviewedAt = getTimestamp("reviewedAt")?.toDate()?.time,
        renewedUntil = getString("renewedUntil").orEmpty()
    )
}

class RenovacionRepository(
    private val gestionMembresiasRepository: GestionMembresiasRepository = FirebaseGestionMembresiasRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val solicitudes = db.collection("solicitudes_renovacion")

    fun observarUltimaSolicitud(socioId: String): Flow<SolicitudRenovacion?> = callbackFlow {
        val registration = solicitudes
            .whereEqualTo("socioId", socioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val ultima = snapshot?.documents
                        ?.mapNotNull { it.toSolicitudRenovacion() }
                        ?.maxByOrNull { it.createdAt }
                    trySend(ultima)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun obtenerUltimaSolicitud(socioId: String): SolicitudRenovacion? =
        solicitudes.whereEqualTo("socioId", socioId).get().await()
            .documents
            .mapNotNull { it.toSolicitudRenovacion() }
            .maxByOrNull { it.createdAt }

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

    suspend fun enviar(solicitud: SolicitudRenovacion, comprobanteBase64: String): SolicitudRenovacion {
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
        val documento = if (solicitud.id.isBlank()) solicitudes.document() else solicitudes.document(solicitud.id)
        val guardada = solicitud.copy(
            id = documento.id,
            socioId = socioUid,
            comprobanteUrl = comprobanteBase64
        )
        try {
            documento.set(guardada.toMap()).await()
        } catch (error: Exception) {
            throw IllegalStateException(
                "${Textos.get("renewal_request_write_failed")} ${error.message.orEmpty()}",
                error
            )
        }
        return guardada
    }

    suspend fun aprobar(solicitud: SolicitudRenovacion) {
        gestionMembresiasRepository.renovar(
            membresiaId = solicitud.membresiaId,
            precio = solicitud.monto,
            metodoPago = com.example.clubdeportivo.data.model.MetodoPago.TRANSFERENCIA,
            solicitudId = solicitud.id
        )
    }

    suspend fun rechazar(solicitudId: String, motivo: String) {
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
                    "motivoRechazo" to motivo.trim(),
                    "reviewedBy" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                    "reviewedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    private fun SolicitudRenovacion.toMap(): Map<String, Any> = buildMap {
        put("id", id)
        put("socioId", socioId)
        put("socioNombre", socioNombre)
        put("membresiaId", membresiaId)
        put("planId", planId)
        put("monto", monto)
        put("concepto", concepto)
        put("comprobanteUrl", comprobanteUrl)
        put("status", status)
        put("createdAt", createdAt)
        if (motivoRechazo.isNotBlank()) put("motivoRechazo", motivoRechazo)
    }

    private companion object {
        const val PREFIJO_COMPROBANTE = "data:image/jpeg;base64,"
        const val MAX_COMPROBANTE_CARACTERES = 280_000
    }
}
