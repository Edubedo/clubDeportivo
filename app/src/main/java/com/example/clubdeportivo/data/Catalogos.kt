package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.PaqueteFamiliar
import com.example.clubdeportivo.data.model.PlanIndividual

/**
 * Precios y reglas de negocio del club, en un solo lugar para que la app y
 * los repositorios de prueba no repitan números "mágicos" por su cuenta.
 * Cuando exista el backend real, esta información vivirá en la base de
 * datos (tablas paquete_familiar / membresia) y este objeto ya no hará falta.
 */
object Catalogos {

    const val PRECIO_VISITA = 200.0

    fun precioPlanIndividual(plan: PlanIndividual): Double = when (plan) {
        PlanIndividual.NINO -> 1500.0
        PlanIndividual.NORMAL -> 1800.0
        PlanIndividual.DELUXE -> 3500.0
    }

    fun beneficiosPlanIndividual(plan: PlanIndividual): List<String> = when (plan) {
        PlanIndividual.NINO -> listOf("Acceso a todas las áreas (tarifa infantil)")
        PlanIndividual.NORMAL -> listOf("Acceso a todas las áreas")
        PlanIndividual.DELUXE -> listOf(
            "Acceso a todas las áreas",
            "Prioridad en reservaciones",
            "Entrenamiento personalizado incluido",
            "Agua gratis",
            "Snacks incluidos"
        )
    }

    fun nombrePlanIndividual(plan: PlanIndividual): String = when (plan) {
        PlanIndividual.NINO -> "Individual niños"
        PlanIndividual.NORMAL -> "Individual normal"
        PlanIndividual.DELUXE -> "Individual deluxe"
    }

    val paquetesFamiliares = listOf(
        PaqueteFamiliar(
            id = 1,
            nombre = "Familiar",
            descripcion = "Incluye 2 adultos y 3 niños. Acceso a todas las áreas para todos los integrantes.",
            maxIntegrantes = 5,
            precioMensual = 7000.0
        ),
        PaqueteFamiliar(
            id = 2,
            nombre = "Pareja",
            descripcion = "Incluye 2 adultos. Acceso a todas las áreas para ambos integrantes.",
            maxIntegrantes = 2,
            precioMensual = 2999.0
        ),
        PaqueteFamiliar(
            id = 3,
            nombre = "Niños",
            descripcion = "Paquete para hermanos. Acceso a todas las áreas.",
            maxIntegrantes = 3,
            precioMensual = 2800.0
        )
    )

    fun paqueteFamiliarPorId(id: Int): PaqueteFamiliar? = paquetesFamiliares.find { it.id == id }

    // --- Reglas de reservación ---
    const val MAX_RESERVAS_ACTIVAS_POR_SOCIO = 3
    const val ANTICIPACION_MINIMA_HORAS = 2
    const val ANTICIPACION_MAXIMA_DIAS = 7
    const val DURACION_RESERVA_HORAS = 1
    const val CANCELACION_SIN_PENALIZACION_HORAS = 4
    const val MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS = 1

    // --- Reglas de inasistencias (no-show) ---
    const val INASISTENCIAS_PARA_BLOQUEO = 3
    const val VENTANA_INASISTENCIAS_DIAS = 30
    const val DIAS_BLOQUEO_POR_INASISTENCIAS = 7

    // --- Horario de reservas para visitantes externos aprobados ---
    const val HORA_INICIO_EXTERNOS = "08:00"
    const val HORA_FIN_EXTERNOS = "20:00"
}
