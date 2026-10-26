package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.model.PaqueteFamiliar
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.util.Textos

/**
 * Precios y reglas de negocio del club, en un solo lugar para que la app y
 * los repositorios de prueba no repitan números "mágicos" por su cuenta.
 * Cuando exista el backend real, esta información vivirá en la base de
 * datos (tablas paquete_familiar / membresia) y este objeto ya no hará falta.
 */
object Catalogos {

    const val PRECIO_VISITA = 200.0

    /** Tipos de personal conocidos. El puesto se escribe "<tipo> de <área>" (salvo los que ya traen "de área"). */
    val TIPOS_DE_PERSONAL = listOf("Instructor", "Limpieza", "Ayudante de área", "Administrador de área")

    /** Tipo de personal de un puesto ("Instructor de Cancha 1" -> "Instructor"); si no es conocido, el puesto completo. */
    fun tipoDePuesto(puesto: String): String =
        TIPOS_DE_PERSONAL.firstOrNull { puesto.startsWith(it) } ?: puesto

    /** Cómo se cobra: membresías y paquetes se pagan cada mes; la visita se paga una sola vez, por día. */
    val METODOS_DE_PAGO = listOf(Textos.get("cash"), Textos.get("card"), Textos.get("bank_transfer"))

    fun precioPlanIndividual(plan: PlanIndividual): Double = when (plan) {
        PlanIndividual.NINO -> 1500.0
        PlanIndividual.NORMAL -> 1800.0
        PlanIndividual.DELUXE -> 3500.0
    }

    fun beneficiosPlanIndividual(plan: PlanIndividual): List<String> = when (plan) {
        PlanIndividual.NINO -> listOf(Textos.get("access_to_all_areas_kids_rate"))
        PlanIndividual.NORMAL -> listOf(Textos.get("access_to_all_areas"))
        PlanIndividual.DELUXE -> listOf(
            Textos.get("access_to_all_areas"),
            Textos.get("booking_priority"),
            Textos.get("personal_training_included"),
            Textos.get("free_water"),
            Textos.get("snacks_included")
        )
    }

    fun nombrePlanIndividual(plan: PlanIndividual): String = when (plan) {
        PlanIndividual.NINO -> Textos.get("individual_kids")
        PlanIndividual.NORMAL -> Textos.get("individual_standard")
        PlanIndividual.DELUXE -> Textos.get("individual_deluxe")
    }

    val paquetesFamiliares = listOf(
        PaqueteFamiliar(
            id = 1,
            nombre = Textos.get("family"),
            descripcion = Textos.get("includes_2_adults_and_3_children"),
            maxIntegrantes = 5,
            precioMensual = 7000.0
        ),
        PaqueteFamiliar(
            id = 2,
            nombre = Textos.get("couple"),
            descripcion = Textos.get("includes_2_adults_access_to_all"),
            maxIntegrantes = 2,
            precioMensual = 2999.0
        ),
        PaqueteFamiliar(
            id = 3,
            nombre = Textos.get("kids"),
            descripcion = Textos.get("package_for_siblings_access_to_all"),
            maxIntegrantes = 3,
            precioMensual = 2800.0
        )
    )

    fun paqueteFamiliarPorId(id: Int): PaqueteFamiliar? = paquetesFamiliares.find { it.id == id }

    // --- Reglas de reservación ---
    const val MAX_RESERVAS_ACTIVAS_POR_SOCIO = 20
    const val ANTICIPACION_MINIMA_HORAS = 0
    const val ANTICIPACION_MAXIMA_DIAS = 7
    const val DURACION_RESERVA_HORAS = 1
    const val MAX_HORAS_POR_RESERVA = 4
    const val CANCELACION_SIN_PENALIZACION_HORAS = 4
    const val MATERIAL_AUTOMATICO_ANTICIPACION_MINIMA_HORAS = 1
    const val HORA_APERTURA_AREAS = "06:00"
    const val HORA_CIERRE_AREAS = "22:00"
    const val LUGARES_PARA_ALERTA_CUPO_BAJO = 2

    // --- Lista de espera de horas llenas ---
    /** Minutos que se le guarda el lugar liberado a quien le tocó en la fila, antes de pasarlo al siguiente. */
    const val MINUTOS_PRIORIDAD_LISTA_ESPERA = 30

    // --- Reglas de inasistencias (no-show) ---
    const val INASISTENCIAS_PARA_BLOQUEO = 3
    const val VENTANA_INASISTENCIAS_DIAS = 30
    const val DIAS_BLOQUEO_POR_INASISTENCIAS = 7

}
