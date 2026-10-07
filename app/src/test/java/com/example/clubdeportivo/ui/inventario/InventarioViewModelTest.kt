package com.example.clubdeportivo.ui.inventario

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.repository.InventarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Inventario en memoria con los mismos artículos de ejemplo de siempre: así las pruebas no dependen de Firebase. */
private class InventarioFalso : InventarioRepository {
    private var siguiente = 100
    val guardado = mutableListOf(
        ArticuloInventario("1", "Raquetas de Tenis", "Tenis", 12, "🎾", 5),
        ArticuloInventario("2", "Pelotas de Tenis", "Tenis", 48, "🎾", 20),
        ArticuloInventario("4", "Balones de Básquetbol", "Básquetbol", 6, "🏀", 4),
        ArticuloInventario("5", "Balones de Fútbol", "Fútbol", 10, "⚽", 5),
        ArticuloInventario("6", "Gorros de Natación", "Natación", 20, "🏊", 10),
        ArticuloInventario("7", "Tablas de Natación", "Natación", 15, "🏊", 8),
        ArticuloInventario("8", "Redes de Básquetbol", "Básquetbol", 4, "🏀", 2)
    )

    override suspend fun obtenerArticulos() = guardado.toList()

    override suspend fun crear(nombre: String, deporte: String, cantidad: Int, stockMinimo: Int, icono: String): ArticuloInventario {
        val nuevo = ArticuloInventario("${siguiente++}", nombre, deporte, cantidad, icono, stockMinimo)
        guardado.add(nuevo)
        return nuevo
    }

    override suspend fun actualizar(articulo: ArticuloInventario) {
        val i = guardado.indexOfFirst { it.id == articulo.id }
        if (i != -1) guardado[i] = articulo
    }

    override suspend fun actualizarCantidad(id: String, cantidad: Int) {
        val i = guardado.indexOfFirst { it.id == id }
        if (i != -1) guardado[i] = guardado[i].copy(cantidad = cantidad)
    }

    override suspend fun eliminar(id: String) {
        guardado.removeAll { it.id == id }
    }
}

class InventarioViewModelTest {

    // Hace que MutableLiveData.setValue() funcione de forma síncrona fuera del hilo principal,
    // que es lo que se necesita para probar un ViewModel basado en LiveData desde un test JVM.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: InventarioViewModel
    private lateinit var repositorio: InventarioFalso

    @Before
    fun setUp() {
        repositorio = InventarioFalso()
        // Dispatchers.Unconfined ejecuta todo al instante, como si el guardado fuera síncrono.
        viewModel = InventarioViewModel(repositorio, CoroutineScope(Dispatchers.Unconfined)) { emptyList() }
    }

    @Test
    fun `el inventario inicial trae articulos de ejemplo`() {
        val articulos = viewModel.articulos.value
        assertTrue(articulos!!.isNotEmpty())
    }

    @Test
    fun `incrementarCantidad suma 1 a la cantidad del articulo`() {
        val articulo = viewModel.articulos.value!!.first()
        val cantidadOriginal = articulo.cantidad

        viewModel.incrementarCantidad(articulo)

        val actualizado = viewModel.articulos.value!!.first { it.id == articulo.id }
        assertEquals(cantidadOriginal + 1, actualizado.cantidad)
    }

    @Test
    fun `decrementarCantidad resta 1 a la cantidad del articulo`() {
        val articulo = viewModel.articulos.value!!.first()
        val cantidadOriginal = articulo.cantidad

        viewModel.decrementarCantidad(articulo)

        val actualizado = viewModel.articulos.value!!.first { it.id == articulo.id }
        assertEquals(cantidadOriginal - 1, actualizado.cantidad)
    }

    @Test
    fun `decrementarCantidad no baja de 0`() {
        viewModel.agregarArticulo("Silbatos", "Fútbol", 0, 1)
        val articulo = viewModel.articulos.value!!.first { it.nombre == "Silbatos" }

        viewModel.decrementarCantidad(articulo)

        val actualizado = viewModel.articulos.value!!.first { it.id == articulo.id }
        assertEquals(0, actualizado.cantidad)
    }

    @Test
    fun `los botones de mas y menos son simetricos y no pierden unidades`() {
        val articulo = viewModel.articulos.value!!.first()
        val cantidadOriginal = articulo.cantidad

        viewModel.incrementarCantidad(articulo)
        val trasIncrementar = viewModel.articulos.value!!.first { it.id == articulo.id }
        viewModel.decrementarCantidad(trasIncrementar)

        val actualizado = viewModel.articulos.value!!.first { it.id == articulo.id }
        assertEquals(cantidadOriginal, actualizado.cantidad)
    }

    @Test
    fun `agregarArticulo agrega un nuevo articulo y cierra el modal`() {
        val totalOriginal = viewModel.articulos.value!!.size
        viewModel.abrirModalAgregar()

        viewModel.agregarArticulo("Conos", "Fútbol", 10, 4)

        val articulos = viewModel.articulos.value!!
        assertEquals(totalOriginal + 1, articulos.size)
        val nuevo = articulos.last()
        assertEquals("Conos", nuevo.nombre)
        assertEquals("Fútbol", nuevo.deporte)
        assertEquals(10, nuevo.cantidad)
        assertEquals(4, nuevo.stockMinimo)
        assertFalse(viewModel.mostrarModal.value!!)
    }

    @Test
    fun `eliminarArticulo quita el articulo del inventario`() {
        val articulo = viewModel.articulos.value!!.first()
        val totalOriginal = viewModel.articulos.value!!.size

        viewModel.eliminarArticulo(articulo)

        val articulos = viewModel.articulos.value!!
        assertEquals(totalOriginal - 1, articulos.size)
        assertTrue(articulos.none { it.id == articulo.id })
    }

    @Test
    fun `eliminarArticulo sobre un id inexistente no cambia la lista`() {
        val original = viewModel.articulos.value!!
        val articuloInexistente = ArticuloInventario("no-existe", "X", "Fútbol", 1, "📦", 1)

        viewModel.eliminarArticulo(articuloInexistente)

        assertEquals(original, viewModel.articulos.value!!)
    }

    @Test
    fun `editarArticulo actualiza nombre deporte cantidad y stock minimo`() {
        val articulo = viewModel.articulos.value!!.first()

        viewModel.editarArticulo(articulo.id, "Nombre editado", "Natación", 99, 7)

        val actualizado = viewModel.articulos.value!!.first { it.id == articulo.id }
        assertEquals("Nombre editado", actualizado.nombre)
        assertEquals("Natación", actualizado.deporte)
        assertEquals(99, actualizado.cantidad)
        assertEquals(7, actualizado.stockMinimo)
        assertEquals("🏊", actualizado.icono)
        assertFalse(viewModel.mostrarModal.value!!)
    }

    @Test
    fun `abrirModalEditar deja el articulo disponible para el modal y cerrar lo limpia`() {
        val articulo = viewModel.articulos.value!!.first()

        viewModel.abrirModalEditar(articulo)
        assertTrue(viewModel.mostrarModal.value!!)
        assertEquals(articulo.id, viewModel.articuloEnEdicion.value!!.id)

        viewModel.cerrarModalAgregar()
        assertFalse(viewModel.mostrarModal.value!!)
        assertNull(viewModel.articuloEnEdicion.value)
    }

    @Test
    fun `filtrarPorDeporte solo devuelve articulos de ese deporte`() {
        viewModel.filtrarPorDeporte("Tenis")

        val filtrados = viewModel.obtenerArticulosFiltrados()
        assertTrue(filtrados.isNotEmpty())
        assertTrue(filtrados.all { it.deporte == "Tenis" })
    }

    @Test
    fun `filtrarPorDeporte Todos devuelve el inventario completo`() {
        viewModel.filtrarPorDeporte("Tenis")
        viewModel.filtrarPorDeporte("Todos")

        assertEquals(viewModel.articulos.value!!.size, viewModel.obtenerArticulosFiltrados().size)
    }

    @Test
    fun `cada operacion sobre el inventario queda registrada en el historial, la mas reciente primero`() {
        assertTrue(viewModel.historial.value!!.isEmpty())

        viewModel.agregarArticulo("Conos", "Fútbol", 10, 4)
        val nuevo = viewModel.articulos.value!!.last { it.nombre == "Conos" }
        viewModel.incrementarCantidad(nuevo)
        viewModel.eliminarArticulo(nuevo)

        val historial = viewModel.historial.value!!
        assertEquals(3, historial.size)
        assertTrue(historial[0].descripcion.contains("eliminó"))
        assertTrue(historial[1].descripcion.contains("+1"))
        assertTrue(historial[2].descripcion.contains("agregó"))
    }

    @Test
    fun `no permite agregar un articulo con nombre repetido`() {
        val cantidadAntes = viewModel.articulos.value!!.size

        val agregado = viewModel.agregarArticulo("  raquetas de tenis ", "Tenis", 3, 1)

        assertFalse(agregado)
        assertEquals(cantidadAntes, viewModel.articulos.value!!.size)
    }

    @Test
    fun `no permite renombrar un articulo con el nombre de otro`() {
        val primero = viewModel.articulos.value!![0]
        val segundo = viewModel.articulos.value!![1]

        val editado = viewModel.editarArticulo(primero.id, segundo.nombre, primero.deporte, primero.cantidad, primero.stockMinimo)

        assertFalse(editado)
        assertEquals(primero.nombre, viewModel.articulos.value!!.first { it.id == primero.id }.nombre)
    }

    @Test
    fun `editar un articulo conservando su propio nombre si se permite`() {
        val primero = viewModel.articulos.value!![0]

        assertTrue(viewModel.editarArticulo(primero.id, primero.nombre, primero.deporte, 99, primero.stockMinimo))
    }

    @Test
    fun `los cambios tambien se guardan en el repositorio`() {
        val articulo = viewModel.articulos.value!!.first()

        viewModel.incrementarCantidad(articulo)
        viewModel.agregarArticulo("Conos", "Fútbol", 10, 4)
        viewModel.eliminarArticulo(viewModel.articulos.value!!.first { it.id == "2" })

        assertEquals(articulo.cantidad + 1, repositorio.guardado.first { it.id == articulo.id }.cantidad)
        assertTrue(repositorio.guardado.any { it.nombre == "Conos" })
        assertTrue(repositorio.guardado.none { it.id == "2" })
    }
}
