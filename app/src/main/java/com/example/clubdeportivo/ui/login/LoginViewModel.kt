package com.example.clubdeportivo.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.launch

/**
 * ViewModel del login. No conoce Views ni Fragments: solo recibe datos,
 * valida, le pide al repositorio que inicie sesión y expone el resultado
 * mediante LiveData para que la Vista reaccione.
 */
class LoginViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _loginExitoso = MutableLiveData<Boolean>()
    val loginExitoso: LiveData<Boolean> = _loginExitoso

    private val _errorGeneral = MutableLiveData<String?>()
    val errorGeneral: LiveData<String?> = _errorGeneral

    fun login(email: String, password: String) {
        var esValido = true

        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Ingresá un correo electrónico válido"
            esValido = false
        } else {
            _emailError.value = null
        }

        if (password.isBlank() || password.length < 6) {
            _passwordError.value = "La contraseña debe tener al menos 6 caracteres"
            esValido = false
        } else {
            _passwordError.value = null
        }

        if (!esValido) return

        viewModelScope.launch {
            _cargando.value = true
            when (val resultado = authRepository.login(email, password)) {
                is Resultado.Exito -> {
                    SesionManager.iniciarSesion(resultado.datos)
                    _errorGeneral.value = null
                    _loginExitoso.value = true
                }
                is Resultado.Error -> {
                    _errorGeneral.value = resultado.mensaje
                }
            }
            _cargando.value = false
        }
    }

    fun onNavegacionCompletada() {
        _loginExitoso.value = false
    }
}
