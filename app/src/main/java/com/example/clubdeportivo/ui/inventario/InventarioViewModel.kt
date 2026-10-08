package com.example.clubdeportivo.ui.inventario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.data.repository.InventarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ActividadInventario(
    val id: String,
    val descripcion: String,
    val timestamp: Long
)

/** Cuántas entradas de [ActividadInventario] se conservan como máximo en el historial. */
private const val MAX_ACTIVIDADES = 50

/**
 * Inventario del club. Los cambios se ven al instante en pantalla y se guardan en Firestore; si el guardado falla,
 * se vuelve a leer el inventario real para no mostrar algo que no quedó guardado.
 *
 * [alcance] permite a las pruebas ejecutar todo de forma inmediata; en la app se usa el del ViewModel.
 */
class InventarioViewModel(
    private val repositorio: InventarioRepository = AppContainer.inventarioRepository,
    private val alcance: CoroutineScope? = null,
    /** Usuario que está viendo el inventario: un encargado solo ve (y agrega) lo de su área. */
    private val usuario: () -> Usuario? = { SesionManager.usuarioActual },
    /** Deportes de las áreas del club (para ofrecer también los que se dieron de alta después). */
    private val deportesDelClub: suspend () -> List<String> = {
        runCatching { AppContainer.areaRepository.obtenerAreas().map { it.tipo.trim() } }.getOrDefault(emptyList())
    }
) : ViewModel() {

    private val scope: CoroutineScope get() = alcance ?: viewModelScope

    private val _articulos = MutableLiveData<List<ArticuloInventario>>(emptyList())
    val articulos: LiveData<List<ArticuloInventario>> = _articulos

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> = _cargando

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _filtroDeporte = MutableLiveData("Todos")
    val filtroDeporte: LiveData<String> = _filtroDeporte

    private val _mostrarModal = MutableLiveData(false)
    val mostrarModal: LiveData<Boolean> = _mostrarModal

    /** Artículo que se está editando en el modal, o null si el modal está en modo "agregar". */
    private val _articuloEnEdicion = MutableLiveData<ArticuloInventario?>(null)
    val articuloEnEdicion: LiveData<ArticuloInventario?> = _articuloEnEdicion

    private val _historial = MutableLiveData<List<ActividadInventario>>(emptyList())
    val historial: LiveData<List<ActividadInventario>> = _historial

    private val _deportes = MutableLiveData(listOf("Todos") + Deportes.predefinidos)
    val deportes: LiveData<List<String>> = _deportes

    init {
        cargar()
    }

    fun cargar() {
        scope.launch {
            _cargando.value = true
            try {
                val actual = usuario()
                _articulos.value = repositorio.obtenerArticulos().filter { actual.puedeVerDeporte(it.deporte) }
                val propios = _articulos.value.orEmpty().map { it.deporte }
                _deportes.value = if (actual?.rol?.esEncargado() == true) {
                    // Un encargado solo trabaja con el deporte de su área: ni filtro "Todos" ni otros deportes.
                    listOfNotNull(actual.areaTrabajo?.trim()?.takeIf { it.isNotEmpty() })
                } else {
                    listOf("Todos") +
                        (Deportes.predefinidos + deportesDelClub() + propios).filter { it.isNotBlank() }.distinct()
                }
                if (actual?.rol?.esEncargado() == true) _filtroDeporte.value = _deportes.value.orEmpty().firstOrNull() ?: "Todos"
            } catch (e: Exception) {
                _mensaje.value = "No se pudo cargar el inventario. Revisa tu conexión."
            }
            _cargando.value = false
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    fun obtenerDeportes(): List<String> = _deportes.value.orEmpty()

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
        val icono = mapearIconoDeporte(deporte)
        _mostrarModal.value = false
        _articuloEnEdicion.value = null
        scope.launch {
            try {
                val creado = repositorio.crear(nombre.trim(), deporte, cantidadInicial, stockMinimo, icono)
                _articulos.value = (_articulos.value.orEmpty() + creado)
                registrarActividad("Se agregó \"${nombre.trim()}\" ($cantidadInicial uds.)")
            } catch (e: Exception) {
                _mensaje.value = "No se pudo agregar el artículo. Intenta de nuevo."
            }
        }
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
            val editado = listaActual[indice].copy(
                nombre = nombre.trim(),
                deporte = deporte,
                cantidad = cantidad,
                icono = mapearIconoDeporte(deporte),
                stockMinimo = stockMinimo
            )
            listaActual[indice] = editado
            _articulos.value = listaActual
            registrarActividad("Se editó \"${nombre.trim()}\"")
            guardar { repositorio.actualizar(editado) }
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
            guardar { repositorio.eliminar(articulo.id) }
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
            guardar { repositorio.actualizarCantidad(articulo.id, nuevaCantidad) }
        }
    }

    /** Guarda en Firestore lo que ya se ve en pantalla; si falla, avisa y vuelve a leer lo que realmente quedó guardado. */
    private fun guardar(operacion: suspend () -> Unit) {
        scope.launch {
            try {
                operacion()
            } catch (e: Exception) {
                _mensaje.value = "No se pudo guardar el cambio. Se restauró el inventario."
                try {
                    _articulos.value = repositorio.obtenerArticulos().filter { usuario().puedeVerDeporte(it.deporte) }
                } catch (_: Exception) { }
            }
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
