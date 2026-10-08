package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasReserva
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toReserva(): Reserva? {
    val usuarioId = getString("usuarioId") ?: return null

    return Reserva(
        id = id,
        usuarioId = usuarioId,
        areaId = getString("areaId") ?: "",
        fecha = getString("fecha") ?: "",
        horaInicio = getString("horaInicio") ?: "",
        horaFin = getString("horaFin") ?: "",
        estado = runCatching { EstadoReserva.valueOf(getString("estado") ?: "CONFIRMADA") }
            .getOrDefault(EstadoReserva.CANCELADA),
        esExterno = getBoolean("esExterno") ?: false,
        personas = (getLong("personas") ?: 1L)
            .toInt()
            .coerceAtLeast(1),

        usuarioNombre = getString("usuarioNombre") ?: "",
        areaNombre = getString("areaNombre") ?: "",
        deporte = getString("deporte") ?: ""
    )
}

class FirebaseReservaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReservaRepository {

    private val reservas = db.collection("reservas")
    private val materialAsignado = db.collection("materialAsignado")
    private val checkins = db.collection("checkins")

    /**
     * Qué herramienta se presta automáticamente según el área — dato fijo del club (no
     * depende de qué usuario reserva), igual que en la versión de prueba: no hace falta
     * guardarlo en Firestore, es configuración de la app.
     */
    private val herramientaPorArea = mapOf(
        "1" to "10", "2" to "10", "3" to "12", "4" to "12",
        "5" to "11", "6" to "11", "7" to "15", "8" to "15"
    )

    override suspend fun obtenerReservasDeUsuario(usuarioId: String): List<Reserva> {
        return reservas.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.toReserva() }
    }

    override suspend fun contarReservasActivas(usuarioId: String): Int {
        return obtenerReservasDeUsuario(usuarioId).count {
            it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.PENDIENTE_APROBACION
        }
    }

    override suspend fun crearReserva(
        usuarioId: String,
        areaId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        esExterno: Boolean,
        personas: Int,
        requiereAprobacion: Boolean
    ): Reserva {
        // Miembros y visitantes quedan "en revisión" hasta que el encargado del área apruebe o rechace; la reserva
        // del propio personal (administrador o encargado) queda confirmada.
        val estadoInicial = if (requiereAprobacion) EstadoReserva.PENDIENTE_APROBACION else EstadoReserva.CONFIRMADA

        // Nombre del área y de la persona se guardan en la propia reserva: así las listas y reportes no
        // dependen de que el área siga existiendo ni necesitan una lectura extra por cada fila.
        val area = db.collection("areas").document(areaId).get().await()
        val persona = db.collection("usuarios").document(usuarioId).get().await()
        val horas = ((ReglasReserva.aMinutos(horaFin) ?: 0) - (ReglasReserva.aMinutos(horaInicio) ?: 0)) / 60

        val datos = mapOf(
            "usuarioId" to usuarioId,
            "usuarioNombre" to (persona.getString("nombre") ?: ""),
            "areaId" to areaId,
            "areaNombre" to (area.getString("nombre") ?: ""),
            "deporte" to (area.getString("tipo") ?: ""),
            "fecha" to fecha,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin,
            "duracionHoras" to horas.toLong(),
            "personas" to personas.toLong(),
            "estado" to estadoInicial.name,
            "esExterno" to esExterno,
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        val documento = reservas.add(datos).await()
        val nuevaReserva = Reserva(
            documento.id, usuarioId, areaId, fecha, horaInicio, horaFin, estadoInicial, esExterno, personas,
            usuarioNombre = persona.getString("nombre") ?: "",
            areaNombre = area.getString("nombre") ?: "",
            deporte = area.getString("tipo") ?: ""
        )

        // El material solo se asigna si la reserva queda CONFIRMADA (una pendiente de aprobación todavía no lo
        // necesita: se aparta cuando el encargado la aprueba).
        if (estadoInicial == EstadoReserva.CONFIRMADA) asignarMaterial(nuevaReserva)

        return nuevaReserva
    }

    /** Aparta el material del área si la reserva tiene al menos [Catalogos.MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS] de anticipación. */
    private suspend fun asignarMaterial(reserva: Reserva) {
        val horasDeAnticipacion = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio)
        if (horasDeAnticipacion < Catalogos.MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS) return
        val herramientaId = herramientaPorArea[reserva.areaId] ?: return

        val herramienta = db.collection("herramientas").document(herramientaId).get().await()
        materialAsignado.add(
            mapOf(
                "reservaId" to reserva.id,
                "herramientaId" to herramientaId,
                "herramientaNombre" to (herramienta.getString("nombre") ?: ""),
                "areaId" to reserva.areaId,
                "fecha" to reserva.fecha,
                "cantidad" to 1L,
                "estado" to "ASIGNADO",
                "creadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Lee una reserva que espera respuesta y comprueba que quien responde puede hacerlo (el encargado, solo en su área). */
    private suspend fun reservaPorResolver(reservaId: String): Reserva {
        val reserva = reservas.document(reservaId).get().await().toReserva() ?: error("La reserva ya no existe.")
        if (!com.example.clubdeportivo.data.SesionManager.usuarioActual.puedeVerDeporte(reserva.deporte)) {
            error("Esta reserva es de otra área: solo su encargado puede responderla.")
        }
        if (reserva.estado != EstadoReserva.PENDIENTE_APROBACION) {
            error("Esta reserva ya no está en revisión (${reserva.estado.name.lowercase().replace('_', ' ')}).")
        }
        return reserva
    }

    override suspend fun aprobarReserva(reservaId: String) {
        val reserva = reservaPorResolver(reservaId)
        if (Fechas.horasDesdeAhora(reserva.fecha, reserva.horaFin) <= 0) error("Esta reserva ya terminó: no se puede aprobar.")
        reservas.document(reservaId).update(
            mapOf(
                "estado" to EstadoReserva.CONFIRMADA.name,
                "aprobadaEn" to FieldValue.serverTimestamp(),
                "aprobadaPor" to (com.example.clubdeportivo.data.SesionManager.usuarioActual?.id ?: ""),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        asignarMaterial(reserva.copy(estado = EstadoReserva.CONFIRMADA))
    }

    override suspend fun rechazarReserva(reservaId: String) {
        reservaPorResolver(reservaId)
        reservas.document(reservaId).update(
            mapOf(
                "estado" to EstadoReserva.RECHAZADA.name,
                "rechazadaEn" to FieldValue.serverTimestamp(),
                "rechazadaPor" to (com.example.clubdeportivo.data.SesionManager.usuarioActual?.id ?: ""),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun obtenerReservasDeArea(areaId: String): List<Reserva> {
        return reservas.whereEqualTo("areaId", areaId).get().await()
            .documents.mapNotNull { it.toReserva() }
            .filter { ReglasReserva.esVigente(it) }
    }

    override suspend fun obtenerReservasPorDeporte(
        deporte: String
    ): List<Reserva> {

        return reservas
            .whereEqualTo("deporte", deporte)
            .get()
            .await()
            .documents
            .mapNotNull { it.toReserva() }
            .filter { ReglasReserva.esVigente(it) }
    }

    override suspend fun obtenerReservasVigentes(): List<Reserva> {
        return reservas.get().await().documents.mapNotNull { it.toReserva() }
            .filter { ReglasReserva.esVigente(it) }
    }

    override suspend fun obtenerMaterialAsignado(reservaId: String): List<MaterialAsignado> {
        return materialAsignado.whereEqualTo("reservaId", reservaId).get().await().documents.map { doc ->
            MaterialAsignado(
                id = doc.id,
                reservaId = reservaId,
                herramientaId = doc.getString("herramientaId") ?: "",
                cantidad = (doc.getLong("cantidad") ?: 0).toInt()
            )
        }
    }

    override suspend fun registrarAsistencia(
        reservaId: String,
        usuarioId: String,
        asistencia: String
    ) {

        val datos = mapOf(
            "reservaId" to reservaId,
            "usuarioId" to usuarioId,
            "asistencia" to asistencia,
            "registradoEn" to FieldValue.serverTimestamp()
        )

        checkins.document(reservaId).set(datos).await()

        if (asistencia == "NO_ASISTIO") {
            registrarNoShow(usuarioId)
        }
    }

    override suspend fun obtenerAsistencia(
        reservaId: String
    ): String? {

        val documento = checkins
            .document(reservaId)
            .get()
            .await()

        if (!documento.exists()) {
            return null
        }

        return documento.getString("asistencia")
    }

    override suspend fun cancelarReserva(reservaId: String): Boolean {
        val documento = reservas.document(reservaId).get().await()
        val reserva = documento.toReserva() ?: return true
        // Una reserva ya cancelada o rechazada no se cancela otra vez (no se reescribe su estado).
        if (!ReglasReserva.esVigente(reserva)) return true

        val sinPenalizacion = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio) >=
            Catalogos.CANCELACION_SIN_PENALIZACION_HORAS

        reservas.document(reservaId).update(
            mapOf(
                "estado" to EstadoReserva.CANCELADA.name,
                "canceladaEn" to FieldValue.serverTimestamp(),
                "sinPenalizacion" to sinPenalizacion,
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        // El material que se había apartado para esta reserva ya no hace falta.
        val asignado = materialAsignado.whereEqualTo("reservaId", reservaId).get().await()
        if (!asignado.isEmpty) {
            val batch = db.batch()
            asignado.documents.forEach { batch.update(it.reference, "estado", "CANCELADO") }
            batch.commit().await()
        }
        return sinPenalizacion
    }

    /** La inasistencia ya queda guardada como check-in en [registrarAsistencia]; de ahí se calcula el bloqueo. */
    override suspend fun registrarNoShow(usuarioId: String) = Unit

    override suspend fun estaBloqueadoPorInasistencias(usuarioId: String): Boolean {
        // Las faltas salen de los check-ins guardados (no de la memoria del teléfono), así valen en cualquier dispositivo.
        val strikes = checkins.whereEqualTo("usuarioId", usuarioId).get().await().documents
            .filter { it.getString("asistencia") == "NO_ASISTIO" }
            .mapNotNull { it.getTimestamp("registradoEn")?.toDate()?.time }
        if (strikes.isEmpty()) return false
        val ventanaMs = Catalogos.VENTANA_INASISTENCIAS_DIAS * 24L * 60 * 60 * 1000
        val recientes = strikes.filter { System.currentTimeMillis() - it <= ventanaMs }
        if (recientes.size < Catalogos.INASISTENCIAS_PARA_BLOQUEO) return false

        val ultimaFalta = recientes.max()
        val bloqueoMs = Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS * 24L * 60 * 60 * 1000
        return System.currentTimeMillis() - ultimaFalta <= bloqueoMs
    }
}
