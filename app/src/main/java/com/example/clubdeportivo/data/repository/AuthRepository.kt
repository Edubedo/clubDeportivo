package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.util.Resultado
import kotlinx.coroutines.delay

enum class EstadoCodigo { DISPONIBLE, YA_REGISTRADO }

interface AuthRepository {
    suspend fun login(correo: String, password: String): Resultado<Usuario>

    /**
     * Dice si [codigo] pertenece a un miembro real y si ya tiene cuenta. No pide sesión ni expone datos personales.
     * Devuelve error si el código no existe.
     */
    suspend fun consultarCodigo(codigo: String): Resultado<EstadoCodigo>

    /** Crea la cuenta (correo y contraseña propios) de un miembro que se registra con su código. */
    suspend fun registrarMiembro(codigo: String, nombre: String, correo: String, password: String): Resultado<Usuario>

    /** Si el teléfono todavía recuerda una sesión de Firebase, la recupera para no pedir la contraseña cada vez. */
    suspend fun restaurarSesion(): Usuario?

    fun cerrarSesion()

    /** Envía al correo un enlace para elegir una contraseña nueva. */
    suspend fun enviarRestablecimiento(correo: String): Resultado<Unit>
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

    override suspend fun consultarCodigo(codigo: String): Resultado<EstadoCodigo> =
        Resultado.Error("El registro por código solo está disponible con la base de datos real.")

    override suspend fun registrarMiembro(codigo: String, nombre: String, correo: String, password: String): Resultado<Usuario> =
        Resultado.Error("El registro por código solo está disponible con la base de datos real.")

    override suspend fun restaurarSesion(): Usuario? = null

    override fun cerrarSesion() = Unit

    override suspend fun enviarRestablecimiento(correo: String): Resultado<Unit> = Resultado.Exito(Unit)
}
