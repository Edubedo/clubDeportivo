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
 * ViewModel del login (correo y contraseña). No conoce la pantalla: recibe datos, valida, le pide al
 * repositorio que inicie sesión y expone el resultado mediante LiveData.
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

    /** Avisos informativos (no son errores), como "te enviamos un correo". */
    private val _aviso = MutableLiveData<String?>()
    val aviso: LiveData<String?> = _aviso

    /** true mientras se revisa si el teléfono todavía tiene una sesión guardada (se muestra una carga en vez del formulario). */
    private val _restaurando = MutableLiveData(true)
    val restaurando: LiveData<Boolean> = _restaurando

    init {
        viewModelScope.launch {
            val usuario = authRepository.restaurarSesion()
            if (usuario != null) {
                SesionManager.iniciarSesion(usuario)
                _loginExitoso.value = true
            }
            _restaurando.value = false
        }
    }

    fun login(email: String, password: String) {
        if (_cargando.value == true) return
        var esValido = true
        val emailLimpio = email.trim()

        if (emailLimpio.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailLimpio).matches()) {
            _emailError.value = "Escribe un correo electrónico válido"
            esValido = false
        } else {
            _emailError.value = null
        }

        if (password.isEmpty()) {
            _passwordError.value = "Escribe tu contraseña"
            esValido = false
        } else {
            _passwordError.value = null
        }

        if (!esValido) return

        viewModelScope.launch {
            _cargando.value = true
            when (val resultado = authRepository.login(emailLimpio, password)) {
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

    /** Manda el correo para cambiar la contraseña; el aviso de éxito o error sale en el snackbar. */
    fun olvideContrasena(email: String) {
        val emailLimpio = email.trim()
        if (emailLimpio.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailLimpio).matches()) {
            _emailError.value = "Escribe tu correo para enviarte el enlace"
            return
        }
        _emailError.value = null
        viewModelScope.launch {
            _cargando.value = true
            _aviso.value = when (val resultado = authRepository.enviarRestablecimiento(emailLimpio)) {
                is Resultado.Exito -> "Te enviamos un correo a $emailLimpio para cambiar tu contraseña."
                is Resultado.Error -> resultado.mensaje
            }
            _cargando.value = false
        }
    }

    fun onAvisoMostrado() {
        _aviso.value = null
    }

    fun onErrorMostrado() {
        _errorGeneral.value = null
    }

    fun onNavegacionCompletada() {
        _loginExitoso.value = false
    }
}
