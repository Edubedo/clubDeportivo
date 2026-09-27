package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado

interface UsuarioRepository {

    suspend fun actualizarNombre(
        usuario: Usuario
    ): Resultado<Usuario>
}