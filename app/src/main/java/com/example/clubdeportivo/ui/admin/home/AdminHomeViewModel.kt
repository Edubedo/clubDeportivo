package com.example.clubdeportivo.ui.admin.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdminHomeViewModel : ViewModel() {

    private val _tabActiva = MutableLiveData("Dashboard")
    val tabActiva: LiveData<String> = _tabActiva

    fun seleccionarTab(tab: String) {
        _tabActiva.value = tab
    }
}
