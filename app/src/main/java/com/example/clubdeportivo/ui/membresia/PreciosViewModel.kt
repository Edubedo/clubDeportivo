package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.repository.PreciosRepository
import kotlinx.coroutines.launch

/** Precios editados por el administrador (sobre los precios base del catálogo). */
class PreciosViewModel(
    private val preciosRepository: PreciosRepository = AppContainer.preciosRepository
) : ViewModel() {

    private val _editados = MutableLiveData<Map<String, Double>>(emptyMap())
    val editados: LiveData<Map<String, Double>> = _editados

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                _editados.value = preciosRepository.obtenerEditados()
            } catch (e: Exception) {
                _mensaje.value = "No se pudieron cargar los precios actualizados; se muestran los precios base."
            }
        }
    }

    fun guardar(clave: String, precio: Double) {
        viewModelScope.launch {
            try {
                preciosRepository.guardar(clave, precio)
                _editados.value = _editados.value.orEmpty() + (clave to precio)
                _mensaje.value = "Precio actualizado."
            } catch (e: Exception) {
                _mensaje.value = "No se pudo guardar el precio."
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
