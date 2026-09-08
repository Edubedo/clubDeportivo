package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.FirebaseSeeder
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Traduce un documento de la colección "areas" al mismo `Area` que ya usa toda la app. */
private fun DocumentSnapshot.toArea(): Area? {
    val nombre = getString("nombre") ?: return null
    return Area(
        id = id,
        nombre = nombre,
        tipo = getString("tipo") ?: "",
        capacidad = (getLong("capacidad") ?: 0).toInt(),
        disponibilidad = DisponibilidadArea.valueOf(getString("disponibilidad") ?: "DISPONIBLE"),
        permiteExternos = getBoolean("permiteExternos") ?: false
    )
}

class FirebaseAreaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AreaRepository {

    private val coleccion = db.collection("areas")

    override suspend fun obtenerAreas(): List<Area> {
        FirebaseSeeder.asegurarAreas(db)
        return coleccion.get().await().documents.mapNotNull { it.toArea() }
    }

    override suspend fun obtenerAreaPorId(id: String): Area? {
        return coleccion.document(id).get().await().toArea()
    }
}
