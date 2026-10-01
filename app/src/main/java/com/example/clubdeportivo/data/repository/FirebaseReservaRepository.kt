package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.Reserva
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
        estado = EstadoReserva.valueOf(getString("estado") ?: "CONFIRMADA"),
        esExterno = getBoolean("esExterno") ?: false,
        personas = (getLong("personas") ?: 1L).toInt().coerceAtLeast(1)
    )
}

class FirebaseReservaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReservaRepository {

    private val reservas = db.collection("reservas")
    private val materialAsignado = db.collection("materialAsignado")

    /**
     * Qué herramienta se presta automáticamente según el área — dato fijo del club (no
     * depende de qué usuario reserva), igual que en la versión de prueba: no hace falta
     * guardarlo en Firestore, es configuración de la app.
     */
    private val herramientaPorArea = mapOf(
        "1" to "10", "2" to "10", "3" to "12", "4" to "12",
        "5" to "11", "6" to "11", "7" to "15", "8" to "15"
    )

    /** Marcas de tiempo (ms) de inasistencias por usuario, para la regla de 3 strikes. */
    private val noShowsPorUsuario = mutableMapOf<String, MutableList<Long>>()

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
        personas: Int
    ): Reserva {
        // Un visitante externo necesita aprobación de un administrador antes de que su
        // reserva sea válida; un socio queda confirmado de inmediato.
        val estadoInicial = if (esExterno) EstadoReserva.PENDIENTE_APROBACION else EstadoReserva.CONFIRMADA

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
        val nuevaReserva = Reserva(documento.id, usuarioId, areaId, fecha, horaInicio, horaFin, estadoInicial, esExterno, personas)

        // El material solo se asigna si la reserva queda CONFIRMADA con al menos 1 hora de
        // anticipación (una PENDIENTE_APROBACION todavía no lo necesita: se asignará cuando
        // un administrador la confirme).
        val horasDeAnticipacion = Fechas.horasDesdeAhora(fecha, horaInicio)
        val debeAsignarMaterial = estadoInicial == EstadoReserva.CONFIRMADA &&
            horasDeAnticipacion >= Catalogos.MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS

        val herramientaId = herramientaPorArea[areaId]
        if (debeAsignarMaterial && herramientaId != null) {
            val herramienta = db.collection("herramientas").document(herramientaId).get().await()
            materialAsignado.add(
                mapOf(
                    "reservaId" to nuevaReserva.id,
                    "herramientaId" to herramientaId,
                    "herramientaNombre" to (herramienta.getString("nombre") ?: ""),
                    "areaId" to areaId,
                    "fecha" to fecha,
                    "cantidad" to 1L,
                    "estado" to "ASIGNADO",
                    "creadoEn" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        return nuevaReserva
    }

    override suspend fun obtenerReservasDeArea(areaId: String): List<Reserva> {
        return reservas.whereEqualTo("areaId", areaId).get().await()
            .documents.mapNotNull { it.toReserva() }
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

    override suspend fun cancelarReserva(reservaId: String): Boolean {
        val documento = reservas.document(reservaId).get().await()
        val reserva = documento.toReserva() ?: return true

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

    override suspend fun registrarNoShow(usuarioId: String) {
        val strikes = noShowsPorUsuario.getOrPut(usuarioId) { mutableListOf() }
        strikes.add(System.currentTimeMillis())
    }

    override suspend fun estaBloqueadoPorInasistencias(usuarioId: String): Boolean {
        val strikes = noShowsPorUsuario[usuarioId] ?: return false
        val ventanaMs = Catalogos.VENTANA_INASISTENCIAS_DIAS * 24L * 60 * 60 * 1000
        val recientes = strikes.filter { System.currentTimeMillis() - it <= ventanaMs }
        if (recientes.size < Catalogos.INASISTENCIAS_PARA_BLOQUEO) return false

        val ultimaFalta = recientes.max()
        val bloqueoMs = Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS * 24L * 60 * 60 * 1000
        return System.currentTimeMillis() - ultimaFalta <= bloqueoMs
    }
}
