package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.SolicitudCambioPlan
import com.example.clubdeportivo.data.model.SolicitudRenovacion
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasCambioPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal fun DocumentSnapshot.toSolicitudCambioPlan(): SolicitudCambioPlan? {
    val socioId = getString("socioId") ?: return null
    val actual = runCatching { PlanIndividual.valueOf(getString("planActual").orEmpty()) }.getOrNull() ?: return null
    val nuevo = runCatching { PlanIndividual.valueOf(getString("planNuevo").orEmpty()) }.getOrNull() ?: return null
    return SolicitudCambioPlan(
        id = id,
        socioId = socioId,
        socioNombre = getString("socioNombre").orEmpty(),
        membresiaId = getString("membresiaId").orEmpty(),
        planActual = actual,
        planNuevo = nuevo,
        precioActual = getDouble("precioActual") ?: 0.0,
        precioNuevo = getDouble("precioNuevo") ?: 0.0,
        status = getString("status").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
        motivoRechazo = getString("motivoRechazo").orEmpty(),
        revisadoPor = getString("reviewedBy").orEmpty(),
        revisadoPorNombre = getString("reviewedByNombre").orEmpty(),
        revisadaEn = getTimestamp("reviewedAt")?.toDate()?.time
    )
}

/** Solicitudes de cambio de plan: las envía el miembro y las resuelve un administrador o un supervisor. */
class CambioPlanRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val preciosRepository: PreciosRepository = FirebasePreciosRepository(db)
) {
    private val solicitudes = db.collection("solicitudes_cambio_plan")
    private val membresias = db.collection("membresias")

    fun observarUltima(socioId: String): Flow<SolicitudCambioPlan?> = callbackFlow {
        val registro = solicitudes.whereEqualTo("socioId", socioId).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else {
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toSolicitudCambioPlan() }.maxByOrNull { it.createdAt })
            }
        }
        awaitClose { registro.remove() }
    }

    fun observarPendientes(): Flow<List<SolicitudCambioPlan>> = callbackFlow {
        val registro = solicitudes.whereEqualTo("status", SolicitudCambioPlan.PENDIENTE).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else {
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toSolicitudCambioPlan() }.sortedBy { it.createdAt })
            }
        }
        awaitClose { registro.remove() }
    }

    /**
     * ¿Tiene esta membresía una renovación esperando respuesta? Si sí, no se pide ni se aprueba un cambio de plan.
     * Un miembro solo puede consultar lo suyo (las reglas de Firestore lo exigen), por eso con [socioId] la consulta
     * también filtra por quien la hizo; el personal que aprueba consulta por membresía.
     */
    suspend fun hayRenovacionPendiente(membresiaId: String, socioId: String? = null): Boolean {
        var consulta = db.collection("solicitudes_renovacion")
            .whereEqualTo("membresiaId", membresiaId)
            .whereEqualTo("status", SolicitudRenovacion.PENDIENTE)
        if (socioId != null) consulta = consulta.whereEqualTo("socioId", socioId)
        return consulta.get().await().isEmpty.not()
    }

    /** El precio vigente del plan (el que editó el administrador, o el base del catálogo). */
    suspend fun precioVigente(plan: PlanIndividual): Double =
        ClavesPrecio.de(ClavesPrecio.individual(plan), preciosRepository.obtenerEditados())

    suspend fun enviar(membresiaId: String, planActual: PlanIndividual, precioActual: Double, planNuevo: PlanIndividual, socioNombre: String): SolicitudCambioPlan {
        val uid = auth.currentUser?.uid ?: error(Textos.get("err_sin_sesion"))
        val documento = solicitudes.document()
        val solicitud = SolicitudCambioPlan(
            id = documento.id,
            socioId = uid,
            socioNombre = socioNombre,
            membresiaId = membresiaId,
            planActual = planActual,
            planNuevo = planNuevo,
            precioActual = precioActual,
            precioNuevo = precioVigente(planNuevo),
            status = SolicitudCambioPlan.PENDIENTE,
            createdAt = System.currentTimeMillis()
        )
        documento.set(
            mapOf(
                "id" to solicitud.id,
                "socioId" to solicitud.socioId,
                "socioNombre" to solicitud.socioNombre,
                "membresiaId" to solicitud.membresiaId,
                "planActual" to solicitud.planActual.name,
                "planNuevo" to solicitud.planNuevo.name,
                "precioActual" to solicitud.precioActual,
                "precioNuevo" to solicitud.precioNuevo,
                "status" to solicitud.status,
                "createdAt" to solicitud.createdAt
            )
        ).await()
        return solicitud
    }

    /**
     * Aprueba el cambio: la membresía pasa al plan nuevo (con el precio vigente de ese plan) y la solicitud queda
     * aprobada, todo en una transacción. El precio nuevo se cobra desde la siguiente renovación.
     */
    suspend fun aprobar(solicitud: SolicitudCambioPlan) {
        val hayRenovacion = hayRenovacionPendiente(solicitud.membresiaId)
        val precioNuevo = precioVigente(solicitud.planNuevo)
        val revisor = SesionManager.usuarioActual
        val solicitudRef = solicitudes.document(solicitud.id)
        val membresiaRef = membresias.document(solicitud.membresiaId)
        db.runTransaction { transaccion ->
            val actualSolicitud = transaccion.get(solicitudRef).toSolicitudCambioPlan()
                ?: throw IllegalStateException(Textos.get("err_solicitud_inexistente"))
            val membresia = transaccion.get(membresiaRef).toMembresia()
            ReglasCambioPlan.motivoNoSePuedeAprobar(actualSolicitud, membresia, Fechas.hoy(), hayRenovacion)
                ?.let { throw IllegalStateException(it) }
            transaccion.update(
                membresiaRef,
                mapOf(
                    "plan" to solicitud.planNuevo.name,
                    "precio" to precioNuevo,
                    "planAnterior" to solicitud.planActual.name,
                    "precioAnterior" to (membresia?.precio ?: solicitud.precioActual),
                    "cambioPlanSolicitudId" to solicitud.id,
                    "cambioPlanEn" to FieldValue.serverTimestamp(),
                    "cambioPlanPor" to (revisor?.id ?: ""),
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            transaccion.update(
                solicitudRef,
                mapOf(
                    "status" to SolicitudCambioPlan.APROBADO,
                    "precioNuevo" to precioNuevo,
                    "reviewedBy" to (revisor?.id ?: ""),
                    "reviewedByNombre" to (revisor?.nombre ?: ""),
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
    }

    suspend fun rechazar(solicitudId: String, motivo: String) {
        val revisor = SesionManager.usuarioActual
        val ref = solicitudes.document(solicitudId)
        db.runTransaction { transaccion ->
            val actual = transaccion.get(ref).toSolicitudCambioPlan() ?: throw IllegalStateException(Textos.get("err_solicitud_inexistente"))
            if (actual.status != SolicitudCambioPlan.PENDIENTE) throw IllegalStateException(Textos.get("err_solicitud_atendida"))
            transaccion.update(
                ref,
                mapOf(
                    "status" to SolicitudCambioPlan.RECHAZADO,
                    "motivoRechazo" to motivo.trim(),
                    "reviewedBy" to (revisor?.id ?: ""),
                    "reviewedByNombre" to (revisor?.nombre ?: ""),
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
    }
}
