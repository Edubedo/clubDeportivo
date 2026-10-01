package com.example.clubdeportivo.ui.inventario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.clubdeportivo.data.Deportes

data class ArticuloInventario(
    val id: String,
    val nombre: String,
    val deporte: String,
    val cantidad: Int,
    val icono: String,
    val stockMinimo: Int
)

data class ActividadInventario(
    val id: String,
    val descripcion: String,
    val timestamp: Long
)

/** Cuántas entradas de [ActividadInventario] se conservan como máximo en el historial. */
private const val MAX_ACTIVIDADES = 50

class InventarioViewModel : ViewModel() {

    private val _articulos = MutableLiveData<List<ArticuloInventario>>(
        listOf(
            ArticuloInventario("1", "Raquetas de Tenis", "Tenis", 12, "🎾", 5),
            ArticuloInventario("2", "Pelotas de Tenis", "Tenis", 48, "🎾", 20),
            ArticuloInventario("4", "Balones de Básquetbol", "Básquetbol", 6, "🏀", 4),
            ArticuloInventario("5", "Balones de Fútbol", "Fútbol", 10, "⚽", 5),
            ArticuloInventario("6", "Gorros de Natación", "Natación", 20, "🏊", 10),
            ArticuloInventario("7", "Tablas de Natación", "Natación", 15, "🏊", 8),
            ArticuloInventario("8", "Redes de Básquetbol", "Básquetbol", 4, "🏀", 2)
        )
    )
    val articulos: LiveData<List<ArticuloInventario>> = _articulos

    private val _filtroDeporte = MutableLiveData("Todos")
    val filtroDeporte: LiveData<String> = _filtroDeporte

    private val _mostrarModal = MutableLiveData(false)
    val mostrarModal: LiveData<Boolean> = _mostrarModal

    /** Artículo que se está editando en el modal, o null si el modal está en modo "agregar". */
    private val _articuloEnEdicion = MutableLiveData<ArticuloInventario?>(null)
    val articuloEnEdicion: LiveData<ArticuloInventario?> = _articuloEnEdicion

    private val _historial = MutableLiveData<List<ActividadInventario>>(emptyList())
    val historial: LiveData<List<ActividadInventario>> = _historial

    private val _deportes = listOf("Todos") + Deportes.predefinidos

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
        _articuloEnEdicion.value = null
        _mostrarModal.value = true
    }

    fun abrirModalEditar(articulo: ArticuloInventario) {
        _articuloEnEdicion.value = articulo
        _mostrarModal.value = true
    }

    fun cerrarModalAgregar() {
        _mostrarModal.value = false
        _articuloEnEdicion.value = null
    }

    /** true si ya hay otro artículo con ese nombre (sin distinguir mayúsculas ni espacios sobrantes). */
    fun existeNombre(nombre: String, excluirId: String? = null): Boolean {
        val buscado = nombre.trim()
        return _articulos.value.orEmpty().any { it.id != excluirId && it.nombre.trim().equals(buscado, ignoreCase = true) }
    }

    /** @return false (sin guardar nada) si el nombre ya existe en el inventario. */
    fun agregarArticulo(nombre: String, deporte: String, cantidadInicial: Int, stockMinimo: Int): Boolean {
        if (existeNombre(nombre)) return false
        val iconoDeporte = mapearIconoDeporte(deporte)
        val nuevoArticulo = ArticuloInventario(
            id = System.currentTimeMillis().toString(),
            nombre = nombre.trim(),
            deporte = deporte,
            cantidad = cantidadInicial,
            icono = iconoDeporte,
            stockMinimo = stockMinimo
        )
        val listaActual = _articulos.value?.toMutableList() ?: mutableListOf()
        listaActual.add(nuevoArticulo)
        _articulos.value = listaActual
        registrarActividad("Se agregó \"$nombre\" ($cantidadInicial uds.)")
        _mostrarModal.value = false
        _articuloEnEdicion.value = null
        return true
    }

    /**
     * Actualiza nombre, deporte, cantidad y stock mínimo de un artículo existente.
     * A diferencia de [incrementarCantidad]/[decrementarCantidad] (que solo tocan la
     * cantidad de a 1 en 1 desde la tarjeta), esto permite corregir cualquier campo desde
     * el modal de edición.
     */
    fun editarArticulo(id: String, nombre: String, deporte: String, cantidad: Int, stockMinimo: Int): Boolean {
        if (existeNombre(nombre, excluirId = id)) return false
        val listaActual = _articulos.value?.toMutableList() ?: return false
        val indice = listaActual.indexOfFirst { it.id == id }
        if (indice != -1) {
            listaActual[indice] = listaActual[indice].copy(
                nombre = nombre.trim(),
                deporte = deporte,
                cantidad = cantidad,
                icono = mapearIconoDeporte(deporte),
                stockMinimo = stockMinimo
            )
            _articulos.value = listaActual
            registrarActividad("Se editó \"$nombre\"")
        }
        _mostrarModal.value = false
        _articuloEnEdicion.value = null
        return true
    }

    fun eliminarArticulo(articulo: ArticuloInventario) {
        val listaActual = _articulos.value?.toMutableList() ?: return
        if (listaActual.removeAll { it.id == articulo.id }) {
            _articulos.value = listaActual
            registrarActividad("Se eliminó \"${articulo.nombre}\" del inventario")
        }
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
            val signo = if (nuevaCantidad > articulo.cantidad) "+1" else "-1"
            registrarActividad("${articulo.nombre}: $signo (ahora $nuevaCantidad uds.)")
        }
    }

    private fun registrarActividad(descripcion: String) {
        val actual = _historial.value ?: emptyList()
        val nueva = ActividadInventario(
            id = "${System.currentTimeMillis()}-${actual.size}",
            descripcion = descripcion,
            timestamp = System.currentTimeMillis()
        )
        _historial.value = (listOf(nueva) + actual).take(MAX_ACTIVIDADES)
    }

    private fun mapearIconoDeporte(deporte: String): String = Deportes.emojiDe(deporte).let { if (it == "🏆") "📦" else it }
}
