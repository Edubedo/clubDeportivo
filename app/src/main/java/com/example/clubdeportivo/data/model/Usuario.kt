package com.example.clubdeportivo.data.model

/**
 * Roles del club, de mayor a menor alcance:
 * - SUPERADMIN: nivel sistema, controla todo.
 * - ADMIN: nivel empresa, controla todas las áreas.
 * - ADMIN_AREA: nivel área, solo administra su área.
 * - AYUDANTE_AREA: ayuda dentro de un área.
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
    Rol.ADMIN_AREA -> "Administrador de área"
    Rol.AYUDANTE_AREA -> "Ayudante de área"
    Rol.SOCIO -> "Socio"
    Rol.VISITANTE_EXTERNO -> "Visitante externo"
}

/** Roles del personal del club: no están sujetos a los límites de reservas pensados para socios y visitantes. */
fun Rol.esPersonal(): Boolean = this == Rol.SUPERADMIN || this == Rol.ADMIN || this == Rol.ADMIN_AREA

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

data class Empleado(
    val id: String,
    val usuarioId: String,
    val puesto: String,
    val areaAsignadaId: String? = null
)
