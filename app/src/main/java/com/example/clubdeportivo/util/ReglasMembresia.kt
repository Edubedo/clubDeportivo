package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.TipoMembresia
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * Reglas de membresías y códigos de acceso, sin Firebase ni Android para poder probarlas.
 *
 *  - Cada persona del club tiene un código único ("CLB-" + 6 caracteres) y con él entra al sistema.
 *  - El código se genera al azar (no es secuencial, no se puede adivinar el de otra persona) y
 *    evita los caracteres que se confunden al dictarlos: sin 0/O ni 1/I/L.
 *  - Solo se puede entrar con una membresía ACTIVA y dentro de su vigencia.
 */
object ReglasMembresia {

    const val PREFIJO = "CLB-"
    private const val LARGO = 6
    private const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val DOMINIO_CUENTAS = "miembros.clubdeportivo.app"

    fun generarCodigo(azar: Random = SecureRandom().asKotlinRandom()): String =
        PREFIJO + (1..LARGO).map { ALFABETO[azar.nextInt(ALFABETO.length)] }.joinToString("")

    /** Acepta "clb-7k3m9q", "CLB 7K3M9Q", "CLB7K3M9Q" o solo "7K3M9Q" y lo deja como "CLB-7K3M9Q". */
    fun normalizarCodigo(texto: String): String {
        val limpio = texto.trim().uppercase().replace(" ", "").replace("-", "")
        return when {
            limpio.startsWith("CLB") -> PREFIJO + limpio.removePrefix("CLB")
            limpio.length == LARGO -> PREFIJO + limpio
            else -> limpio
        }
    }

    fun esCodigo(texto: String): Boolean =
        Regex("^CLB-[$ALFABETO]{$LARGO}$").matches(normalizarCodigo(texto))

    /** Firebase Authentication identifica cuentas por correo: cada código tiene uno propio e interno. */
    fun emailDeCodigo(codigo: String) = "${normalizarCodigo(codigo).lowercase()}@$DOMINIO_CUENTAS"

    /** Una membresía activa cuya vigencia ya terminó cuenta como vencida. */
    fun estadoEfectivo(membresia: Membresia, hoy: String): EstadoMembresia =
        if (membresia.estado == EstadoMembresia.ACTIVA &&
            membresia.fechaVencimiento.isNotBlank() &&
            membresia.fechaVencimiento < hoy
        ) EstadoMembresia.VENCIDA else membresia.estado

    fun vencimientoInicial(tipo: TipoMembresia, inicio: String): String =
        if (tipo == TipoMembresia.VISITA) inicio else Fechas.sumarMeses(inicio, 1)

    /** Renovar suma un mes a partir del vencimiento (si aún no vence) o de hoy (si ya venció): no se regalan ni se pierden días. */
    fun fechasDeRenovacion(membresia: Membresia, hoy: String): Pair<String, String> {
        val vigente = membresia.fechaVencimiento.isNotBlank() && membresia.fechaVencimiento >= hoy
        val base = if (vigente) membresia.fechaVencimiento else hoy
        val inicio = if (vigente && membresia.fechaInicio.isNotBlank()) membresia.fechaInicio else hoy
        return inicio to Fechas.sumarMeses(base, 1)
    }

    /** Cuántas personas cubre como máximo (titular incluido). */
    fun maxPersonas(tipo: TipoMembresia, paqueteId: Int?): Int = when (tipo) {
        TipoMembresia.FAMILIAR -> paqueteId?.let { Catalogos.paqueteFamiliarPorId(it)?.maxIntegrantes } ?: 1
        else -> 1
    }

    fun rolDeAcceso(tipo: TipoMembresia): Rol =
        if (tipo == TipoMembresia.VISITA) Rol.VISITANTE_EXTERNO else Rol.SOCIO

    fun etiquetaPlan(membresia: Membresia): String = when (membresia.tipo) {
        TipoMembresia.INDIVIDUAL -> membresia.plan?.let { Catalogos.nombrePlanIndividual(it) } ?: "Individual"
        TipoMembresia.FAMILIAR ->
            "Paquete ${membresia.paqueteFamiliarId?.let { Catalogos.paqueteFamiliarPorId(it)?.nombre } ?: "familiar"}"
        TipoMembresia.VISITA -> "Visita"
    }

    const val ESQUEMA_ENLACE = "clubdeportivo"
    private const val DESTINO_ENLACE = "registro"

    /** Enlace de acceso que el titular comparte con su familiar: al abrirlo la app abre el registro con el código ya escrito. */
    fun enlaceDeAcceso(codigo: String) = "$ESQUEMA_ENLACE://$DESTINO_ENLACE?codigo=${normalizarCodigo(codigo)}"

    /** El código que trae un enlace de acceso, o null si el texto no es uno de nuestros enlaces o el código no es válido. */
    fun codigoDeEnlace(enlace: String?): String? {
        val texto = enlace?.trim().orEmpty()
        val prefijo = "$ESQUEMA_ENLACE://$DESTINO_ENLACE"
        if (!texto.startsWith(prefijo, ignoreCase = true)) return null
        val codigo = texto.substringAfter("codigo=", "").substringBefore("&")
        return normalizarCodigo(codigo).takeIf { esCodigo(it) }
    }

    /** Mensaje listo para mandar por WhatsApp, correo o SMS con el enlace de acceso y, por si el enlace no abre, el código. */
    fun mensajeDeInvitacion(nombreFamiliar: String, nombreTitular: String, codigo: String): String =
        "Hola ${nombreFamiliar.trim().substringBefore(" ")}, ${nombreTitular.trim()} te dio de alta en su plan familiar del Athletic Club.\n\n" +
            "Crea tu cuenta con este enlace:\n${enlaceDeAcceso(codigo)}\n\n" +
            "Si el enlace no abre la app, entra a Regístrate y escribe tu código: ${normalizarCodigo(codigo)}"

    /** @return el motivo por el que los datos del familiar que agrega el titular no son válidos, o null. */
    fun validarFamiliar(persona: PersonaForm, personasActuales: Int, tipo: TipoMembresia, paqueteId: Int?): String? {
        if (tipo != TipoMembresia.FAMILIAR) return "Solo los paquetes familiares admiten más personas."
        if (personasActuales >= maxPersonas(tipo, paqueteId)) {
            return "Tu paquete ya tiene a todos sus integrantes (${maxPersonas(tipo, paqueteId)})."
        }
        if (persona.parentesco.trim().length < 3) return "Indica el parentesco (Cónyuge, Hijo, Hija...)."
        return validarPersonas(tipo, paqueteId, listOf(persona))
    }

    /** @return el motivo por el que los datos no son válidos, o null. */
    fun validarPersonas(tipo: TipoMembresia, paqueteId: Int?, personas: List<PersonaForm>): String? {
        if (personas.isEmpty()) return "Agrega al titular."
        val max = maxPersonas(tipo, paqueteId)
        if (personas.size > max) return "Este paquete admite máximo $max personas (titular incluido)."
        personas.forEach { persona ->
            if (persona.nombre.trim().length < 3) return "Escribe el nombre completo de cada persona."
            if (persona.correo.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(persona.correo.trim())) {
                return "El correo de ${persona.nombre.trim()} no es válido."
            }
            val digitos = persona.telefono.count { it.isDigit() }
            if (persona.telefono.isNotBlank() && digitos < 10) {
                return "El teléfono de ${persona.nombre.trim()} debe tener 10 dígitos."
            }
        }
        return null
    }
}
