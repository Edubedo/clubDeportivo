package com.example.clubdeportivo.data.model

import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos
enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

/** Cómo pagó la persona. Se guarda en mayúsculas en `pagos.metodoPago`. */
enum class MetodoPago(val etiqueta: String) {
    EFECTIVO(Textos.get(R.string.cash)),
    TARJETA(Textos.get(R.string.card)),
    TRANSFERENCIA(Textos.get(R.string.bank_transfer))
}

/** Qué se cobró: el primer pago de una membresía, cada renovación mensual o una visita de un día. */
enum class ConceptoPago(val etiqueta: String) {
    ALTA(Textos.get(R.string.registration)),
    MENSUALIDAD(Textos.get(R.string.monthly_fee)),
    VISITA(Textos.get(R.string.visit)),
    OTRO(Textos.get(R.string.other))
}

/** Un cobro registrado por el personal. Es lo que alimenta los ingresos del dashboard. */
data class PagoClub(
    val id: String,
    val membresiaId: String,
    val titularNombre: String,
    val concepto: ConceptoPago,
    val monto: Double,
    val metodo: MetodoPago?,
    /** "yyyy-MM-dd". */
    val fecha: String,
    val estado: EstadoPago = EstadoPago.PAGADO
)
