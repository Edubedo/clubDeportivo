package com.example.clubdeportivo.ui.inventario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class ArticuloInventario(
    val id: String,
    val nombre: String,
    val deporte: String,
    val cantidad: Int,
    val icono: String,
    val stockMinimo: Int
)

class InventarioViewModel : ViewModel() {

    private val _articulos = MutableLiveData<List<ArticuloInventario>>(
        listOf(
            ArticuloInventario("1", "Raquetas de Tenis", "Tenis", 12, "🎾", 5),
            ArticuloInventario("2", "Pelotas de Tenis", "Tenis", 48, "🎾", 20),
            ArticuloInventario("3", "Balones de Baloncesto", "Baloncesto", 8, "🏀", 3),
            ArticuloInventario("4", "Balones de Voleibol", "Voleibol", 6, "🏐", 4),
            ArticuloInventario("5", "Balones de Fútbol", "Fútbol", 10, "⚽", 5),
            ArticuloInventario("6", "Gorros de Natación", "Natación", 20, "🏊", 10),
            ArticuloInventario("7", "Tablas de Natación", "Natación", 15, "🏊", 8),
            ArticuloInventario("8", "Redes de Voleibol", "Voleibol", 4, "🏐", 2)
        )
    )
    val articulos: LiveData<List<ArticuloInventario>> = _articulos

    private val _filtroDeporte = MutableLiveData("Todos")
    val filtroDeporte: LiveData<String> = _filtroDeporte

    private val _mostrarModal = MutableLiveData(false)
    val mostrarModal: LiveData<Boolean> = _mostrarModal

    private val _deportes = listOf("Todos", "Baloncesto", "Voleibol", "Tenis", "Fútbol", "Natación")

    fun obtenerDeportes(): List<String> = _deportes

    fun filtrarPorDeporte(deporte: String) {
        _filtroDeporte.value = deporte
    }

    fun obtenerArticulosFiltrados(): List<ArticuloInventario> {
        val deporte = _filtroDeporte.value ?: "Todos"
        return if (deporte == "Todos") {
            _articulos.value ?: emptyList()
        } else {
            _articulos.value?.filter { it.deporte == deporte } ?: emptyList()
        }
    }

    fun abrirModalAgregar() {
        _mostrarModal.value = true
    }

    fun cerrarModalAgregar() {
        _mostrarModal.value = false
    }

    fun agregarArticulo(nombre: String, deporte: String, cantidadInicial: Int, stockMinimo: Int) {
        val iconoDeporte = mapearIconoDeporte(deporte)
        val nuevoArticulo = ArticuloInventario(
            id = System.currentTimeMillis().toString(),
            nombre = nombre,
            deporte = deporte,
            cantidad = cantidadInicial,
            icono = iconoDeporte,
            stockMinimo = stockMinimo
        )
        val listaActual = _articulos.value?.toMutableList() ?: mutableListOf()
        listaActual.add(nuevoArticulo)
        _articulos.value = listaActual
        _mostrarModal.value = false
    }

    fun incrementarCantidad(articulo: ArticuloInventario) {
        actualizarCantidad(articulo, articulo.cantidad + 1)
    }

    fun decrementarCantidad(articulo: ArticuloInventario) {
        if (articulo.cantidad > 0) {
            actualizarCantidad(articulo, articulo.cantidad - 1)
        }
    }

    private fun actualizarCantidad(articulo: ArticuloInventario, nuevaCantidad: Int) {
        val listaActual = _articulos.value?.toMutableList() ?: return
        val indice = listaActual.indexOfFirst { it.id == articulo.id }
        if (indice != -1) {
            listaActual[indice] = articulo.copy(cantidad = nuevaCantidad)
            _articulos.value = listaActual
        }
    }

    private fun mapearIconoDeporte(deporte: String): String = when (deporte) {
        "Tenis" -> "🎾"
        "Baloncesto" -> "🏀"
        "Fútbol" -> "⚽"
        "Voleibol" -> "🏐"
        "Natación" -> "🏊"
        else -> "📦"
    }
}
