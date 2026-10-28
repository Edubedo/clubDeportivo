package com.example.clubdeportivo.util.reportes

import com.example.clubdeportivo.TextosDePrueba
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.EstadoPago
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PagoClub
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.TipoMembresia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ReportesTest {
    init { TextosDePrueba.instalar() }

    private val hoy = "2026-10-27"

    private fun pago(id: String, fecha: String, monto: Double, metodo: MetodoPago = MetodoPago.EFECTIVO, estado: EstadoPago = EstadoPago.PAGADO) =
        PagoClub(id, "m1", "Ana López", ConceptoPago.MENSUALIDAD, monto, metodo, fecha, estado)

    private fun reserva(id: String, fecha: String, estado: EstadoReserva = EstadoReserva.CONFIRMADA) =
        Reserva(id, "u1", "a1", fecha, "15:00", "16:30", estado, personas = 2, usuarioNombre = "Ana", areaNombre = "Alberca 1", deporte = "Natación")

    private val membresia = MembresiaDetalle(
        Membresia("m1", "u1", TipoMembresia.INDIVIDUAL, null, null, 800.0, EstadoMembresia.ACTIVA, "2026-10-03", "2026-10-30"),
        listOf(MiembroClub("CLB-1", "Ana López", "555", "ana@club.mx", "m1", MiembroClub.PARENTESCO_TITULAR, "u1", true))
    )

    private val datos = DatosReporte(
        pagos = listOf(pago("p1", "2026-10-05", 800.0), pago("p2", "2026-10-20", 200.0, MetodoPago.TARJETA), pago("p3", "2026-09-28", 999.0),
            pago("p4", "2026-10-21", 50.0, estado = EstadoPago.PENDIENTE)),
        membresias = listOf(membresia),
        reservas = listOf(reserva("r1", "2026-10-10"), reserva("r2", "2026-10-11", EstadoReserva.CANCELADA), reserva("r3", "2026-08-01")),
        asistencias = mapOf("r1" to "ASISTIO"),
        areas = listOf(Area("a1", "Alberca 1", "Natación", 8, DisponibilidadArea.DISPONIBLE)),
        inventario = listOf(ArticuloInventario("i1", "Balón", "Fútbol", 2, "", 5), ArticuloInventario("i2", "Red", "Tenis", 9, "", 1))
    )

    private fun reporte(periodo: PeriodoReporte = PeriodoReporte.ESTE_MES, d: DatosReporte = datos) =
        GeneradorReporte.generar(d, periodo, hoy, "Generado")

    @Test
    fun `los periodos cubren meses completos`() {
        assertEquals("2026-10-01" to "2026-10-31", PeriodoReporte.ESTE_MES.rango(hoy))
        assertEquals("2026-09-01" to "2026-09-30", PeriodoReporte.MES_ANTERIOR.rango(hoy))
        assertEquals("2026-08-01" to "2026-10-31", PeriodoReporte.ULTIMOS_3_MESES.rango(hoy))
        assertEquals("2026-01-01" to "2026-12-31", PeriodoReporte.ESTE_ANIO.rango(hoy))
        assertEquals("2026-02-01" to "2026-02-28", PeriodoReporte.MES_ANTERIOR.rango("2026-03-31"))
    }

    @Test
    fun `los ingresos solo cuentan lo pagado dentro del periodo`() {
        val r = reporte()
        val ingresos = r.tablas.first { it.titulo == "Ingresos" }
        assertEquals(3, ingresos.filas.size) // p3 es de septiembre
        val resumen = r.resumen.first { it.titulo == "Ingresos" }.datos.toMap()
        assertEquals("$1,000.00", resumen["Total cobrado"])
        assertEquals("2", resumen["Cobros registrados"])
        assertEquals("1", resumen["Cobros pendientes o rechazados"])
    }

    @Test
    fun `reservas del periodo con asistencia y uso por area`() {
        val r = reporte()
        val reservas = r.tablas.first { it.titulo == "Reservas" }
        assertEquals(2, reservas.filas.size)
        assertEquals("Asistió", reservas.filas[0].last().comoTexto())
        val uso = r.tablas.first { it.titulo == "Uso por área" }.filas.single()
        assertEquals(1.0, (uso[2] as Celda.Numero).valor, 0.0) // una reserva usada
        assertEquals(1.5, (uso[3] as Celda.Numero).valor, 0.0) // 15:00–16:30
        assertEquals(1.0, (uso[5] as Celda.Numero).valor, 0.0) // una cancelada
    }

    @Test
    fun `solo se exportan las secciones con datos`() {
        val r = reporte(d = DatosReporte(inventario = datos.inventario))
        assertEquals(listOf("Inventario"), r.tablas.map { it.titulo })
        // El de stock bajo va primero.
        assertEquals("Balón", r.tablas.single().filas.first().first().comoTexto())
        assertEquals("1", r.resumen.single().datos.toMap()["Con stock bajo"])
    }

    @Test
    fun `membresias y miembros van en tablas separadas`() {
        val r = reporte(PeriodoReporte.TODO)
        assertEquals(1, r.tablas.first { it.titulo == "Membresías" }.filas.size)
        val miembro = r.tablas.first { it.titulo == "Miembros" }.filas.single()
        assertEquals("CLB-1", miembro.first().comoTexto())
        assertEquals("Sí", miembro.last().comoTexto())
        assertEquals("Todo el historial", r.periodo)
    }

    @Test
    fun `el excel es un zip con una hoja por tabla mas el resumen`() {
        val salida = ByteArrayOutputStream()
        EscritorXlsx.escribir(reporte(), salida)
        val archivos = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(salida.toByteArray())).use { zip ->
            generateSequence { zip.nextEntry }.forEach { archivos[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        val tablas = reporte().tablas.size
        assertTrue("[Content_Types].xml" in archivos && "xl/workbook.xml" in archivos && "xl/styles.xml" in archivos)
        assertEquals(tablas + 1, archivos.keys.count { it.startsWith("xl/worksheets/sheet") })
        assertTrue(archivos.getValue("xl/workbook.xml").contains("""name="Resumen""""))
        // Los montos van como número con formato de pesos (estilo 2), no como texto.
        assertTrue(archivos.values.any { it.contains("""s="2"><v>800.0</v>""") })
    }

    @Test
    fun `columnas y nombres de hoja validos para excel`() {
        assertEquals("A", EscritorXlsx.columna(0))
        assertEquals("Z", EscritorXlsx.columna(25))
        assertEquals("AA", EscritorXlsx.columna(26))
        val usados = mutableSetOf<String>()
        assertEquals("Uso por área", EscritorXlsx.nombreUnico("Uso por área", usados))
        assertEquals("Uso por área (2)", EscritorXlsx.nombreUnico("Uso por área", usados))
        assertEquals("a b", EscritorXlsx.nombreUnico("a/b", usados))
        assertTrue(EscritorXlsx.nombreUnico("x".repeat(50), usados).length <= 31)
    }
}
