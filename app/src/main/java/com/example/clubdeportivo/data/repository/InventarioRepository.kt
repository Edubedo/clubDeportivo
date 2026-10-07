package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.ArticuloInventario
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface InventarioRepository {
    suspend fun obtenerArticulos(): List<ArticuloInventario>
    suspend fun crear(nombre: String, deporte: String, cantidad: Int, stockMinimo: Int, icono: String): ArticuloInventario
    suspend fun actualizar(articulo: ArticuloInventario)
    suspend fun actualizarCantidad(id: String, cantidad: Int)
    suspend fun eliminar(id: String)
}

private fun DocumentSnapshot.toArticulo(): ArticuloInventario? {
    val nombre = getString("nombre") ?: return null
    return ArticuloInventario(
        id = id,
        nombre = nombre,
        deporte = getString("deporte").orEmpty(),
        cantidad = (getLong("cantidadTotal") ?: 0L).toInt(),
        icono = getString("icono").orEmpty().ifBlank { "📦" },
        stockMinimo = (getLong("stockMinimo") ?: 0L).toInt()
    )
}

/**
 * Inventario sobre la colección `herramientas` (la misma que usan las reservas para asignar material).
 * Como la app no lleva un registro de préstamos, la cantidad disponible siempre es igual a la total.
 */
class FirebaseInventarioRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : InventarioRepository {

    private val coleccion = db.collection("herramientas")

    override suspend fun obtenerArticulos(): List<ArticuloInventario> =
        coleccion.get().await().documents.mapNotNull { it.toArticulo() }.sortedBy { it.nombre.lowercase() }

    override suspend fun crear(nombre: String, deporte: String, cantidad: Int, stockMinimo: Int, icono: String): ArticuloInventario {
        val datos = mapOf(
            "nombre" to nombre,
            "deporte" to deporte,
            "areaIds" to emptyList<String>(),
            "icono" to icono,
            "cantidadTotal" to cantidad.toLong(),
            "cantidadDisponible" to cantidad.toLong(),
            "stockMinimo" to stockMinimo.toLong(),
            "estado" to "BUENO",
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        val documento = coleccion.add(datos).await()
        return ArticuloInventario(documento.id, nombre, deporte, cantidad, icono, stockMinimo)
    }

    override suspend fun actualizar(articulo: ArticuloInventario) {
        coleccion.document(articulo.id).update(
            mapOf(
                "nombre" to articulo.nombre,
                "deporte" to articulo.deporte,
                "icono" to articulo.icono,
                "cantidadTotal" to articulo.cantidad.toLong(),
                "cantidadDisponible" to articulo.cantidad.toLong(),
                "stockMinimo" to articulo.stockMinimo.toLong(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun actualizarCantidad(id: String, cantidad: Int) {
        coleccion.document(id).update(
            mapOf(
                "cantidadTotal" to cantidad.toLong(),
                "cantidadDisponible" to cantidad.toLong(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun eliminar(id: String) {
        coleccion.document(id).delete().await()
    }
}
