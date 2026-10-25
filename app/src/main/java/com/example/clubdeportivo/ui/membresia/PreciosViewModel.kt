package com.example.clubdeportivo.ui.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.repository.PreciosRepository
import kotlinx.coroutines.launch
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

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
                _mensaje.value = Textos.get(R.string.the_updated_prices_could_not_be)
            }
        }
    }

    /** Los precios solo los cambia un administrador; el resto del personal, los socios y los titulares de un plan solo los consultan. */
    fun puedeEditar(): Boolean = SesionManager.usuarioActual?.rol?.esAdministrador() == true

    fun guardar(clave: String, precio: Double) {
        if (!puedeEditar()) {
            _mensaje.value = Textos.get(R.string.only_an_administrator_can_change_price)
            return
        }
        viewModelScope.launch {
            try {
                preciosRepository.guardar(clave, precio)
                _editados.value = _editados.value.orEmpty() + (clave to precio)
                _mensaje.value = Textos.get(R.string.price_updated)
            } catch (e: Exception) {
                _mensaje.value = Textos.get(R.string.the_price_could_not_be_saved)
            }
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }
}
