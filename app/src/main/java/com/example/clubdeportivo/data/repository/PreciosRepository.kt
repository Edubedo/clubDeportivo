package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.util.Fechas
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Precios que el administrador editó, por clave (ver ClavesPrecio). Lo que no esté aquí usa el precio base del catálogo. */
interface PreciosRepository {
    suspend fun obtenerEditados(): Map<String, Double>
    suspend fun guardar(clave: String, precio: Double)
}

class FirebasePreciosRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PreciosRepository {

    private val coleccion = db.collection("precios")

    override suspend fun obtenerEditados(): Map<String, Double> =
        coleccion.get().await().documents.mapNotNull { doc -> doc.getDouble("precio")?.let { doc.id to it } }.toMap()

    override suspend fun guardar(clave: String, precio: Double) {
        coleccion.document(clave).set(mapOf("precio" to precio, "actualizado" to Fechas.hoy())).await()
    }
}
