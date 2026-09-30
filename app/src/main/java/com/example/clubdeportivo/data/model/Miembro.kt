package com.example.clubdeportivo.data.model

/**
 * Persona del club con código propio. El código (p. ej. "CLB-7K3M9Q") es único y es con lo que
 * la persona entra al sistema. [codigo] vacío = registro anterior a los códigos.
 */
data class MiembroClub(
    val codigo: String,
    val nombre: String,
    val telefono: String,
    val correo: String,
    val membresiaId: String,
    /** "Titular" o el parentesco con el titular (Cónyuge, Hijo...). */
    val parentesco: String,
    /** Uid de la cuenta de acceso en Firebase Authentication. */
    val usuarioId: String
) {
    val esTitular get() = parentesco == PARENTESCO_TITULAR

    companion object {
        const val PARENTESCO_TITULAR = "Titular"
    }
}

/** Una membresía con las personas que cubre (el titular va primero). */
data class MembresiaDetalle(val membresia: Membresia, val personas: List<MiembroClub>) {
    val titular: MiembroClub? get() = personas.firstOrNull { it.esTitular } ?: personas.firstOrNull()
}

/** Datos capturados en el formulario para una persona; [codigo] null = persona nueva (todavía sin código). */
data class PersonaForm(
    val codigo: String?,
    val nombre: String,
    val telefono: String,
    val correo: String,
    val parentesco: String
)
