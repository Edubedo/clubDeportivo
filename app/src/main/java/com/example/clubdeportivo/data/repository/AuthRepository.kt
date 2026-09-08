package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.delay

interface AuthRepository {
    suspend fun login(correo: String, password: String): Resultado<Usuario>
    suspend fun registrar(nombre: String, correo: String, password: String, rol: Rol): Resultado<Usuario>
}

/**
 * Implementación de prueba: no llama a ningún servidor todavía.
 * El rol (y el id de usuario) se decide según el correo, para poder probar
 * las pantallas de cada rol mientras el backend no existe. Cada rol tiene
 * un id fijo distinto para que sus reservas/membresía de prueba no se mezclen:
 *
 *   superadmin@clubdeportivo.com -> SUPERADMIN       (id 1)
 *   admin@clubdeportivo.com      -> ADMIN            (id 2)
 *   areadmin@clubdeportivo.com   -> ADMIN_AREA       (id 3)
 *   ayudante@clubdeportivo.com   -> AYUDANTE_AREA    (id 4)
 *   externo@clubdeportivo.com    -> VISITANTE_EXTERNO (id 6)
 *   cualquier otro correo        -> SOCIO            (id 5)
 *
 * Ya no se usa (ver FirebaseAuthRepository), pero se deja como referencia simple del
 * patrón repositorio + `Resultado<T>`.
 */
class FakeAuthRepository : AuthRepository {

    private data class PerfilDemo(val id: String, val nombre: String, val rol: Rol)

    private val perfiles = mapOf(
        "superadmin@clubdeportivo.com" to PerfilDemo("1", "Superadmin", Rol.SUPERADMIN),
        "admin@clubdeportivo.com" to PerfilDemo("2", "Admin", Rol.ADMIN),
        "areadmin@clubdeportivo.com" to PerfilDemo("3", "Admin de área", Rol.ADMIN_AREA),
        "ayudante@clubdeportivo.com" to PerfilDemo("4", "Ayudante", Rol.AYUDANTE_AREA),
        "externo@clubdeportivo.com" to PerfilDemo("6", "Visitante", Rol.VISITANTE_EXTERNO)
    )

    override suspend fun login(correo: String, password: String): Resultado<Usuario> {
        delay(400)

        val correoNormalizado = correo.trim().lowercase()
        val perfil = perfiles[correoNormalizado]
            ?: PerfilDemo("5", correo.substringBefore("@").replaceFirstChar { it.uppercase() }, Rol.SOCIO)

        val usuario = Usuario(
            id = perfil.id,
            nombre = perfil.nombre,
            correo = correo,
            rol = perfil.rol
        )

        return Resultado.Exito(usuario)
    }

    override suspend fun registrar(nombre: String, correo: String, password: String, rol: Rol): Resultado<Usuario> {
        delay(400)
        return Resultado.Exito(Usuario(id = correo, nombre = nombre, correo = correo, rol = rol))
    }
}
