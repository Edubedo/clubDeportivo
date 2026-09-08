package com.example.clubdeportivo.ui.registro

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.launch

/**
 * Crea una cuenta real (Firebase Authentication + su perfil en Firestore). Reemplaza al viejo
 * "cualquier correo con 6+ caracteres entra" del login de prueba: ahora la cuenta tiene que
 * existir de verdad. El selector de rol queda visible para poder seguir probando cada pantalla
 * según el rol, igual que antes.
 */
class RegistroViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _nombreError = MutableLiveData<String?>()
    val nombreError: LiveData<String?> = _nombreError

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _registroExitoso = MutableLiveData(false)
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _errorGeneral = MutableLiveData<String?>()
    val errorGeneral: LiveData<String?> = _errorGeneral

    fun registrar(nombre: String, email: String, password: String, rol: Rol) {
        var esValido = true

        if (nombre.isBlank()) {
            _nombreError.value = "Ingresá tu nombre"
            esValido = false
        } else {
            _nombreError.value = null
        }

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
            when (val resultado = authRepository.registrar(nombre, email, password, rol)) {
                is Resultado.Exito -> {
                    SesionManager.iniciarSesion(resultado.datos)
                    _errorGeneral.value = null
                    _registroExitoso.value = true
                }
                is Resultado.Error -> {
                    _errorGeneral.value = resultado.mensaje
                }
            }
            _cargando.value = false
        }
    }
}
