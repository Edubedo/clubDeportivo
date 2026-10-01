package com.example.clubdeportivo.data.model

/**
 * Una persona del personal del club. Vive en un solo documento de `usuarios/{uid}`: la identidad, el rol de
 * acceso ([rol]: Administrador o Encargado de área) y los datos laborales ([tipoPersonal], [turno], [areaTrabajo]).
 */
data class Personal(
    val id: String,
    val nombre: String,
    val email: String,
    val telefono: String,
    val estado: String,
    val rol: Rol,
    /** Tipo de trabajo sin el área: "Instructor". */
    val tipoPersonal: String,
    /** Matutino | Vespertino; "" si todavía no se capturó. */
    val turno: String,
    /** Área del club (deporte: "Fútbol", "Tenis"...) donde trabaja; null = sin área asignada. */
    val areaTrabajo: String?,
    val fotoUrl: String?,
    /** Fecha de alta como personal (yyyy-MM-dd); "" en registros anteriores sin ese dato. */
    val fechaIngreso: String = ""
)

/** Datos que captura el formulario de Personal. [contrasena] solo se usa al crear la cuenta. */
data class DatosPersonal(
    val nombre: String,
    val email: String,
    val telefono: String,
    val estado: String,
    val rol: Rol,
    val tipoPersonal: String,
    val turno: String,
    val areaTrabajo: String?,
    val fotoUrl: String?,
    val contrasena: String = ""
)
