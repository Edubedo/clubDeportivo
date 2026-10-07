package com.example.clubdeportivo.ui.registro

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.data.repository.EstadoCodigo
import com.example.clubdeportivo.util.ReglasContrasena
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.launch

/** Los dos pasos del registro: primero el código del miembro, después sus datos. */
enum class PasoRegistro { CODIGO, DATOS }

/**
 * Registro de un miembro del club. Solo se puede crear una cuenta con un código real que el personal entregó al
 * dar de alta la membresía; si el código ya tiene cuenta se avisa y se manda a iniciar sesión.
 */
class RegistroViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _paso = MutableLiveData(PasoRegistro.CODIGO)
    val paso: LiveData<PasoRegistro> = _paso

    /** Código ya verificado (normalizado, ej. "CLB-7K3M9Q"). */
    private val _codigo = MutableLiveData("")
    val codigo: LiveData<String> = _codigo

    private val _yaRegistrado = MutableLiveData(false)
    val yaRegistrado: LiveData<Boolean> = _yaRegistrado

    private val _codigoError = MutableLiveData<String?>()
    val codigoError: LiveData<String?> = _codigoError

    private val _nombreError = MutableLiveData<String?>()
    val nombreError: LiveData<String?> = _nombreError

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _confirmarError = MutableLiveData<String?>()
    val confirmarError: LiveData<String?> = _confirmarError

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _registroExitoso = MutableLiveData(false)
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _errorGeneral = MutableLiveData<String?>()
    val errorGeneral: LiveData<String?> = _errorGeneral

    fun verificarCodigo(texto: String) {
        if (_cargando.value == true) return
        _yaRegistrado.value = false
        val limpio = ReglasMembresia.normalizarCodigo(texto)
        if (texto.isBlank()) {
            _codigoError.value = "Escribe tu código de miembro"
            return
        }
        if (!ReglasMembresia.esCodigo(limpio)) {
            _codigoError.value = "El código se ve así: CLB-7K3M9Q"
            return
        }
        _codigoError.value = null

        viewModelScope.launch {
            _cargando.value = true
            when (val resultado = authRepository.consultarCodigo(limpio)) {
                is Resultado.Exito -> when (resultado.datos) {
                    EstadoCodigo.DISPONIBLE -> {
                        _codigo.value = limpio
                        _paso.value = PasoRegistro.DATOS
                    }
                    EstadoCodigo.YA_REGISTRADO -> {
                        _codigo.value = limpio
                        _yaRegistrado.value = true
                    }
                }
                is Resultado.Error -> _codigoError.value = resultado.mensaje
            }
            _cargando.value = false
        }
    }

    /** Vuelve al paso del código (por ejemplo, para corregirlo). */
    fun cambiarCodigo() {
        _paso.value = PasoRegistro.CODIGO
        _yaRegistrado.value = false
        _codigoError.value = null
    }

    fun registrar(nombre: String, email: String, password: String, confirmar: String) {
        if (_cargando.value == true) return
        var esValido = true

        if (nombre.trim().length < 3) {
            _nombreError.value = "Escribe tu nombre completo"
            esValido = false
        } else {
            _nombreError.value = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _emailError.value = "Escribe un correo electrónico válido"
            esValido = false
        } else {
            _emailError.value = null
        }

        val errorContrasena = ReglasContrasena.primerError(password)
        if (errorContrasena != null) {
            _passwordError.value = errorContrasena
            esValido = false
        } else {
            _passwordError.value = null
        }

        if (confirmar != password) {
            _confirmarError.value = "Las contraseñas no coinciden"
            esValido = false
        } else {
            _confirmarError.value = null
        }

        if (!esValido) return

        val codigoVerificado = _codigo.value.orEmpty()
        viewModelScope.launch {
            _cargando.value = true
            when (val resultado = authRepository.registrarMiembro(codigoVerificado, nombre, email, password)) {
                is Resultado.Exito -> {
                    SesionManager.iniciarSesion(resultado.datos)
                    _errorGeneral.value = null
                    _registroExitoso.value = true
                }
                is Resultado.Error -> _errorGeneral.value = resultado.mensaje
            }
            _cargando.value = false
        }
    }

    fun onErrorMostrado() {
        _errorGeneral.value = null
    }
}
