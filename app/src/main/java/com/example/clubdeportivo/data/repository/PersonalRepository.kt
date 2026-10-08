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

    /** Actualiza el perfil. El correo no se cambia aquí y la contraseña tiene su propio método ([cambiarContrasena]). */
    suspend fun actualizar(id: String, datos: DatosPersonal): Resultado<Unit>

    /**
     * Cambia la contraseña de la persona [id] sin mandarle correo. Solo funciona si quien llama es administrador:
     * lo comprueba el servidor (Cloud Function `cambiarContrasenaPersonal`), no la app. Cierra las sesiones abiertas
     * de esa persona.
     */
    suspend fun cambiarContrasena(id: String, contrasena: String): Resultado<Unit>
}
