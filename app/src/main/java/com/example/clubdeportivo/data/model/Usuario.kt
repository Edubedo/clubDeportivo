package com.example.clubdeportivo.data.model

/**
 * Roles del club, de mayor a menor alcance:
 * - SUPERADMIN: nivel sistema, controla todo (igual que ADMIN en la app).
 * - ADMIN: Administrador. Único con acceso al dashboard y a Personal (altas y cuentas del personal).
 * - ADMIN_AREA: rol heredado; se trata como Encargado de área.
 * - AYUDANTE_AREA: Encargado de área. Solo reservas, áreas y membresías.
 * - SOCIO: solo puede elegir a qué área ir y reservar.
 * - VISITANTE_EXTERNO: fue cliente de visita, acceso limitado (requiere aprobación para reservar).
 */
enum class Rol {
    SUPERADMIN,
    ADMIN,
    ADMIN_AREA,
    AYUDANTE_AREA,
    SOCIO,
    VISITANTE_EXTERNO
}

fun Rol.nombreLegible(): String = when (this) {
    Rol.SUPERADMIN -> "Superadministrador"
    Rol.ADMIN -> "Administrador"
    Rol.ADMIN_AREA -> "Encargado de área"
    Rol.AYUDANTE_AREA -> "Encargado de área"
    Rol.SOCIO -> "Socio"
    Rol.VISITANTE_EXTERNO -> "Visitante externo"
}

/** Roles del personal del club: no están sujetos a los límites de reservas pensados para socios y visitantes. */
fun Rol.esPersonal(): Boolean = this != Rol.SOCIO && this != Rol.VISITANTE_EXTERNO

/** Solo los administradores entran al dashboard y a Personal, y crean cuentas del personal. */
fun Rol.esAdministrador(): Boolean = this == Rol.SUPERADMIN || this == Rol.ADMIN

/** Roles con los que se puede dar de alta a una persona del personal desde la pantalla Personal. */
val ROLES_DE_PERSONAL = listOf(Rol.ADMIN, Rol.AYUDANTE_AREA)

/** Socios y visitantes: clientes del club, no personal; usan la app con un menú propio (reservar, su membresía y su perfil). */
fun Rol.esCliente(): Boolean = this == Rol.SOCIO || this == Rol.VISITANTE_EXTERNO

/** [id] es el mismo uid que genera Firebase Authentication al iniciar sesión. */
data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val fotoUrl: String? = null
)
