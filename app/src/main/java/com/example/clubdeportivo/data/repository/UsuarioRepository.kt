package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado

interface UsuarioRepository {

    suspend fun actualizarNombre(
        usuario: Usuario
    ): Resultado<Usuario>

    /** Usuarios con rol SOCIO, para elegir a quién asignarle una membresía. */
    suspend fun obtenerSocios(): List<Usuario>
}