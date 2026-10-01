package com.example.clubdeportivo.ui.admin.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.MigracionEsquema
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AdminHomeViewModel : ViewModel() {

    private val _tabActiva = MutableLiveData("Dashboard")
    val tabActiva: LiveData<String> = _tabActiva

    /** Totales reales (Firestore) para las tarjetas del dashboard; "–" mientras cargan o si falla. */
    private val _totalAreas = MutableLiveData("–")
    val totalAreas: LiveData<String> = _totalAreas

    private val _totalPersonal = MutableLiveData("–")
    val totalPersonal: LiveData<String> = _totalPersonal

    private val _totalReservasHoy = MutableLiveData("–")
    val totalReservasHoy: LiveData<String> = _totalReservasHoy

    private val _totalMiembros = MutableLiveData("–")
    val totalMiembros: LiveData<String> = _totalMiembros

    init {
        cargarTotales()
    }

    fun seleccionarTab(tab: String) {
        _tabActiva.value = tab
    }

    private fun cargarTotales() {
        viewModelScope.launch {
            // Primero se deja la base en la estructura actual (solo hace algo la primera vez que entra el personal).
            try {
                MigracionEsquema.asegurar(FirebaseFirestore.getInstance())
            } catch (e: Exception) {
                Log.w("Esquema", "No se pudo migrar la estructura de la base de datos", e)
            }
            try {
                _totalAreas.value = AppContainer.areaRepository.obtenerAreas().size.toString()
            } catch (_: Exception) { }
            try {
                val hoy = Fechas.hoy()
                // Personas con una membresía activa y vigente (cuenta a cada integrante de un paquete familiar).
                _totalMiembros.value = AppContainer.gestionMembresiasRepository.obtenerTodas()
                    .filter { ReglasMembresia.estadoEfectivo(it.membresia, hoy) == EstadoMembresia.ACTIVA }
                    .sumOf { it.personas.size }.toString()
            } catch (_: Exception) { }
            try {
                val hoy = Fechas.hoy()
                _totalReservasHoy.value = AppContainer.reservaRepository.obtenerReservasVigentes()
                    .count { it.fecha == hoy }.toString()
            } catch (_: Exception) { }
            try {
                _totalPersonal.value = AppContainer.personalRepository.contarPersonal().toString()
            } catch (_: Exception) { }
        }
    }
}
