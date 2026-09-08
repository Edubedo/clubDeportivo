package com.example.clubdeportivo.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Usuario

class HomeViewModel : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    init {
        _usuario.value = SesionManager.usuarioActual
    }
}
