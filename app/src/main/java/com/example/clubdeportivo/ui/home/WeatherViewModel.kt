package com.example.clubdeportivo.ui.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.WeatherData
import com.example.clubdeportivo.util.Textos
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val data: WeatherData) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel : ViewModel() {
    private val repository = AppContainer.weatherRepository

    var uiState by mutableStateOf<WeatherUiState>(WeatherUiState.Loading)
        private set

    init {
        cargar()
    }

    fun reintentar() {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            uiState = WeatherUiState.Loading
            try {
                uiState = WeatherUiState.Success(repository.obtenerClimaActual())
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo obtener el clima actual", e)
                uiState = WeatherUiState.Error(Textos.get("weather_error"))
            }
        }
    }

    private companion object {
        const val TAG = "WeatherViewModel"
    }
}
