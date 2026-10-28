package com.example.clubdeportivo.util.reportes

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.EstadoPago
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.PagoClub
import com.example.clubdeportivo.data.model.Personal
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esEncargado
import com.example.clubdeportivo.data.model.etiquetaArea
import com.example.clubdeportivo.data.model.etiquetaDeporte
import com.example.clubdeportivo.data.model.etiquetaMotivoSuspension
import com.example.clubdeportivo.data.model.etiquetaParentesco
import com.example.clubdeportivo.data.model.etiquetaTipoPersonal
import com.example.clubdeportivo.data.model.etiquetaTurno
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.notificaciones.ReglasRecordatorios
import com.example.clubdeportivo.util.ReglasMembresia
import com.example.clubdeportivo.util.ReglasReserva
import com.example.clubdeportivo.util.Textos

/**
 * Datos leídos para el reporte. Una lista en null = esa sección no se pidió o no se pudo leer (no se exporta, en vez
 * de exportarla vacía y que parezca que no hay datos).
 */
data class DatosReporte(
    val pagos: List<PagoClub>? = null,
    val membresias: List<MembresiaDetalle>? = null,
    val reservas: List<Reserva>? = null,
    /** id de reserva -> "ASISTIO" / "NO_ASISTIO". */
    val asistencias: Map<String, String> = emptyMap(),
    val areas: List<Area>? = null,
    val inventario: List<ArticuloInventario>? = null,
    val personal: List<Personal>? = null,
    val torneos: List<Torneo>? = null
)

/**
 * Arma el reporte completo del club para el administrador: un resumen con las cifras clave y una tabla con todos los
 * registros de cada sección, para darle seguimiento fuera de la app (en Excel o en PDF).
 */
object GeneradorReporte {
    /** Membresías activas que vencen en estos días o menos se cuentan como "por vencer". */
    const val DIAS_POR_VENCER = 7

    private fun t(clave: String, vararg args: Any) = Textos.get(clave, *args)

    fun generar(datos: DatosReporte, periodo: PeriodoReporte, hoy: String, generado: String): Reporte {
        val (desde, hasta) = periodo.rango(hoy)
        val enPeriodo: (String) -> Boolean = { it in desde..hasta }
        val resumen = mutableListOf<BloqueResumen>()
        val tablas = mutableListOf<Tabla>()

        datos.pagos?.let { todos ->
            val pagos = todos.filter { enPeriodo(it.fecha) }.sortedWith(compareBy({ it.fecha }, { it.titularNombre }))
            resumen += resumenIngresos(pagos)
            tablas += tablaIngresos(pagos)
        }
        datos.membresias?.let { membresias ->
            resumen += resumenMembresias(membresias, enPeriodo, hoy)
            tablas += tablaMembresias(membresias)
            tablas += tablaMiembros(membresias)
        }
        datos.reservas?.let { todas ->
            val reservas = todas.filter { enPeriodo(it.fecha) }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            resumen += resumenReservas(reservas, datos.asistencias)
            tablas += tablaReservas(reservas, datos.asistencias)
            tablas += tablaOcupacion(reservas, datos.asistencias)
        }
        datos.areas?.let { areas ->
            resumen += BloqueResumen(
                t("rep_sec_areas"), listOf(
                    t("rep_res_areas_total") to areas.size.toString(),
                    t("rep_disp_disponible") to areas.count { it.disponibilidad == DisponibilidadArea.DISPONIBLE }.toString(),
                    t("rep_disp_ocupada") to areas.count { it.disponibilidad == DisponibilidadArea.OCUPADA }.toString(),
                    t("rep_disp_mantenimiento") to areas.count { it.disponibilidad == DisponibilidadArea.MANTENIMIENTO }.toString()
                )
            )
            tablas += tablaAreas(areas)
        }
        datos.inventario?.let { articulos ->
            resumen += BloqueResumen(
                t("rep_sec_inventario"), listOf(
                    t("rep_res_articulos") to articulos.size.toString(),
                    t("rep_res_unidades") to articulos.sumOf { it.cantidad }.toString(),
                    t("rep_res_stock_bajo") to articulos.count { stockBajo(it) }.toString()
                )
            )
            tablas += tablaInventario(articulos)
        }
        datos.personal?.let { personal ->
            resumen += BloqueResumen(
                t("rep_sec_personal"), listOf(
                    t("rep_res_personal_total") to personal.size.toString(),
                    t("rep_activo") to personal.count { it.estado.equals("ACTIVO", ignoreCase = true) }.toString(),
                    t("rol_admin") to personal.count { it.rol.esAdministrador() }.toString(),
                    t("rol_encargado") to personal.count { it.rol.esEncargado() }.toString()
                )
            )
            tablas += tablaPersonal(personal)
        }
        datos.torneos?.let { todos ->
            // Un torneo entra si alguno de sus días cae dentro del periodo.
            val torneos = todos.filter { it.fechaInicio <= hasta && it.fechaFin.ifBlank { it.fechaInicio } >= desde }
                .sortedBy { it.fechaInicio }
            resumen += BloqueResumen(
                t("rep_sec_torneos"), listOf(
                    t("rep_res_torneos") to torneos.size.toString(),
                    t("rep_res_inscritos") to torneos.sumOf { it.inscritos }.toString(),
                    t("rep_res_cupo") to torneos.sumOf { it.cupoMaximo }.toString()
                )
            )
            tablas += tablaTorneos(torneos)
        }

        val textoPeriodo = if (periodo == PeriodoReporte.TODO) periodo.etiqueta
        else t("rep_periodo_rango", periodo.etiqueta, desde, hasta)
        return Reporte(t("rep_titulo_documento"), textoPeriodo, generado, resumen, tablas)
    }

    // ---------------------------------------------------------------- Ingresos

    private fun resumenIngresos(pagos: List<PagoClub>): BloqueResumen {
        val pagados = pagos.filter { it.estado == EstadoPago.PAGADO }
        val datos = mutableListOf(
            t("rep_res_total_cobrado") to formatoDinero(pagados.sumOf { it.monto }),
            t("rep_res_cobros") to pagados.size.toString()
        )
        if (pagados.isNotEmpty()) datos += t("rep_res_ticket_promedio") to formatoDinero(pagados.sumOf { it.monto } / pagados.size)
        pagados.groupBy { it.metodo }.forEach { (metodo, lista) ->
            datos += t("rep_res_por_metodo", metodo?.etiqueta ?: t("not_specified_2")) to formatoDinero(lista.sumOf { it.monto })
        }
        pagados.groupBy { it.concepto }.forEach { (concepto, lista) ->
            datos += t("rep_res_por_concepto", concepto.etiqueta) to formatoDinero(lista.sumOf { it.monto })
        }
        val noPagados = pagos.size - pagados.size
        if (noPagados > 0) datos += t("rep_res_no_pagados") to noPagados.toString()
        return BloqueResumen(t("rep_sec_ingresos"), datos)
    }

    private fun tablaIngresos(pagos: List<PagoClub>) = Tabla(
        t("rep_sec_ingresos"),
        listOf(t("rep_col_fecha"), t("rep_col_titular"), t("rep_col_concepto"), t("rep_col_metodo"), t("rep_col_estado"), t("rep_col_monto")),
        pagos.map { p ->
            listOf(
                texto(p.fecha), texto(p.titularNombre), texto(p.concepto.etiqueta),
                texto(p.metodo?.etiqueta ?: t("not_specified_2")), texto(estadoPago(p.estado)), dinero(p.monto)
            )
        },
        listOf(1f, 2.2f, 1.3f, 1.3f, 1f, 1.1f)
    )

    private fun estadoPago(estado: EstadoPago) = when (estado) {
        EstadoPago.PAGADO -> t("rep_pago_pagado")
        EstadoPago.PENDIENTE -> t("rep_pago_pendiente")
        EstadoPago.RECHAZADO -> t("rep_pago_rechazado")
    }

    // ---------------------------------------------------------------- Membresías y miembros

    private fun resumenMembresias(membresias: List<MembresiaDetalle>, enPeriodo: (String) -> Boolean, hoy: String): BloqueResumen {
        val activas = membresias.filter { it.membresia.estado == EstadoMembresia.ACTIVA }
        return BloqueResumen(
            t("rep_sec_membresias"), listOf(
                t("rep_res_membresias_total") to membresias.size.toString(),
                t("rep_estado_activa") to activas.size.toString(),
                t("rep_estado_vencida") to membresias.count { it.membresia.estado == EstadoMembresia.VENCIDA }.toString(),
                t("rep_estado_suspendida") to membresias.count { it.membresia.estado == EstadoMembresia.SUSPENDIDA }.toString(),
                t("rep_res_altas_periodo") to membresias.count { enPeriodo(it.membresia.fechaInicio) }.toString(),
                t("rep_res_personas_cubiertas") to activas.sumOf { it.personas.size.coerceAtLeast(1) }.toString(),
                t("rep_res_por_vencer", DIAS_POR_VENCER) to activas.count {
                    ReglasRecordatorios.diasHasta(hoy, it.membresia.fechaVencimiento) in 0..DIAS_POR_VENCER
                }.toString(),
                t("rep_res_ingreso_mensual") to formatoDinero(activas.sumOf { it.membresia.precio })
            )
        )
    }

    private fun tablaMembresias(membresias: List<MembresiaDetalle>) = Tabla(
        t("rep_sec_membresias"),
        listOf(
            t("rep_col_titular"), t("rep_col_codigo"), t("rep_col_tipo"), t("rep_col_plan"), t("rep_col_precio"),
            t("rep_col_estado"), t("rep_col_inicio"), t("rep_col_vence"), t("rep_col_personas"), t("rep_col_motivo")
        ),
        membresias.sortedWith(compareBy({ it.membresia.estado.ordinal }, { it.membresia.fechaVencimiento })).map { d ->
            val m = d.membresia
            listOf(
                texto(d.titular?.nombre.orEmpty()), texto(d.titular?.codigo.orEmpty()), texto(tipoMembresia(m.tipo)),
                texto(ReglasMembresia.etiquetaPlan(m)), dinero(m.precio), texto(estadoMembresia(m.estado)),
                texto(m.fechaInicio), texto(m.fechaVencimiento), numero(d.personas.size),
                texto(etiquetaMotivoSuspension(m.motivoSuspension))
            )
        },
        listOf(2f, 1.3f, 1f, 1.5f, 1f, 1f, 1f, 1f, 0.8f, 1.6f)
    )

    private fun tablaMiembros(membresias: List<MembresiaDetalle>) = Tabla(
        t("rep_sec_miembros"),
        listOf(
            t("rep_col_codigo"), t("rep_col_nombre"), t("rep_col_parentesco"), t("rep_col_telefono"), t("rep_col_correo"),
            t("rep_col_plan"), t("rep_col_estado"), t("rep_col_vence"), t("rep_col_cuenta")
        ),
        membresias.flatMap { d -> d.personas.map { d to it } }
            .sortedBy { it.second.nombre.lowercase() }
            .map { (d, p) ->
                listOf(
                    texto(p.codigo), texto(p.nombre), texto(etiquetaParentesco(p.parentesco)), texto(p.telefono), texto(p.correo),
                    texto(ReglasMembresia.etiquetaPlan(d.membresia)), texto(estadoMembresia(d.membresia.estado)),
                    texto(d.membresia.fechaVencimiento), texto(siNo(p.cuentaCreada))
                )
            },
        listOf(1.2f, 2f, 1f, 1.2f, 2.2f, 1.4f, 0.9f, 1f, 0.7f)
    )

    private fun tipoMembresia(tipo: TipoMembresia) = when (tipo) {
        TipoMembresia.INDIVIDUAL -> t("rep_tipo_individual")
        TipoMembresia.FAMILIAR -> t("rep_tipo_familiar")
        TipoMembresia.VISITA -> t("rep_tipo_visita")
    }

    private fun estadoMembresia(estado: EstadoMembresia) = when (estado) {
        EstadoMembresia.ACTIVA -> t("rep_estado_activa")
        EstadoMembresia.VENCIDA -> t("rep_estado_vencida")
        EstadoMembresia.SUSPENDIDA -> t("rep_estado_suspendida")
    }

    // ---------------------------------------------------------------- Reservas

    private fun resumenReservas(reservas: List<Reserva>, asistencias: Map<String, String>): BloqueResumen {
        val datos = mutableListOf(t("rep_res_reservas_total") to reservas.size.toString())
        EstadoReserva.entries.forEach { estado ->
            val cantidad = reservas.count { it.estado == estado }
            if (cantidad > 0) datos += estadoReserva(estado) to cantidad.toString()
        }
        val atendidas = reservas.filter { it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.FINALIZADA }
        datos += t("rep_res_personas_atendidas") to atendidas.sumOf { it.personas }.toString()
        datos += t("rep_asistio") to reservas.count { asistencias[it.id] == ASISTIO }.toString()
        datos += t("rep_no_asistio") to reservas.count { asistencias[it.id] == NO_ASISTIO }.toString()
        atendidas.groupBy { nombreArea(it) }.maxByOrNull { it.value.size }?.let { (area, lista) ->
            datos += t("rep_res_area_top") to "$area (${lista.size})"
        }
        atendidas.groupBy { it.horaInicio }.maxByOrNull { it.value.size }?.let { (hora, lista) ->
            datos += t("rep_res_hora_top") to "$hora (${lista.size})"
        }
        return BloqueResumen(t("rep_sec_reservas"), datos)
    }

    private fun tablaReservas(reservas: List<Reserva>, asistencias: Map<String, String>) = Tabla(
        t("rep_sec_reservas"),
        listOf(
            t("rep_col_fecha"), t("rep_col_horario"), t("rep_col_area"), t("rep_col_deporte"), t("rep_col_socio"),
            t("rep_col_personas"), t("rep_col_externo"), t("rep_col_estado"), t("rep_col_asistencia")
        ),
        reservas.map { r ->
            listOf(
                texto(r.fecha), texto("${r.horaInicio}–${r.horaFin}"), texto(nombreArea(r) + (r.carril?.let { " · ${t("rep_carril", it)}" } ?: "")),
                texto(etiquetaDeporte(r.deporte)), texto(r.usuarioNombre), numero(r.personas), texto(siNo(r.esExterno)),
                texto(estadoReserva(r.estado)), texto(asistencia(asistencias[r.id]))
            )
        },
        listOf(1f, 1.1f, 1.8f, 1f, 1.8f, 0.7f, 0.7f, 1.1f, 1f)
    )

    /** Reservas agrupadas por área: cuánto se usa cada una en el periodo. */
    private fun tablaOcupacion(reservas: List<Reserva>, asistencias: Map<String, String>) = Tabla(
        t("rep_sec_ocupacion"),
        listOf(
            t("rep_col_area"), t("rep_col_deporte"), t("rep_col_reservas"), t("rep_col_horas"), t("rep_col_personas"),
            t("rep_col_canceladas"), t("rep_asistio"), t("rep_no_asistio")
        ),
        reservas.groupBy { it.areaId to nombreArea(it) }.map { (clave, lista) ->
            val usadas = lista.filter { it.estado == EstadoReserva.CONFIRMADA || it.estado == EstadoReserva.FINALIZADA }
            listOf(
                texto(clave.second), texto(etiquetaDeporte(lista.first().deporte)), numero(usadas.size),
                numero(usadas.sumOf { horas(it) }), numero(usadas.sumOf { it.personas }),
                numero(lista.count { it.estado == EstadoReserva.CANCELADA || it.estado == EstadoReserva.RECHAZADA }),
                numero(lista.count { asistencias[it.id] == ASISTIO }), numero(lista.count { asistencias[it.id] == NO_ASISTIO })
            )
        }.sortedByDescending { (it[2] as Celda.Numero).valor },
        listOf(2f, 1.2f, 1f, 1f, 1f, 1f, 1f, 1f)
    )

    private fun nombreArea(r: Reserva) = etiquetaArea(r.areaNombre.ifBlank { r.deporte })

    /** Duración de la reserva en horas (0 si las horas no son válidas). */
    private fun horas(r: Reserva): Double {
        val inicio = ReglasReserva.aMinutos(r.horaInicio) ?: return 0.0
        val fin = ReglasReserva.aMinutos(r.horaFin) ?: return 0.0
        return ((fin - inicio).coerceAtLeast(0)) / 60.0
    }

    private fun estadoReserva(estado: EstadoReserva) = when (estado) {
        EstadoReserva.CONFIRMADA -> t("rep_reserva_confirmada")
        EstadoReserva.PENDIENTE_APROBACION -> t("rep_reserva_pendiente")
        EstadoReserva.CANCELADA -> t("rep_reserva_cancelada")
        EstadoReserva.RECHAZADA -> t("rep_reserva_rechazada")
        EstadoReserva.FINALIZADA -> t("rep_reserva_finalizada")
    }

    private fun asistencia(valor: String?) = when (valor) {
        ASISTIO -> t("rep_asistio")
        NO_ASISTIO -> t("rep_no_asistio")
        else -> t("rep_sin_registro")
    }

    // ---------------------------------------------------------------- Áreas, inventario, personal y torneos

    private fun tablaAreas(areas: List<Area>) = Tabla(
        t("rep_sec_areas"),
        listOf(t("rep_col_area"), t("rep_col_deporte"), t("rep_col_capacidad"), t("rep_col_carriles"), t("rep_col_estado"), t("rep_col_externos")),
        areas.sortedBy { it.nombre }.map { a ->
            listOf(
                texto(etiquetaArea(a.nombre)), texto(etiquetaDeporte(a.tipo)), numero(a.capacidad), numero(a.carriles),
                texto(disponibilidad(a.disponibilidad)), texto(siNo(a.permiteExternos))
            )
        },
        listOf(2f, 1.2f, 1f, 1f, 1.2f, 1f)
    )

    private fun disponibilidad(d: DisponibilidadArea) = when (d) {
        DisponibilidadArea.DISPONIBLE -> t("rep_disp_disponible")
        DisponibilidadArea.OCUPADA -> t("rep_disp_ocupada")
        DisponibilidadArea.MANTENIMIENTO -> t("rep_disp_mantenimiento")
    }

    private fun stockBajo(a: ArticuloInventario) = a.stockMinimo > 0 && a.cantidad <= a.stockMinimo

    private fun tablaInventario(articulos: List<ArticuloInventario>) = Tabla(
        t("rep_sec_inventario"),
        listOf(t("rep_col_articulo"), t("rep_col_deporte"), t("rep_col_cantidad"), t("rep_col_minimo"), t("rep_col_estado")),
        articulos.sortedWith(compareBy({ !stockBajo(it) }, { it.deporte }, { it.nombre })).map { a ->
            listOf(
                texto(a.nombre), texto(etiquetaDeporte(a.deporte)), numero(a.cantidad), numero(a.stockMinimo),
                texto(if (stockBajo(a)) t("rep_stock_bajo") else t("rep_stock_ok"))
            )
        },
        listOf(2f, 1.2f, 1f, 1f, 1f)
    )

    private fun tablaPersonal(personal: List<Personal>) = Tabla(
        t("rep_sec_personal"),
        listOf(
            t("rep_col_nombre"), t("rep_col_rol"), t("rep_col_puesto"), t("rep_col_area"), t("rep_col_turno"),
            t("rep_col_telefono"), t("rep_col_correo"), t("rep_col_estado"), t("rep_col_ingreso")
        ),
        personal.sortedBy { it.nombre.lowercase() }.map { p ->
            listOf(
                texto(p.nombre), texto(p.rol.nombreLegible()), texto(etiquetaTipoPersonal(p.tipoPersonal)),
                texto(p.areaTrabajo?.let { etiquetaDeporte(it) }.orEmpty()), texto(etiquetaTurno(p.turno)), texto(p.telefono),
                texto(p.email), texto(estadoPersonal(p.estado)), texto(p.fechaIngreso)
            )
        },
        listOf(1.8f, 1.1f, 1.2f, 1f, 1f, 1.1f, 2f, 0.8f, 1f)
    )

    private fun estadoPersonal(estado: String) = when (estado.uppercase()) {
        "ACTIVO" -> t("rep_activo")
        "INACTIVO" -> t("rep_inactivo")
        else -> estado
    }

    private fun tablaTorneos(torneos: List<Torneo>) = Tabla(
        t("rep_sec_torneos"),
        listOf(t("rep_col_torneo"), t("rep_col_deporte"), t("rep_col_inicio"), t("rep_col_fin"), t("rep_col_horario"), t("rep_col_cupo"), t("rep_col_inscritos")),
        torneos.map { torneo ->
            listOf(
                texto(torneo.nombre), texto(etiquetaDeporte(torneo.disciplina)), texto(torneo.fechaInicio), texto(torneo.fechaFin),
                texto(if (torneo.horaInicio.isBlank()) "" else "${torneo.horaInicio}–${torneo.horaFin}"),
                numero(torneo.cupoMaximo), numero(torneo.inscritos)
            )
        },
        listOf(2f, 1.2f, 1f, 1f, 1.1f, 0.8f, 0.8f)
    )

    private fun siNo(valor: Boolean) = if (valor) t("rep_si") else t("rep_no")

    private const val ASISTIO = "ASISTIO"
    private const val NO_ASISTIO = "NO_ASISTIO"
}
