package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Traduce un documento de la colección "areas" al mismo `Area` que ya usa toda la app. */
private fun DocumentSnapshot.toArea(): Area? {
    val nombre = getString("nombre") ?: return null
    val desde = getString("mantenimientoDesde") ?: ""
    val hasta = getString("mantenimientoHasta") ?: ""
    var disponibilidad = DisponibilidadArea.valueOf(getString("disponibilidad") ?: "DISPONIBLE")
    // Un mantenimiento con periodo que ya terminó deja el área disponible sin que nadie tenga que volver a marcarla.
    if (disponibilidad == DisponibilidadArea.MANTENIMIENTO && hasta.isNotBlank() && hasta < Fechas.hoy()) {
        disponibilidad = DisponibilidadArea.DISPONIBLE
    }
    val enMantenimiento = disponibilidad == DisponibilidadArea.MANTENIMIENTO
    return Area(
        id = id,
        nombre = nombre,
        tipo = getString("tipo") ?: "",
        capacidad = (getLong("capacidad") ?: 0).toInt(),
        disponibilidad = disponibilidad,
        permiteExternos = getBoolean("permiteExternos") ?: false,
        emoji = getString("emoji") ?: "",
        carriles = (getLong("carriles") ?: 0L).toInt(),
        imagenUrl = getString("imagenUrl") ?: "",
        mantenimientoDesde = if (enMantenimiento) desde else "",
        mantenimientoHasta = if (enMantenimiento) hasta else "",
        mantenimientoMotivo = if (enMantenimiento) getString("mantenimientoMotivo") ?: "" else ""
    )
}

class FirebaseAreaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AreaRepository {

    private val coleccion = db.collection("areas")

    override suspend fun obtenerAreas(): List<Area> {
        return coleccion.get().await().documents.mapNotNull { it.toArea() }
    }

    override suspend fun obtenerAreaPorId(id: String): Area? {
        return coleccion.document(id).get().await().toArea()
    }

    override suspend fun crearArea(
        nombre: String,
        tipo: String,
        capacidad: Int,
        emoji: String,
        carriles: Int,
        imagenUrl: String
    ): Area {
        val datos = mapOf(
            "nombre" to nombre,
            "tipo" to tipo,
            "capacidad" to capacidad.toLong(),
            "disponibilidad" to DisponibilidadArea.DISPONIBLE.name,
            "permiteExternos" to false,
            "emoji" to emoji,
            "carriles" to carriles.toLong(),
            "imagenUrl" to imagenUrl,
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        val documento = coleccion.add(datos).await()
        return Area(
            documento.id,
            nombre,
            tipo,
            capacidad,
            DisponibilidadArea.DISPONIBLE,
            false,
            emoji,
            carriles,
            imagenUrl = imagenUrl
        )
    }

    override suspend fun actualizarArea(area: Area): Area {
        coleccion.document(area.id).update(
            mapOf(
                "nombre" to area.nombre,
                "tipo" to area.tipo,
                "capacidad" to area.capacidad.toLong(),
                "disponibilidad" to area.disponibilidad.name,
                "emoji" to area.emoji,
                "carriles" to area.carriles.toLong(),
                "imagenUrl" to area.imagenUrl,
                // Fuera de mantenimiento el periodo se borra; en mantenimiento se guarda el que se eligió.
                "mantenimientoDesde" to if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) area.mantenimientoDesde else "",
                "mantenimientoHasta" to if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) area.mantenimientoHasta else "",
                "mantenimientoMotivo" to if (area.disponibilidad == DisponibilidadArea.MANTENIMIENTO) area.mantenimientoMotivo else "",
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        return area
    }

    override suspend fun eliminarArea(id: String) {
        // El horario del área no tiene sentido sin ella: se borra junto (el historial de reservas y torneos
        // conserva el nombre del área guardado en cada registro).
        val batch = db.batch()
        batch.delete(coleccion.document(id))
        db.collection("restriccionesHorario").whereEqualTo("areaId", id).get().await().documents
            .forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
