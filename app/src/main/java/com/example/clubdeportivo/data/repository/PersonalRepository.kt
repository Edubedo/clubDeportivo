package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.DatosPersonal
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.util.Resultado

interface PersonalRepository {
    /** Personal del club (administradores y encargados de área), ordenado por nombre. */
    suspend fun obtenerPersonal(): List<Personal>

    /** Cuántas personas del personal hay; lo usa el dashboard. */
    suspend fun contarPersonal(): Int

    /** Crea la cuenta de acceso (Firebase Authentication) y el perfil en `usuarios`, sin cerrar la sesión de quien la crea. */
    suspend fun crear(datos: DatosPersonal): Resultado<Personal>

    /** Actualiza el perfil. El correo y la contraseña no se cambian aquí (ver [enviarRestablecimientoDeContrasena]). */
    suspend fun actualizar(id: String, datos: DatosPersonal): Resultado<Unit>

    /** Manda a [email] un correo para que la persona elija una contraseña nueva. */
    suspend fun enviarRestablecimientoDeContrasena(email: String): Resultado<Unit>
}
