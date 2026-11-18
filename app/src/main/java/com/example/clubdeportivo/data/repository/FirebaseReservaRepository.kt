package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.ModificacionReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasReserva
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import com.example.clubdeportivo.util.Textos

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
        carril = getLong("carril")?.toInt(),

        usuarioNombre = getString("usuarioNombre") ?: "",
        areaNombre = getString("areaNombre") ?: "",
        deporte = getString("deporte") ?: "",
        modificacion = (get("modificacion") as? Map<*, *>)?.let { m ->
            val fecha = m["fecha"] as? String ?: return@let null
            ModificacionReserva(
                fecha = fecha,
                horaInicio = m["horaInicio"] as? String ?: "",
                horaFin = m["horaFin"] as? String ?: "",
                personas = (m["personas"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
                carril = (m["carril"] as? Number)?.toInt()
            )
        },
        canceladaPorMantenimiento = getBoolean("canceladaPorMantenimiento") ?: false,
        mantenimientoDesde = getString("mantenimientoDesde") ?: "",
        mantenimientoHasta = getString("mantenimientoHasta") ?: "",
        reagendada = getBoolean("reagendada") ?: false
    )
}

/** Otra persona se quedó con el último lugar al mismo tiempo: la reserva recién creada se canceló sola. */
class CupoAgotadoException(mensaje: String) : IllegalStateException(mensaje)

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
        return ReglasReserva.contarReservasActivasFuturas(
            usuarioId = usuarioId,
            reservas = obtenerReservasDeUsuario(usuarioId)
        )
    }

    override suspend fun crearReserva(
        usuarioId: String,
        areaId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        esExterno: Boolean,
        personas: Int,
        carril: Int?,
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
            "carril" to carril?.toLong(),
            "estado" to estadoInicial.name,
            "esExterno" to esExterno,
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        val documento = reservas.add(datos).await()
        verificarCupoTrasCrear(documento.id, areaId, (area.getLong("capacidad") ?: 0L).toInt(), fecha, horaInicio, horaFin, carril)
        val nuevaReserva = Reserva(
            documento.id, usuarioId, areaId, fecha, horaInicio, horaFin, estadoInicial, esExterno, personas,
            usuarioNombre = persona.getString("nombre") ?: "",
            areaNombre = area.getString("nombre") ?: "",
            deporte = area.getString("tipo") ?: "",
            carril = carril
        )

        // El material solo se asigna si la reserva queda CONFIRMADA (una pendiente de aprobación todavía no lo
        // necesita: se aparta cuando el encargado la aprueba).
        if (estadoInicial == EstadoReserva.CONFIRMADA) asignarMaterial(nuevaReserva)

        return nuevaReserva
    }

    /**
     * Firestore no permite leer-y-escribir el cupo en una sola transacción (no hay consultas dentro de ellas), así que dos
     * personas que reservan la última plaza a la vez podían pasarse del cupo. Tras crear la reserva se vuelve a leer el
     * servidor y se ordenan las reservas del horario por orden de llegada (marca de tiempo del servidor y, de empate, el id):
     * si la nuestra ya no cabe —o su carril lo ganó otra persona antes—, se cancela y se avisa. Las dos apps llegan a la
     * misma conclusión, así que gana solo la primera.
     */
    private suspend fun verificarCupoTrasCrear(
        reservaId: String, areaId: String, capacidad: Int, fecha: String,
        horaInicio: String, horaFin: String, carril: Int?
    ) {
        val inicio = ReglasReserva.aMinutos(horaInicio) ?: return
        val fin = ReglasReserva.aMinutos(horaFin) ?: return
        val delArea = try {
            reservas.whereEqualTo("areaId", areaId).get(Source.SERVER).await().documents
        } catch (e: Exception) {
            return // sin conexión al servidor no se puede comprobar; la reserva queda como está
        }
        class Llegada(val id: String, val llegada: Long, val reserva: Reserva)
        val orden = delArea
            .filter { it.getString("fecha") == fecha }
            .mapNotNull { doc -> doc.toReserva()?.let { Llegada(doc.id, doc.getTimestamp("creadoEn")?.toDate()?.time ?: Long.MAX_VALUE, it) } }
            .filter { ReglasReserva.esVigente(it.reserva) }
            .filter { llegada ->
                val i = ReglasReserva.aMinutos(llegada.reserva.horaInicio) ?: return@filter false
                val f = ReglasReserva.aMinutos(llegada.reserva.horaFin) ?: return@filter false
                i < fin && inicio < f
            }
            .sortedWith(compareBy({ it.llegada }, { it.id }))
        val posicion = orden.indexOfFirst { it.id == reservaId }
        if (posicion < 0) return
        val anteriores = orden.take(posicion)

        var sinCupo = false
        for (hora in (inicio / 60) until (fin / 60)) {
            val h0 = hora * 60
            val ocupadas = (anteriores + orden[posicion]).filter {
                val i = ReglasReserva.aMinutos(it.reserva.horaInicio) ?: return@filter false
                val f = ReglasReserva.aMinutos(it.reserva.horaFin) ?: return@filter false
                i < h0 + 60 && h0 < f
            }.sumOf { it.reserva.personas }
            if (ocupadas > capacidad) { sinCupo = true; break }
        }
        val carrilGanado = carril != null && anteriores.any { it.reserva.carril == carril }
        if (!sinCupo && !carrilGanado) return

        android.util.Log.w(
            "VerificarCupo",
            "Reserva $reservaId cancelada: sinCupo=$sinCupo carrilGanado=$carrilGanado capacidad=$capacidad " +
                "area=$areaId fecha=$fecha $horaInicio-$horaFin personas=${orden[posicion].reserva.personas} carril=$carril " +
                "anteriores=" + anteriores.joinToString { "${it.id}[${it.reserva.estado} ${it.reserva.horaInicio}-${it.reserva.horaFin} p=${it.reserva.personas} c=${it.reserva.carril}]" }
        )

        reservas.document(reservaId).update(
            mapOf(
                "estado" to EstadoReserva.CANCELADA.name,
                "canceladaEn" to FieldValue.serverTimestamp(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        throw CupoAgotadoException(Textos.get("espera_cupo_lleno_ahora"))
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
        val reserva = reservas.document(reservaId).get().await().toReserva() ?: error(Textos.get("the_booking_no_longer_exists"))
        if (!com.example.clubdeportivo.data.SesionManager.usuarioActual.puedeVerDeporte(reserva.deporte)) {
            error(Textos.get("this_booking_belongs_to_another_area"))
        }
        if (reserva.estado != EstadoReserva.PENDIENTE_APROBACION) {
            error(Textos.get("this_booking_is_no_longer_under", reserva.estado.name.lowercase().replace('_', ' ')))
        }
        return reserva
    }

    override suspend fun aprobarReserva(reservaId: String) {
        val reserva = reservaPorResolver(reservaId)
        if (Fechas.horasDesdeAhora(reserva.fecha, reserva.horaFin) <= 0) error(Textos.get("this_booking_has_already_ended_it"))
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

    override suspend fun rechazarReserva(
        reservaId: String,
        motivo: String
    ) {
        reservaPorResolver(reservaId)

        reservas.document(reservaId).update(
            mapOf(
                "estado" to EstadoReserva.RECHAZADA.name,
                "motivoRechazo" to motivo.trim(),
                "rechazadaEn" to FieldValue.serverTimestamp(),
                "rechazadaPor" to (
                        com.example.clubdeportivo.data.SesionManager
                            .usuarioActual?.id ?: ""
                        ),
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

    override suspend fun obtenerReservasEntre(desde: String, hasta: String): List<Reserva> =
        reservas.whereGreaterThanOrEqualTo("fecha", desde).whereLessThanOrEqualTo("fecha", hasta)
            .get().await().documents.mapNotNull { it.toReserva() }

    override suspend fun obtenerAsistencias(): Map<String, String> =
        checkins.get().await().documents.mapNotNull { doc -> doc.getString("asistencia")?.let { doc.id to it } }.toMap()

    override suspend fun obtenerReservasConAsistencia(usuarioId: String): Set<String> =
        checkins.whereEqualTo("usuarioId", usuarioId).get().await().documents.map { it.id }.toSet()

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
                // Si había un cambio pedido, ya no tiene sentido.
                "modificacion" to FieldValue.delete(),
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

    override suspend fun solicitarModificacion(reservaId: String, modificacion: ModificacionReserva) {
        reservas.document(reservaId).update(
            mapOf(
                "modificacion" to mapOf(
                    "fecha" to modificacion.fecha,
                    "horaInicio" to modificacion.horaInicio,
                    "horaFin" to modificacion.horaFin,
                    "personas" to modificacion.personas.toLong(),
                    "carril" to modificacion.carril?.toLong(),
                    "solicitadaEn" to FieldValue.serverTimestamp()
                ),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun retirarModificacion(reservaId: String) {
        reservas.document(reservaId).update(
            mapOf("modificacion" to FieldValue.delete(), "actualizadoEn" to FieldValue.serverTimestamp())
        ).await()
    }

    /** Lee una reserva con un cambio por resolver y comprueba que quien responde puede hacerlo (el encargado, solo en su área). */
    private suspend fun reservaConCambioPorResolver(reservaId: String): Pair<Reserva, ModificacionReserva> {
        val reserva = reservas.document(reservaId).get().await().toReserva() ?: error(Textos.get("the_booking_no_longer_exists"))
        if (!com.example.clubdeportivo.data.SesionManager.usuarioActual.puedeVerDeporte(reserva.deporte)) {
            error(Textos.get("this_booking_belongs_to_another_area"))
        }
        val cambio = reserva.modificacion
        if (reserva.estado != EstadoReserva.CONFIRMADA || cambio == null) error(Textos.get("cambio_reserva_ya_resuelto"))
        return reserva to cambio
    }

    override suspend fun aprobarModificacion(reservaId: String, esperada: ModificacionReserva) {
        val (reserva, cambio) = reservaConCambioPorResolver(reservaId)
        // El miembro pudo retirar o cambiar su solicitud después de que quien aprueba la vio.
        if (cambio != esperada) error(Textos.get("cambio_reserva_ya_resuelto"))
        val horas = ((ReglasReserva.aMinutos(cambio.horaFin) ?: 0) - (ReglasReserva.aMinutos(cambio.horaInicio) ?: 0)) / 60
        reservas.document(reservaId).update(
            mapOf(
                "fecha" to cambio.fecha,
                "horaInicio" to cambio.horaInicio,
                "horaFin" to cambio.horaFin,
                "duracionHoras" to horas.toLong(),
                "personas" to cambio.personas.toLong(),
                "carril" to (cambio.carril ?: reserva.carril)?.toLong(),
                "modificacion" to FieldValue.delete(),
                "resolucionModificacion" to mapOf(
                    "resultado" to "APROBADA",
                    "resueltaPor" to (com.example.clubdeportivo.data.SesionManager.usuarioActual?.id ?: ""),
                    "resueltaEn" to FieldValue.serverTimestamp()
                ),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        // El material apartado sigue a la reserva a su nuevo día.
        val asignado = materialAsignado.whereEqualTo("reservaId", reservaId).get().await()
        if (!asignado.isEmpty) {
            val batch = db.batch()
            asignado.documents.forEach { batch.update(it.reference, "fecha", cambio.fecha) }
            batch.commit().await()
        }
    }

    override suspend fun rechazarModificacion(reservaId: String, motivo: String) {
        reservaConCambioPorResolver(reservaId)
        reservas.document(reservaId).update(
            mapOf(
                "modificacion" to FieldValue.delete(),
                "resolucionModificacion" to mapOf(
                    "resultado" to "RECHAZADA",
                    "motivo" to motivo.trim(),
                    "resueltaPor" to (com.example.clubdeportivo.data.SesionManager.usuarioActual?.id ?: ""),
                    "resueltaEn" to FieldValue.serverTimestamp()
                ),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun cancelarPorMantenimiento(reservas: List<Reserva>, desde: String, hasta: String, motivo: String) {
        // Firestore admite 500 operaciones por lote: se parte en tandas.
        reservas.chunked(200).forEach { tanda ->
            val batch = db.batch()
            tanda.forEach { reserva ->
                batch.update(
                    this.reservas.document(reserva.id),
                    mapOf(
                        "estado" to EstadoReserva.CANCELADA.name,
                        "canceladaEn" to FieldValue.serverTimestamp(),
                        "sinPenalizacion" to true,
                        "canceladaPorMantenimiento" to true,
                        "mantenimientoDesde" to desde,
                        "mantenimientoHasta" to hasta,
                        "mantenimientoMotivo" to motivo.trim(),
                        "modificacion" to FieldValue.delete(),
                        "actualizadoEn" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            // El material que se había apartado ya no hace falta.
            tanda.forEach { reserva ->
                val asignado = materialAsignado.whereEqualTo("reservaId", reserva.id).get().await()
                if (!asignado.isEmpty) {
                    val lote = db.batch()
                    asignado.documents.forEach { lote.update(it.reference, "estado", "CANCELADO") }
                    lote.commit().await()
                }
            }
        }
    }

    override suspend fun marcarReagendada(reservaId: String) {
        reservas.document(reservaId).update(
            mapOf("reagendada" to true, "actualizadoEn" to FieldValue.serverTimestamp())
        ).await()
    }
}
