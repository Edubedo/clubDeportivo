package com.example.clubdeportivo.ui.areas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.repository.AreaRepository
import kotlinx.coroutines.launch

class AreasViewModel(
    private val areaRepository: AreaRepository = AppContainer.areaRepository
) : ViewModel() {

    private val _areas = MutableLiveData<List<Area>>()
    val areas: LiveData<List<Area>> = _areas

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    init {
        cargarAreas()
    }

    fun cargarAreas() {
        viewModelScope.launch {
            _cargando.value = true
            _areas.value = areaRepository.obtenerAreas()
            _cargando.value = false
        }
    }
}
