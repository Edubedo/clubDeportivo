package com.example.clubdeportivo.data.model

enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

/** Cómo pagó la persona. Se guarda en mayúsculas en `pagos.metodoPago`. */
enum class MetodoPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia")
}

/** Qué se cobró: el primer pago de una membresía, cada renovación mensual o una visita de un día. */
enum class ConceptoPago(val etiqueta: String) {
    ALTA("Alta"),
    MENSUALIDAD("Mensualidad"),
    VISITA("Visita"),
    OTRO("Otro")
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
